package org.openremote.agent.protocol.strijp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.ProtocolLinkedAttributeImport;
import org.openremote.container.util.CodecUtil;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.*;
import org.openremote.model.file.FileInfo;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.asset.AssetType.THING;
import static org.openremote.model.attribute.AttributeValueType.*;
import static org.openremote.model.attribute.MetaItemType.AGENT_LINK;
import static org.openremote.model.attribute.MetaItemType.READ_ONLY;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class StrijpProtocol extends AbstractProtocol implements ProtocolLinkedAttributeImport {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, StrijpProtocol.class);

    private static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":strijpClient";
    private static final String PROTOCOL_DISPLAY_NAME = "Strijp Client";
    private static final String PROTOCOL_VERSION = "0.1";
    private static final String agentProtocolConfigName = "strijpClient";

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Collections.emptyList();

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_ATTRIBUTE_MATCH_FILTERS,
            META_ATTRIBUTE_MATCH_PREDICATE);

    private List<StrijpLight> strijpLightMemory = new ArrayList<>();

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return ATTRIBUTE_META_ITEM_DESCRIPTORS;
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return PROTOCOL_VERSION;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate();
    }

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        updateStatus(protocolRef, ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        // Called when edit/remove Protocol Agent
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) throws Exception {
        String assetId = attribute.getAssetId().orElse(null);
        if (assetId != null) {
            Asset asset = assetService.findAsset(assetId);
            AssetAttribute hostAttribute =  asset.getAttribute("Host").orElse(null);
            AssetAttribute portAttribute =  asset.getAttribute("Port").orElse(null);
            GeoJSONPoint geo = asset.getCoordinates();
            if (hostAttribute != null && portAttribute != null) {
                String hostValue = hostAttribute.getValueAsString().orElse(null);
                Integer portValue = portAttribute.getValueAsInteger().orElse(null);
                if (hostValue != null && portValue != null) {
                    StrijpLight light;
                    if (geo != null) {
                        light = new StrijpLight(assetId, hostValue, portValue, geo.getX(), geo.getY());
                    } else {
                        light = new StrijpLight(assetId, hostValue, portValue);
                    }
                    strijpLightMemory.add(light);
                }
            }
        }
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        String assetId = attribute.getAssetId().orElse(null);
        if (assetId != null) {
            StrijpLight lightToDelete = strijpLightMemory.stream().filter(l -> l.getAssetId().equals(assetId)).findFirst().orElse(null);
            if (lightToDelete != null) {
                strijpLightMemory.remove(lightToDelete);
            }
        }
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
        String host = null;
        Integer port = null;

        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());
        String assetId = attribute.getAssetId().orElse(null);

        if (assetId!= null) {
            Asset asset = assetService.findAsset(assetId);
            AssetAttribute hostAttribute = asset.getAttribute("Host").orElse(null);
            AssetAttribute portAttribute = asset.getAttribute("Port").orElse(null);
            if (hostAttribute != null && portAttribute != null) {
                String hostValue = hostAttribute.getValueAsString().orElse(null);
                Integer portValue = portAttribute.getValueAsInteger().orElse(null);
                if (hostValue != null && portValue != null) {
                    host = hostValue;
                    port = portValue;
                }
            }
        }

        if (host == null) {
            return;
        }

        try {
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getByName(host);

            // processedValue format is [0,0,0,0,0] - R G B A W
            // Remove angle-brackets
            String stringArr = processedValue.toString().substring(1, processedValue.toString().length()-1);
            // Build int array
            int[] buf = Arrays.stream(stringArr.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();

            DatagramPacket DpSend = new DatagramPacket(int_to_unsigned_byte_array(buf), buf.length, ip, port);
            ds.send(DpSend);
        } catch (Exception ignored) {}
    }

    private static byte[] int_to_unsigned_byte_array(int[] byte_array) {
        byte[] buf = {0, 0, 0, 0, 0};

        for (int i = 0; i < byte_array.length; i++) {
            buf[i] = (byte) byte_array[i];
        }

        return buf;
    }

    @Override
    public AssetTreeNode[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration, FileInfo fileInfo) throws IllegalStateException {
        String jsonString;
        if(fileInfo.isBinary())//Read any file that isn't an XML file
        {
            //Read from .json file || Works on files without extention || Works on CSV
            byte[] rawBinaryData = CodecUtil.decodeBase64(fileInfo.getContents());
            jsonString = new String(rawBinaryData);
        }
        else
            throw new IllegalStateException("The import-file format should be .json.");
        try{
            List<StrijpLight> newLights = parseStrijpLightsFromImport(new ObjectMapper().readTree(jsonString));
            return syncLightsToAssets(newLights, protocolConfiguration);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The provided json is invalid.");
        } catch (Exception e) {
            throw new IllegalStateException("Could not import the provided lights.");
        }
    }

    private List<StrijpLight> parseStrijpLightsFromImport(JsonNode jsonNode) {
        JsonNode lightsNode = jsonNode.get("lights");
        List<StrijpLight> parsedLights = new ArrayList<>();
        for(JsonNode lightNode : lightsNode) {
            String host = lightNode.get("host").asText();
            Integer port = lightNode.get("port").asInt();
            double x = lightNode.get("long").asDouble();
            double y = lightNode.get("lat").asDouble();

            StrijpLight light;
            if (x == 0.0 || y == 0.0) {
                light = new StrijpLight("", host, port);
            } else {
                light = new StrijpLight("", host, port, x, y);
            }
            parsedLights.add(light);
        }
        return parsedLights;
    }

    private AssetTreeNode[] syncLightsToAssets(List<StrijpLight> lights, AssetAttribute protocolConfiguration) {
        List<AssetTreeNode> output = new ArrayList<AssetTreeNode>();

        //Fetch all the assets that're connected to the Strijp agent.
        List<Asset> assetsUnderProtocol = assetService.findAssets(protocolConfiguration.getAssetId().orElse(null), new AssetQuery());
        //Get the instance of the Strijp agent itself.
        Asset parentAgent = assetsUnderProtocol.stream().filter(a -> a.getWellKnownType() == AssetType.AGENT).findFirst().orElse(null);
        if(parentAgent != null) {
            for(StrijpLight strijpLight : new ArrayList<>(strijpLightMemory)) {
                Asset asset = assetService.findAsset(strijpLight.getAssetId());

                //TODO CHANGE ASSET TYPE THING TO LIGHT
                if(asset.getWellKnownType() != AssetType.THING)
                    continue;

                if(!asset.hasAttribute("Host")) //Confirm the asset is a light
                    continue;

                //Asset is valid
                AssetAttribute lightAttribute = asset.getAttribute("Host").orElse(null);
                if(lightAttribute != null) {
                    String lightHost = lightAttribute.getValueAsString().orElse(null);
                    if(lightHost != null) {
                        if(lights.stream().anyMatch(l -> l.getHost().equals(lightHost))) {
                            StrijpLight updatedLight = lights.stream().filter(l -> l.getHost().equals(lightHost)).findFirst().orElse(null);
                            if(updatedLight != null) {
                                // Update fields of the asset light
                                List<AssetAttribute> strijpLightAttributes = Arrays.asList(
                                        new AssetAttribute("Host", STRING, Values.create(updatedLight.getHost())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        new AssetAttribute("Port", INTEGER, Values.create(updatedLight.getPort())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        new AssetAttribute("location", GEO_JSON_POINT, Values.createObject().put("type", "Point").put("coordinates", Values.createArray().add(updatedLight.getX()).add(updatedLight.getY()))).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        //TODO Set RGBAW from StrijpLight state
                                        new AssetAttribute("RGBAW", STRING, Values.create("[0,0,0,0,0]")).addMeta(
                                                new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                                        )
                                );
                                asset.setAttributes(strijpLightAttributes);
                                assetService.mergeAsset(asset);

                                StrijpLight lightToUpdate = strijpLightMemory.stream().filter(l -> l.getAssetId().equals(strijpLight.getAssetId())).findFirst().orElse(null);
                                if (lightToUpdate != null) {
                                    // Update properties of the in memory light
                                    lightToUpdate.setPort(updatedLight.getPort());
                                    lightToUpdate.setX(updatedLight.getX());
                                    lightToUpdate.setY(updatedLight.getY());
                                }
                            }
                        }else{
                            if(lights.stream().noneMatch(l -> l.getHost().equals(lightHost))) {
                                assetService.deleteAsset(asset.getId());
                            }
                        }
                    }
                }
            }

            for(StrijpLight light : lights)
            {
                boolean lightAssetExistsAlready = false;
                for(StrijpLight strijpLight : strijpLightMemory) {
                    Asset asset = assetService.findAsset(strijpLight.getAssetId());

                    //TODO CHANGE ASSET TYPE THING TO LIGHT
                    if((asset.getWellKnownType() != AssetType.THING))
                        continue;

                    if(!asset.hasAttribute("Host"))
                        continue;

                    AssetAttribute lightHostAttribute = asset.getAttribute("Host").orElse(null);
                    if(lightHostAttribute != null) {
                        String lightHost = lightHostAttribute.getValueAsString().orElse(null);
                        if(lightHost != null) {
                            if(lightHost.equals(light.getHost())) {
                                lightAssetExistsAlready = true;
                            }
                        }
                    }

                }
                if(!lightAssetExistsAlready)
                    output.add(formLightAsset(light, parentAgent));
            }
            return output.toArray(new AssetTreeNode[0]);
        }
        return null;
    }

    private AssetTreeNode formLightAsset(StrijpLight light, Asset parentAgent) {
        Asset asset = new Asset();
        asset.setId(UniqueIdentifierGenerator.generateId());
        asset.setParent(parentAgent);
        asset.setName("Strijp Light " + light.getHost());
        //TODO CHANGE ASSET TYPE THING TO LIGHT
        asset.setType(THING);

        List<AssetAttribute> strijpLightAttributes = Arrays.asList(
                new AssetAttribute("Host", STRING, Values.create(light.getHost())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                new AssetAttribute("Port", INTEGER, Values.create(light.getPort())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                new AssetAttribute("location", GEO_JSON_POINT, Values.createObject().put("type", "Point").put("coordinates", Values.createArray().add(light.getX()).add(light.getY()))).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                new AssetAttribute("RGBAW", STRING, Values.create("[0,0,0,0,0]")).addMeta(
                        new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                )
        );
        asset.setAttributes(strijpLightAttributes);
        return new AssetTreeNode(asset);
    }
}

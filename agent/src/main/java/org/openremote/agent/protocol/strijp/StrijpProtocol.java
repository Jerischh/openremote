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
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":strijpClient";
    public static final String PROTOCOL_DISPLAY_NAME = "Strijp Client";
    public static final String PROTOCOL_VERSION = "0.1";
    public static final String agentProtocolConfigName = "strijpClient";

    private String host;
    private Integer port;

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_PROTOCOL_HOST,
            META_PROTOCOL_PORT
    );

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
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
                .addMeta(
                        new MetaItem(META_PROTOCOL_HOST, null),
                        new MetaItem(META_PROTOCOL_PORT, null)
                );
    }

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        host = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_HOST,
                false,
                false
        ).flatMap(Values::getString).orElse(null);

        port = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_PORT,
                false,
                false
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        if (port != null && (port < 1 || port > 65536)) {
            throw new IllegalArgumentException("Port must be in the range 1-65536");
        }

        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        updateStatus(protocolRef, ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        // Called when edit/remove Protocol Agent
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) throws Exception {
        // Called when attribute link is created
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        // Called when attribute link is updated/removed
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
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
        } catch (Exception e) {}
    }

    private static byte[] int_to_unsigned_byte_array(int[] byte_array) {
        byte[] buf = {0, 0, 0, 0, 0};

        for (int i = 0; i < byte_array.length; i++) {
            buf[i] = (byte) byte_array[i];
        }

        return buf;
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
            syncLightsToMemory(newLights);
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
            StrijpLight light = new StrijpLight(host);
            parsedLights.add(light);
        }
        return parsedLights;
    }

    private void syncLightsToMemory(List<StrijpLight> lights) {
        // Add lights that don't exist yet
        for (StrijpLight light : lights) {
            // Continue if light already exists.
            if (strijpLightMemory.stream().anyMatch(l -> l.getHost().equals(light.getHost()))) continue;

            StrijpLight newLight = new StrijpLight(light.getHost());
            strijpLightMemory.add(newLight);
        }

        // Remove a light from memory if not included in new list
        for (StrijpLight light : new ArrayList<>(strijpLightMemory)) {
            // Light not found so remove it
            if (lights.stream().noneMatch(l -> l.getHost().equals(light.getHost()))) {
                Optional<StrijpLight> foundLight = strijpLightMemory.stream().filter(l -> l.getHost().equals(light.getHost())).findFirst();
                foundLight.ifPresent(strijpLight -> strijpLightMemory.remove(strijpLight));
            }
        }
    }

    private AssetTreeNode[] syncLightsToAssets(List<StrijpLight> lights, AssetAttribute protocolConfiguration) {
        List<AssetTreeNode> output = new ArrayList<AssetTreeNode>();

        //Fetch all the assets that're connected to the Strijp agent.
        List<Asset> assetsUnderProtocol = assetService.findAssets(protocolConfiguration.getAssetId().orElse(null), new AssetQuery());
        //Get the instance of the Strijp agent itself.
        Asset parentAgent = assetsUnderProtocol.stream().filter(a -> a.getWellKnownType() == AssetType.AGENT).findFirst().orElse(null);
        if(parentAgent != null) {
            for(Asset asset : assetsUnderProtocol)
            {
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
                                List<AssetAttribute> strijpLightAttributes = Arrays.asList(
                                        new AssetAttribute("Host", STRING, Values.create(updatedLight.getHost())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        new AssetAttribute("RGBAW", STRING, Values.create("[0,0,0,0,0]")).addMeta(
                                                new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                                        )
                                );
                                asset.setAttributes(strijpLightAttributes);
                                assetService.mergeAsset(asset);
                            }
                        }else{
                            if(lights.stream().noneMatch(l -> l.getHost().equals(lightHost)))
                                assetService.deleteAsset(asset.getId());
                        }
                    }
                }
            }

            //New data is fetched based on the changes.
            assetsUnderProtocol = assetService.findAssets(protocolConfiguration.getAssetId().orElse(null), new AssetQuery());
            for(StrijpLight light : lights)
            {
                boolean lightAssetExistsAlready = false;
                for(Asset asset : assetsUnderProtocol)
                {
                    //TODO CHANGE ASSET TYPE THING TO LIGHT
                    if((asset.getWellKnownType() != AssetType.THING))
                        continue;

                    if(!asset.hasAttribute("Host"))
                        continue;

                    AssetAttribute lightHostAttribute = asset.getAttribute("Host").orElse(null);
                    if(lightHostAttribute != null) {
                        String lightHost = lightHostAttribute.getValueAsString().orElse(null);
                        if(lightHost != null) {
                            if(lightHost.equals(light.getHost()))
                                lightAssetExistsAlready = true;
                        }
                    }

                }
                if(!lightAssetExistsAlready)
                    output.add(formLightAsset(light, parentAgent));
            }
            return output.toArray(new AssetTreeNode[output.size()]);
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
                new AssetAttribute("RGBAW", STRING, Values.create("[0,0,0,0,0]")).addMeta(
                        new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                )
        );
        asset.setAttributes(strijpLightAttributes);
        return new AssetTreeNode(asset);
    }
}

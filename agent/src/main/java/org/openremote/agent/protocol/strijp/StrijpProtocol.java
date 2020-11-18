package org.openremote.agent.protocol.strijp;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class StrijpProtocol extends AbstractProtocol {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, StrijpProtocol.class);
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":strijpClient";
    public static final String PROTOCOL_DISPLAY_NAME = "Strijp Client";
    public static final String PROTOCOL_VERSION = "0.1";

    private String host;
    private Integer port;

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_PROTOCOL_HOST,
            META_PROTOCOL_PORT
    );

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_ATTRIBUTE_MATCH_FILTERS,
            META_ATTRIBUTE_MATCH_PREDICATE);

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

    private static byte[] int_to_unsigned_byte_array(int[] byte_array)
    {
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
}

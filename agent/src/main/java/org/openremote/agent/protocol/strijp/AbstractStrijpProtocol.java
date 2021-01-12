package org.openremote.agent.protocol.strijp;

import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.agent.protocol.udp.UdpIoClient;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.List;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemInteger;

public abstract class AbstractStrijpProtocol<T> extends AbstractIoClientProtocol<T, UdpIoClient<T>> {
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":abstractStrijpClient";


    public static final MetaItemDescriptor META_PROTOCOL_BIND_PORT = metaItemInteger(
            PROTOCOL_NAME + ":bindPort",
            ACCESS_PRIVATE,
            true,
            1,
            65536);

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_PROTOCOL_HOST,
            META_PROTOCOL_PORT,
            META_PROTOCOL_BIND_PORT
    );

    @Override
    protected UdpIoClient<T> createIoClient(AssetAttribute protocolConfiguration) throws Exception {
        String host = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_HOST,
                false,
                false
        ).flatMap(Values::getString).orElse(null);

        Integer port = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_PORT,
                false,
                false
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        Integer bindPort = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_BIND_PORT,
                false,
                false
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        if (port != null && (port < 1 || port > 65536)) {
            throw new IllegalArgumentException("Port must be in the range 1-65536");
        }

        if (bindPort != null && (bindPort < 1 || bindPort > 65536)) {
            throw new IllegalArgumentException("Bind port must be in the range 1-65536 if specified");
        }

        return new UdpIoClient<>(host, port, bindPort, executorService);
    }
}

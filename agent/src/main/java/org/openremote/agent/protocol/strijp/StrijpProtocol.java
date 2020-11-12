package org.openremote.agent.protocol.strijp;

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.agent.protocol.udp.UdpClientProtocol;
import org.openremote.agent.protocol.udp.UdpIoClient;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;

import static org.openremote.container.util.Util.joinCollections;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class StrijpProtocol extends AbstractStrijpProtocol<String> {
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, UdpClientProtocol.class);
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":strijpClient";
    public static final String PROTOCOL_DISPLAY_NAME = "Strijp Client";
    public static final String PROTOCOL_VERSION = "0.1";

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = joinCollections(AbstractStrijpProtocol.PROTOCOL_META_ITEM_DESCRIPTORS, AbstractIoClientProtocol.PROTOCOL_GENERIC_META_ITEM_DESCRIPTORS);

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_ATTRIBUTE_MATCH_FILTERS,
            META_ATTRIBUTE_MATCH_PREDICATE);

    protected final Map<AttributeRef, List<Pair<AttributeRef, Consumer<String>>>> protocolMessageConsumers = new HashMap<>();

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
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider(UdpIoClient<String> client, AssetAttribute protocolConfiguration) {
        return getGenericStringEncodersAndDecoders(client, protocolConfiguration);
    }

    @Override
    protected void onMessageReceived(AttributeRef protocolRef, String message) {
        List<Pair<AttributeRef, Consumer<String>>> consumers;

        synchronized (protocolMessageConsumers) {
            consumers = protocolMessageConsumers.get(protocolRef);

            if (consumers != null) {
                consumers.forEach(c -> {
                    if (c.value != null) {
                        c.value.accept(message);
                    }
                });
            }
        }
    }

    @Override
    protected String createWriteMessage(AssetAttribute protocolConfiguration, AssetAttribute attribute, AttributeEvent event, Value processedValue) {
        LOG.info("RIES 1.3 - " + processedValue.toJson());
        if (attribute.isReadOnly()) {
            LOG.fine("Attempt to write to an attribute that doesn't support writes: " + event.getAttributeRef());
            return null;
        }

        return processedValue != null ? processedValue.toString() : null;
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) throws Exception {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        Consumer<String> messageConsumer = Protocol.createGenericAttributeMessageConsumer(attribute, assetService, this::updateLinkedAttribute);

        if (messageConsumer != null) {
            synchronized (protocolMessageConsumers) {
                protocolMessageConsumers.compute(protocolRef, (ref, consumers) -> {
                    if (consumers == null) {
                        consumers = new ArrayList<>();
                    }
                    consumers.add(new Pair<>(
                            attribute.getReferenceOrThrow(),
                            messageConsumer
                    ));
                    return consumers;
                });
            }
        }
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        synchronized (protocolMessageConsumers) {
            protocolMessageConsumers.compute(protocolConfiguration.getReferenceOrThrow(), (ref, consumers) -> {
                if (consumers != null) {
                    consumers.removeIf((attrRefConsumer) -> attrRefConsumer.key.equals(attributeRef));
                }
                return consumers;
            });
        }
    }
}

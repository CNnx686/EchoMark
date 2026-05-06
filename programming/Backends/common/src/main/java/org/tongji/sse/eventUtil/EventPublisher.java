package org.tongji.sse.eventUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.tongji.sse.eventUtil.enums.EventChannelEnum;
import org.tongji.sse.eventUtil.properties.EventChannelsProperties;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class EventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final EventChannelsProperties channelsProperties;

    public EventPublisher(RabbitTemplate rabbitTemplate, EventChannelsProperties channelsProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.channelsProperties = channelsProperties;
    }

    private static final ThreadLocal<List<EventWrapper<?>>> EVENTS = ThreadLocal.withInitial(ArrayList::new);

    public <T> void register(T event, EventChannelEnum channel) {
        EVENTS.get().add(new EventWrapper<>(event, channel));

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            sendAll();
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendAll();
            }

            @Override
            public void afterCompletion(int status) {
                EVENTS.remove();
            }
        });
    }

    private void sendAll() {
        List<EventWrapper<?>> toSend = EVENTS.get();
        try {
            for (EventWrapper<?> wrapper : toSend) {
                EventChannelsProperties.EventChannel channelConfig = channelsProperties.getChannels().stream()
                        .filter(c -> c.getName().equals(wrapper.channel().name()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Channel config not found: " + wrapper.channel()));
                rabbitTemplate.convertAndSend(channelConfig.getExchange(), channelConfig.getRoutingKey(), wrapper.event());
            }
        } finally {
            EVENTS.remove();
        }
    }

    private record EventWrapper<T>(T event, EventChannelEnum channel) {}
}

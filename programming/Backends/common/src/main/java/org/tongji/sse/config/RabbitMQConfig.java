package org.tongji.sse.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tongji.sse.eventUtil.properties.EventChannelsProperties;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(EventChannelsProperties.class)
public class RabbitMQConfig {

    private final EventChannelsProperties channelsProperties;

    public RabbitMQConfig(EventChannelsProperties channelsProperties) {
        this.channelsProperties = channelsProperties;
    }

    @Bean
    public Declarables eventDeclarables() {
        List<Declarable> list = new ArrayList<>();
        for (EventChannelsProperties.EventChannel channel : channelsProperties.getChannels()) {
            TopicExchange exchange = new TopicExchange(channel.getExchange(), true, false);
            Queue queue = new Queue(channel.getQueue(), true);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(channel.getRoutingKey());
            list.add(exchange);
            list.add(queue);
            list.add(binding);
        }
        return new Declarables(list);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // 确保这个为 true，这样连接恢复时它会自动重新声明 Declarables
        admin.setAutoStartup(true);
        return admin;
    }
}
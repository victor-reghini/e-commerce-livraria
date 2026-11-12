package com.livraria.ecommerce.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DIRECT_EXCHANGE = "ecommerce.direct";
    public static final String DEAD_LETTER_EXCHANGE = "ecommerce.dlx";

    public static final String NFE_QUEUE = "queue.nfe.emission";
    public static final String NFE_ROUTING_KEY = "nfe.emission";

    public static final String NOTIFICATIONS_QUEUE = "queue.notifications";
    public static final String NOTIFICATIONS_ROUTING_KEY = "notifications.dispatch";

    public static final String STOCK_SYNC_QUEUE = "queue.stock.sync";
    public static final String STOCK_SYNC_ROUTING_KEY = "stock.sync";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue nfeQueue() {
        return QueueBuilder.durable(NFE_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq." + NFE_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding nfeBinding(Queue nfeQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(nfeQueue).to(directExchange).with(NFE_ROUTING_KEY);
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq." + NOTIFICATIONS_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(notificationsQueue).to(directExchange).with(NOTIFICATIONS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}

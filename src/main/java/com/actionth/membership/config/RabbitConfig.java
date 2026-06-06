package com.actionth.membership.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitConfig {
    
    @Value("${spring.rabbitmq.queue.simple-email}")
    private String simpleEmailQueueName;
    
    public String getSimpleEmailQueueName() {
        return simpleEmailQueueName;
    }

    @Bean
    public Queue simpleEmailQueue() {
        return new Queue(simpleEmailQueueName, true);
    }
}
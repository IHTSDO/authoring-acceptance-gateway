package org.snomed.aag.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import jakarta.jms.ConnectionFactory;

@Configuration
public class JMSConfig {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean(name = "topicJmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory getTopicFactory() {
        DefaultJmsListenerContainerFactory factory = new  DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        factory.setPubSubDomain(true);
        return factory;
    }
}

package org.chengy.configuration.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"org.chengy.repository.remote"}, mongoTemplateRef = "orderMongoTemplate")
@ConfigurationProperties(prefix = "order.mongodb")
public class OrderMongoConfigure extends AbstractMongoConfigure{
    @Primary
    @Override
    @Bean(name = "orderMongoTemplate")
    public MongoTemplate getMongoTemplate() throws Exception {
        return new MongoTemplate(mongoDbFactory());
    }
}


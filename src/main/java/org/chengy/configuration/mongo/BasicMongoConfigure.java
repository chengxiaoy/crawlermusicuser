package org.chengy.configuration.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"org.chengy.repository.local"}, mongoTemplateRef = "basicMongoTemplate")
@ConfigurationProperties(prefix = "basic.mongodb")
public class BasicMongoConfigure extends AbstractMongoConfigure {
    @Override
    @Bean(name = "basicMongoTemplate")
    public MongoTemplate getMongoTemplate() throws Exception {
        return new MongoTemplate(mongoDbFactory());
    }
}


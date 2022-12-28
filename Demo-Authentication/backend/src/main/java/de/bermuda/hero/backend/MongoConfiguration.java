package de.bermuda.hero.backend;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories
public class MongoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfiguration.class);


    @RefreshScope
    @Bean
    public MongoClient mongoClient(Environment environment) {
        LOGGER.info("Creating new MongoClient");

        var mongoUri = environment.getProperty("spring.data.mongodb.uri");
        var mongoUuidRepresentation = environment.getProperty("spring.data.mongodb.uuid-representation",
                UuidRepresentation.class, UuidRepresentation.STANDARD);

        if (mongoUri == null) {
            throw new BeanCreationException("MongoClient", "missing connection string");
        }

        MongoClientSettings clientSettings = MongoClientSettings
                .builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .uuidRepresentation(mongoUuidRepresentation)
                .build();
        return MongoClients.create(clientSettings);
    }
}


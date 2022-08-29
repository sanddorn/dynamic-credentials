package de.bermuda.hero.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = BackendApplicationTest.ContextInitializer.class)
class BackendApplicationTest {

    @Container
    private static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");


    @Test
    void contextLoads() {
    }

    static class ContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of("spring.data.mongodb.uri=" + mongoDBContainer.getConnectionString())
                              .applyTo(applicationContext.getEnvironment());
        }
    }
}

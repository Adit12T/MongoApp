package io.github.adit12t.mongoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MongoAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongoAppApplication.class, args);
    }
}

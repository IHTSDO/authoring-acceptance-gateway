package org.snomed.aag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@ConditionalOnProperty(name = "snowstorm.connection.test", havingValue = "true")
@Component
public class SnowstormConnectionTester implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowstormConnectionTester.class);

    @Value("${snowstorm.url}")
    private String snowstormUrl;

    private final RestTemplate restTemplate;

    public SnowstormConnectionTester(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public void run(String... args) {
        snowstormUrl = snowstormUrl + "/version";
        LOGGER.info("Confirming connection to Snowstorm. (Snowstorm URL: {})", snowstormUrl);
        try {
            this.restTemplate.getForObject(snowstormUrl, String.class);
            LOGGER.info("Successfully confirmed connection to Snowstorm.");
        } catch (Exception e) {
            LOGGER.warn("Cannot connect to Snowstorm. (Error: {})", e.getMessage());
        }
    }

}

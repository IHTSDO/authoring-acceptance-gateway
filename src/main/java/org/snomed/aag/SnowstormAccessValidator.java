package org.snomed.aag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@ConditionalOnProperty(name = "snowstorm.confirm-access", havingValue = "true")
@Component
public class SnowstormAccessValidator implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowstormAccessValidator.class);

    @Value("${snowstorm.url}")
    private String snowstormUrl;

    private final RestTemplate restTemplate;

    public SnowstormAccessValidator(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public void run(String... args) {
        snowstormUrl = snowstormUrl + "/version";
        LOGGER.info("Confirming access to Snowstorm. (Snowstorm URL: {})", snowstormUrl);
        try {
            this.restTemplate.getForObject(snowstormUrl, String.class);
            LOGGER.info("Successfully confirmed access to Snowstorm.");
        } catch (Exception e) {
            LOGGER.warn("Cannot access Snowstorm. (Error: {})", e.getMessage());
        }
    }

}

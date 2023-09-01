package org.snomed.aag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.services.CriteriaItemService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchConnectionTester implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchConnectionTester.class);

    private final CriteriaItemService criteriaItemService;

    public ElasticsearchConnectionTester(CriteriaItemService criteriaItemService) {
        this.criteriaItemService = criteriaItemService;
    }

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("Confirming connection to Elasticsearch");
        try {
            criteriaItemService.findAll(PageRequest.of(0, 1));
        } catch (DataAccessResourceFailureException e) {
            throw new IllegalStateException("Failed to connect to Elasticsearch.", e);
        }
    }
}

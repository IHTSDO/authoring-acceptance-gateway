package org.snomed.aag.config;

import org.snomed.aag.config.elasticsearch.ElasticsearchConfig;
import org.snomed.aag.data.services.CriteriaItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.ContextStackAutoConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.annotation.PostConstruct;
import java.net.ConnectException;

@SpringBootApplication(
		exclude = {
				ElasticsearchDataAutoConfiguration.class,
				ElasticsearchRestClientAutoConfiguration.class,
				ContextStackAutoConfiguration.class,
		}
)
@EnableElasticsearchRepositories(
		basePackages = {
				"org.snomed.aag.data.repositories"
		})
@EnableConfigurationProperties
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
public abstract class Config extends ElasticsearchConfig {

	@Autowired
	private CriteriaItemService criteriaItemService;

	@PostConstruct
	public void checkElasticsearchConnection() {
		try {
			criteriaItemService.findAll(PageRequest.of(0, 1));
		} catch (DataAccessResourceFailureException e) {
			throw new IllegalStateException("Failed to connect to Elasticsearch.", e);
		}
	}

}

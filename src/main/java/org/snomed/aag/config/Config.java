package org.snomed.aag.config;

import org.snomed.aag.config.elasticsearch.ElasticsearchConfig;
import org.snomed.aag.data.jira.JiraCloudClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication(
		exclude = {
				ElasticsearchDataAutoConfiguration.class,
				ElasticsearchRestClientAutoConfiguration.class
		}
)
@EnableElasticsearchRepositories(
		basePackages = {
				"org.snomed.aag.data.repositories"
		})
@EnableConfigurationProperties
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
public abstract class Config extends ElasticsearchConfig {

	@Bean
	public JiraCloudClient jiraCloudClient(@Value("${jira.cloud.base-url}") String baseUrl, @Value("${jira.cloud.username}") String email, @Value("${jira.cloud.api-token}") String apiToken) {
		return new JiraCloudClient(baseUrl, email, apiToken);
	}
}

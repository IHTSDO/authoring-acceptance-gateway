package org.snomed.aag;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.config.Config;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;

@PropertySource("classpath:/application.properties")
@PropertySource("classpath:/application-test.properties")
@TestConfiguration
@SpringBootApplication(
		exclude = {
				ElasticsearchRestClientAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class
		})
public class TestConfig extends Config {

	private static final String ELASTIC_SEARCH_SERVER_VERSION = "8.11.1";

	// Set to true to use local standalone Elasticsearch instance rather than Docker test container
	static final boolean useLocalElasticsearch = false;

	private static final Logger LOGGER = LoggerFactory.getLogger(TestConfig.class);

	@Container
	private static final ElasticsearchContainer elasticsearchContainer;
	static
	{
		if (useLocalElasticsearch) {
			elasticsearchContainer = null;
		} else {
			if (!DockerClientFactory.instance().isDockerAvailable()) {
				LOGGER.error("No docker client available to run integration tests.");
				LOGGER.info("Integration tests use the TestContainers framework.(https://www.testcontainers.org)");
				LOGGER.info("TestContainers framework requires docker to be installed.(https://www.testcontainers.org/supported_docker_environment)");
				LOGGER.info("You can download docker via (https://docs.docker.com/get-docker)");
				System.exit(-1);
			}
			elasticsearchContainer = new TestElasticsearchContainer();
			elasticsearchContainer.start();
		}
	}

	public static class TestElasticsearchContainer extends ElasticsearchContainer {
		public TestElasticsearchContainer() {
			super("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTIC_SEARCH_SERVER_VERSION);
			// these are mapped ports used by the test container the actual ports used might be different
			this.addFixedExposedPort(9235, 9235);
			this.addFixedExposedPort(9330, 9330);
			this.addEnv("xpack.security.enabled", "false");
			this.addEnv("cluster.name", "integration-test-cluster");
		}
	}

	static ElasticsearchContainer getElasticsearchContainerInstance() {
		return elasticsearchContainer;
	}

	@Override
	public @NotNull ClientConfiguration clientConfiguration() {
		if (!useLocalElasticsearch) {
			assert elasticsearchContainer != null;
			return ClientConfiguration.builder()
					.connectedTo(elasticsearchContainer.getHttpHostAddress())
					.build();
		}
		return ClientConfiguration.builder()
				.connectedToLocalhost()
				.build();
	}
}

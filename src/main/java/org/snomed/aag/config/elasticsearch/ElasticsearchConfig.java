package org.snomed.aag.config.elasticsearch;

import com.google.common.base.Strings;
import io.github.acm19.aws.interceptor.http.AwsRequestSigningApacheV5Interceptor;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElasticsearchConfig extends ElasticsearchConfiguration {

	@Value("${elasticsearch.username}")
	private String elasticsearchUsername;

	@Value("${elasticsearch.password}")
	private String elasticsearchPassword;

	@Value("${elasticsearch.index.prefix}")
	private String indexNamePrefix;

	@Value("${elasticsearch.index.app.prefix}")
	private String indexNameApplicationPrefix;

	@Value("${elasticsearch.api-key}")
	private String apiKey;

	@Value("${aag.aws.request-signing.enabled}")
	private Boolean awsRequestSigning;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public ClientConfiguration clientConfiguration() {
		final String[] urls = elasticsearchProperties().getUrls();
		for (String url : urls) {
			logger.info("Elasticsearch host: {}", url);
		}

		logger.info("Elasticsearch index prefix: {}", indexNamePrefix);
		logger.info("Elasticsearch index application prefix: {}", indexNameApplicationPrefix);

		boolean useApiKey = !Strings.isNullOrEmpty(apiKey);
		HttpHeaders apiKeyHeaders = new HttpHeaders();
		if (useApiKey) {
			logger.info("Using API key authentication.");
			apiKeyHeaders.add("Authorization", "ApiKey " + apiKey);
		}

		ClientConfiguration.MaybeSecureClientConfigurationBuilder hostBuilder =
				ClientConfiguration.builder().connectedTo(getHosts(urls));
		ClientConfiguration.TerminalClientConfigurationBuilder builder =
				useHttps(urls) ? hostBuilder.usingSsl() : hostBuilder;

		builder.withDefaultHeaders(apiKeyHeaders)
				// Disable lease handling for the connection pool! See https://github.com/elastic/elasticsearch/issues/24069
				.withClientConfigurer(connectionRequestTimeout());

		if (!Strings.isNullOrEmpty(elasticsearchUsername) && !Strings.isNullOrEmpty(elasticsearchPassword)) {
			// withBasicAuth is applied as an Authorization header when the configuration is built, which would replace
			// the API key header set above, so only apply it when no API key is configured. The API key takes precedence.
			if (useApiKey) {
				logger.warn("Both elasticsearch.api-key and elasticsearch.username/password are set. " +
						"The API key takes precedence and basic authentication will not be used.");
			} else {
				logger.info("Using basic authentication.");
				builder.withBasicAuth(elasticsearchUsername, elasticsearchPassword);
			}
		}

		if (awsRequestSigning != null && awsRequestSigning) {
			logger.info("Signing Elasticsearch requests with AWS credentials.");
			builder.withClientConfigurer(awsRequestSigning());
		}

		return builder.build();
	}

	private boolean useHttps(String[] urls) {
		for (String url : urls) {
			if (url.startsWith("https://")) {
				return true;
			}
		}
		return false;
	}

	private Rest5Clients.ElasticsearchRequestConfigCallback connectionRequestTimeout() {
		return Rest5Clients.ElasticsearchRequestConfigCallback.from(requestConfigBuilder ->
				requestConfigBuilder.setConnectionRequestTimeout(Timeout.ofMilliseconds(0)));
	}

	private Rest5Clients.ElasticsearchHttpClientConfigurationCallback awsRequestSigning() {
		return Rest5Clients.ElasticsearchHttpClientConfigurationCallback.from(httpClientBuilder ->
				httpClientBuilder.addRequestInterceptorFirst(awsInterceptor("es")));
	}

	private AwsRequestSigningApacheV5Interceptor awsInterceptor(String serviceName) {
		return new AwsRequestSigningApacheV5Interceptor(
				serviceName,
				Aws4Signer.create(),
				DefaultCredentialsProvider.create(),
				DefaultAwsRegionProviderChain.builder().build().getRegion()
		);
	}

	private String[] getHosts(String[] urls) {
		List<String> hosts = new ArrayList<>();
		for (String url : urls) {
			try {
				hosts.add(HttpHost.create(url).toHostString());
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Invalid Elasticsearch URL configured: " + url, e);
			}
		}
		return hosts.toArray(new String[]{});
	}

	@Bean
	public ElasticsearchProperties elasticsearchProperties() {
		return new ElasticsearchProperties();
	}

	@Bean
	public IndexNameProvider indexNameProvider(ElasticsearchProperties elasticsearchProperties) {
		return new IndexNameProvider(elasticsearchProperties);
	}

	@Bean
	public ElasticsearchCustomConversions elasticsearchCustomConversions() {
		return new ElasticsearchCustomConversions(
				Arrays.asList(new DateToLongConverter(), new LongToDateConverter()));
	}

}

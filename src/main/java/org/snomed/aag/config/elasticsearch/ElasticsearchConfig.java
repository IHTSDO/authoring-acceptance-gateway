package org.snomed.aag.config.elasticsearch;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.AWSRequestSigningApacheInterceptor;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

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

		return ClientConfiguration.builder()
				.connectedTo(getHosts(elasticsearchProperties().getUrls()))
				.usingSsl("17723d59526b1454fc5ae88aff0089ac1e406e5d5cd907d47000d31729125b8f")
				.withBasicAuth(elasticsearchUsername, elasticsearchPassword)
				.withClientConfigurer(ElasticsearchClients.ElasticsearchRestClientConfigurationCallback
						.from(restClientBuilder -> {
							restClientBuilder.setRequestConfigCallback(builder -> {
								builder.setConnectionRequestTimeout(0); //Disable lease handling for the connection pool! See https://github.com/elastic/elasticsearch/issues/24069
								return builder;
							});

							if (awsRequestSigning != null && awsRequestSigning) {
								restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.addInterceptorLast(awsInterceptor("es")));
							}
							return restClientBuilder;
						}))
				.build();
	}

	private AWSRequestSigningApacheInterceptor awsInterceptor(String serviceName) {
		AWS4Signer signer = new AWS4Signer();
		DefaultAwsRegionProviderChain regionProviderChain = new DefaultAwsRegionProviderChain();
		DefaultAWSCredentialsProviderChain credentialsProvider = new DefaultAWSCredentialsProviderChain();
		signer.setServiceName(serviceName);
		signer.setRegionName(regionProviderChain.getRegion());

		return new AWSRequestSigningApacheInterceptor(serviceName, signer, credentialsProvider);
	}

	private String[] getHosts(String[] urls) {
		List<String> hosts = new ArrayList<>();
		for (String url : urls) {
			hosts.add(HttpHost.create(url).toHostString());
		}
		return hosts.toArray(new String[]{});
	}

	@Bean
	public ElasticsearchProperties elasticsearchProperties() {
		return new ElasticsearchProperties();
	}

	@Bean
	public ElasticsearchConverter elasticsearchConverter() {
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		MappingElasticsearchConverter elasticsearchConverter = new MappingElasticsearchConverter(mappingContext);
		elasticsearchConverter.setConversions(elasticsearchCustomConversions());
		return elasticsearchConverter;
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

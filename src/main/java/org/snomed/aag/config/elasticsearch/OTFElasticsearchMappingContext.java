package org.snomed.aag.config.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchPersistentEntity;
import org.springframework.data.util.TypeInformation;

public class OTFElasticsearchMappingContext extends SimpleElasticsearchMappingContext implements ApplicationContextAware {

	private final IndexConfig indexConfig;
	private ApplicationContext context;
	private static final Logger LOGGER = LoggerFactory.getLogger(OTFElasticsearchMappingContext.class);

	public OTFElasticsearchMappingContext(IndexConfig indexConfig) {
		this.indexConfig = indexConfig;
	}

	@Override
	protected <T> SimpleElasticsearchPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		SimpleElasticsearchPersistentEntity<T> persistentEntity = new OTFSimpleElasticsearchPersistentEntity<>(indexConfig, typeInformation);
		if (this.context != null) {
			persistentEntity.setApplicationContext(this.context);
		}
		return persistentEntity;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
}

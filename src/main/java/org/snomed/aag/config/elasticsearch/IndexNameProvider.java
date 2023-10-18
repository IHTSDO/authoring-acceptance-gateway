package org.snomed.aag.config.elasticsearch;

import com.google.common.base.Strings;

public class IndexNameProvider {

	private final String prefix;

	public IndexNameProvider(ElasticsearchProperties elasticsearchProperties) {
		this.prefix = elasticsearchProperties.getIndex().getPrefix() + elasticsearchProperties.getIndex().getApp().getPrefix();
	}

	public String getIndexNameWithPrefix(String indexName) {
		return Strings.isNullOrEmpty(prefix) ? indexName : prefix + indexName;
	}

}

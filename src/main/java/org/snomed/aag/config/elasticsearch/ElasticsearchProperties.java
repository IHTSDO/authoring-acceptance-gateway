package org.snomed.aag.config.elasticsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("elasticsearch")
public class ElasticsearchProperties {

	private String[] urls;
	private IndexProperties index = new IndexProperties();

	public String[] getUrls() {
		return urls;
	}

	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	public IndexProperties getIndex() {
		return index;
	}

	public void setIndex(IndexProperties index) {
		this.index = index;
	}

	public static class IndexProperties {

		private String prefix;

		private App app;

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public App getApp() {
			return app;
		}

		public void setApp(App app) {
			this.app = app;
		}

		public static class App {
			private String prefix;

			public String getPrefix() {
				return prefix;
			}

			public void setPrefix(String prefix) {
				this.prefix = prefix;
			}
		}

	}
}

package org.snomed.aag.data.jira;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "aag.jira.configuration")
public class JiraConfigMapping {

	private Map <String, String> snomedCtProducts;

	public Map <String, String> getSnomedCtProducts() {
		return snomedCtProducts;
	}

	public void setSnomedCtProducts(Map <String, String> snomedCtProducts) {
		this.snomedCtProducts = snomedCtProducts;
	}
}

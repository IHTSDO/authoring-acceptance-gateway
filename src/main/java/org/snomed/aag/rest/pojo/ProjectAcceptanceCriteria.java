package org.snomed.aag.rest.pojo;

import org.snomed.aag.data.domain.CriteriaItem;

import java.util.List;

public class ProjectAcceptanceCriteria {

	private String projectKey;

	private List<CriteriaItem> criteriaItems;

	public ProjectAcceptanceCriteria(String projectKey, List<CriteriaItem> criteriaItems) {
		this.projectKey = projectKey;
		this.criteriaItems = criteriaItems;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public List<CriteriaItem> getCriteriaItems() {
		return criteriaItems;
	}
}

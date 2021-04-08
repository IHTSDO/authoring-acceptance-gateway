package org.snomed.aag.data.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

@Document(indexName = "project-criteria")
public class ProjectAcceptanceCriteria {

	@Id
	@Field(type = FieldType.Keyword)
	@NotBlank
	private String branchPath;

	@Field(type = FieldType.Keyword)
	private Set<String> selectedProjectCriteriaIds;

	@Field(type = FieldType.Keyword)
	private Set<String> selectedTaskCriteriaIds;

	private ProjectAcceptanceCriteria() {
	}

	public ProjectAcceptanceCriteria(String branchPath) {
		this.branchPath = branchPath;
		selectedProjectCriteriaIds = new HashSet<>();
		selectedTaskCriteriaIds = new HashSet<>();
	}

	public String getBranchPath() {
		return branchPath;
	}

	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}

	public Set<String> getSelectedProjectCriteriaIds() {
		return selectedProjectCriteriaIds;
	}

	public void setSelectedProjectCriteriaIds(Set<String> selectedProjectCriteriaIds) {
		this.selectedProjectCriteriaIds = selectedProjectCriteriaIds;
	}

	public Set<String> getSelectedTaskCriteriaIds() {
		return selectedTaskCriteriaIds;
	}

	public void setSelectedTaskCriteriaIds(Set<String> selectedTaskCriteriaIds) {
		this.selectedTaskCriteriaIds = selectedTaskCriteriaIds;
	}

	@JsonIgnore
	public Set<String> getAllCriteriaIdentifiers() {
		Set<String> identifiers = new HashSet<>();
		identifiers.addAll(selectedProjectCriteriaIds);
		identifiers.addAll(selectedTaskCriteriaIds);

		return identifiers;
	}

	public void addToSelectedProjectCriteria(CriteriaItem criteriaItem) {
		if (AuthoringLevel.PROJECT.equals(criteriaItem.getAuthoringLevel())) {
			this.selectedProjectCriteriaIds.add(criteriaItem.getId());
		}
	}
}

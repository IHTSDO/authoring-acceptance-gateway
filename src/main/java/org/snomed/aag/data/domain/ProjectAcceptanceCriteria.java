package org.snomed.aag.data.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Document(indexName = "project-criteria")
public class ProjectAcceptanceCriteria {

	@Id
	@Field(type = FieldType.Keyword)
	private String key; //Composite of branchPath and projectIteration

	@Field(type = FieldType.Keyword)
	private String branchPath;

	@Field(type = FieldType.Integer)
	private Integer projectIteration;

	@Field(type = FieldType.Keyword)
	private Set<String> selectedProjectCriteriaIds;

	@Field(type = FieldType.Keyword)
	private Set<String> selectedTaskCriteriaIds;

	private ProjectAcceptanceCriteria() {
	}

	public ProjectAcceptanceCriteria(String branchPath) {
		this.branchPath = branchPath;
		this.selectedProjectCriteriaIds = new HashSet<>();
		this.selectedTaskCriteriaIds = new HashSet<>();
		this.key = this.branchPath + "_" + this.projectIteration;
	}

	public ProjectAcceptanceCriteria(String branchPath, Integer projectIteration) {
		this.branchPath = branchPath;
		this.projectIteration = projectIteration;
		this.selectedProjectCriteriaIds = new HashSet<>();
		this.selectedTaskCriteriaIds = new HashSet<>();
		this.key = this.branchPath + "_" + this.projectIteration;
	}

	public String getBranchPath() {
		return branchPath;
	}

	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
		this.key = this.branchPath + "_"; //first half
	}

	public Integer getProjectIteration() {
		return projectIteration;
	}

	public void setProjectIteration(Integer projectIteration) {
		this.projectIteration = projectIteration;
		this.key = this.key + this.projectIteration; //second half
	}

	public Set<String> getSelectedProjectCriteriaIds() {
		if (selectedProjectCriteriaIds == null) {
			return Collections.emptySet();
		}

		return selectedProjectCriteriaIds;
	}

	public void setSelectedProjectCriteriaIds(Set<String> selectedProjectCriteriaIds) {
		this.selectedProjectCriteriaIds = selectedProjectCriteriaIds;
	}

	public Set<String> getSelectedTaskCriteriaIds() {
		if (selectedTaskCriteriaIds == null) {
			return Collections.emptySet();
		}

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
			if (this.selectedProjectCriteriaIds == null) {
				this.selectedProjectCriteriaIds = new HashSet<>();
			}
			this.selectedProjectCriteriaIds.add(criteriaItem.getId());
		}
	}

	public void addToSelectedTaskCriteria(CriteriaItem criteriaItem) {
		if (AuthoringLevel.TASK.equals(criteriaItem.getAuthoringLevel())) {
			if (this.selectedTaskCriteriaIds == null) {
				this.selectedTaskCriteriaIds = new HashSet<>();
			}
			this.selectedTaskCriteriaIds.add(criteriaItem.getId());
		}
	}
}

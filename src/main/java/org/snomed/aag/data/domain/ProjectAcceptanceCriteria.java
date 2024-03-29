package org.snomed.aag.data.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.snomed.aag.rest.util.PathUtil;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Document(indexName = "#{@indexNameProvider.getIndexNameWithPrefix('project-criteria')}")
@Setting(settingPath = "elasticsearch-settings.json")
public class ProjectAcceptanceCriteria {

	@Id
	@Field(type = FieldType.Keyword)
	private String key; //Composite of branchPath and projectIteration

	@Field(type = FieldType.Keyword)
	private String branchPath;

	@Field(type = FieldType.Integer)
	private Integer projectIteration;

	@Field(type = FieldType.Long)
	private Date creationDate;

	@Field(type = FieldType.Keyword)
	private Set<String> selectedProjectCriteriaIds;

	@Field(type = FieldType.Keyword)
	private Set<String> selectedTaskCriteriaIds;

	@Transient
	private boolean batch;

	public ProjectAcceptanceCriteria() {
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

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@JsonProperty(value = "creationDateLong")
	public Long getCreationDateLong() {
		if (this.creationDate == null) {
			return null;
		}

		return this.creationDate.getTime();
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

	public void setSelectedCriteria(Set<CriteriaItem> criteriaItems) {
		this.selectedProjectCriteriaIds = new HashSet<>();
		this.selectedTaskCriteriaIds = new HashSet<>();

		for (CriteriaItem criteriaItem : criteriaItems) {
			AuthoringLevel authoringLevel = criteriaItem.getAuthoringLevel();
			if (AuthoringLevel.PROJECT.equals(authoringLevel)) {
				this.selectedProjectCriteriaIds.add(criteriaItem.getId());
			} else {
				this.selectedTaskCriteriaIds.add(criteriaItem.getId());
			}
		}
	}

	public boolean isBranchProjectLevel(String branchPath) {
		return getBranchPath().equals(branchPath);
	}

	public boolean isBranchTaskLevel(String branchPath) {
		return getBranchPath().equals(PathUtil.getParentPath(branchPath));
	}

	public ProjectAcceptanceCriteria cloneWithNextProjectIteration() {
		ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria();
		projectAcceptanceCriteria.setBranchPath(this.branchPath);
		projectAcceptanceCriteria.setProjectIteration(this.projectIteration + 1);
		projectAcceptanceCriteria.setSelectedProjectCriteriaIds(this.selectedProjectCriteriaIds);
		projectAcceptanceCriteria.setSelectedTaskCriteriaIds(this.selectedTaskCriteriaIds);

		return projectAcceptanceCriteria;
	}

	@JsonIgnore
	public boolean isBatch() {
		return batch;
	}

	public void setBatch(boolean batch) {
		this.batch = batch;
	}

	@Override
	public String toString() {
		return "ProjectAcceptanceCriteria{" +
				"key='" + key + '\'' +
				", branchPath='" + branchPath + '\'' +
				", projectIteration=" + projectIteration +
				", creationDate =" + creationDate +
				", selectedProjectCriteriaIds=" + selectedProjectCriteriaIds +
				", selectedTaskCriteriaIds=" + selectedTaskCriteriaIds +
				'}';
	}
}

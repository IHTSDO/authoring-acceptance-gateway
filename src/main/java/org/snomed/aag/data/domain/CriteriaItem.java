package org.snomed.aag.data.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Document(indexName = "#{@indexNameProvider.getIndexNameWithPrefix('criteria-item')}")
@Setting(settingPath = "elasticsearch-settings.json")
public class CriteriaItem implements Comparable<CriteriaItem> {

	public static final String PROJECT_CLASSIFICATION_CLEAN = "project-classification-clean";
	public static final String TASK_CLASSIFICATION_CLEAN = "task-classification-clean";

	public static final String PROJECT_VALIDATION_CLEAN = "project-validation-clean";
	public static final String PROJECT_VALIDATION_CLEAN_MS = "project-validation-clean-ms";
	public static final String TASK_VALIDATION_CLEAN = "task-validation-clean";
	public static final String TASK_VALIDATION_CLEAN_MS = "task-validation-clean-ms";

	@Id
	@Field(type = FieldType.Keyword)
	@NotBlank
	private String id;

	@Field(type = FieldType.Text)
	@NotBlank
	private String label;

	@Field(type = FieldType.Text)
	private String description;

	@Field(type = FieldType.Integer)
	private int order;

	@Field(type = FieldType.Keyword)
	private AuthoringLevel authoringLevel;

	@Field(type = FieldType.Boolean)
	private boolean mandatory;

	@Field(type = FieldType.Boolean)
	private boolean manual;

	@Field(type = FieldType.Boolean)
	private boolean expiresOnCommit;

	@Deprecated
	@Field(type = FieldType.Keyword)
	private String requiredRole;

	@Field(type = FieldType.Keyword)
	private Set<String> requiredRoles;

	@Field(type = FieldType.Keyword)
	private Set<String> enabledByFlag = new HashSet<>();

	@Field(type = FieldType.Text)
	private String reportName;

	@Field(type = FieldType.Keyword)
	private Set<String> forCodeSystems;

	@Field(type = FieldType.Keyword)
	private Set<String> notForCodeSystems;

	@Transient
	private boolean complete;

	public CriteriaItem() {
	}

	public CriteriaItem(String id) {
		this.id = id;
	}

	public CriteriaItem(String id, AuthoringLevel authoringLevel, boolean mandatory, boolean manual, boolean expiresOnCommit) {
		this.id = id;
		this.authoringLevel = authoringLevel;
		this.mandatory = mandatory;
		this.manual = manual;
		this.expiresOnCommit = expiresOnCommit;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public AuthoringLevel getAuthoringLevel() {
		return authoringLevel;
	}

	public void setAuthoringLevel(AuthoringLevel authoringLevel) {
		this.authoringLevel = authoringLevel;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public boolean isManual() {
		return manual;
	}

	public void setManual(boolean manual) {
		this.manual = manual;
	}

	public boolean isExpiresOnCommit() {
		return expiresOnCommit;
	}

	public void setExpiresOnCommit(boolean expiresOnCommit) {
		this.expiresOnCommit = expiresOnCommit;
	}

	@Deprecated
	public String getRequiredRole() {
		return requiredRole;
	}

	@Deprecated
	public void setRequiredRole(String requiredRole) {
		this.requiredRole = requiredRole;
	}

	public Set<String> getRequiredRoles() {
		return requiredRoles;
	}

	public void setRequiredRoles(Set<String> requiredRoles) {
		this.requiredRoles = requiredRoles;
	}

	public Set<String> getEnabledByFlag() {
		return enabledByFlag;
	}

	public void setEnabledByFlag(Set<String> enabledByFlag) {
		this.enabledByFlag = enabledByFlag;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getReportName() {
		return reportName;
	}

	public void setForCodeSystems(Set <String> forCodeSystems) {
		this.forCodeSystems = forCodeSystems;
	}

	public Set <String> getForCodeSystems() {
		return forCodeSystems;
	}

	public void setNotForCodeSystems(Set <String> notForCodeSystems) {
		this.notForCodeSystems = notForCodeSystems;
	}

	public Set <String> getNotForCodeSystems() {
		return notForCodeSystems;
	}

	@Override
	public String toString() {
		return "CriteriaItem{" +
				"id='" + id + '\'' +
				", label='" + label + '\'' +
				", description='" + description + '\'' +
				", order=" + order +
				", authoringLevel=" + authoringLevel +
				", mandatory=" + mandatory +
				", manual=" + manual +
				", expiresOnCommit=" + expiresOnCommit +
				", requiredRoles='" + requiredRoles + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CriteriaItem that = (CriteriaItem) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public int compareTo(CriteriaItem that) {
		if (this.getId().equals(that.getId())) {
			return 0;
		}

		int o1Order = this.getOrder();
		int o2Order = that.getOrder();

		if (o1Order > o2Order) {
			return 1;
		}

		if (o1Order < o2Order) {
			return -1;
		}

		return this.getId().compareTo(that.getId());
	}
}

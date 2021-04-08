package org.snomed.aag.data.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotBlank;
import java.util.Objects;

@Document(indexName = "criteria-item")
public class CriteriaItem implements Comparable<CriteriaItem> {

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

	@Field(type = FieldType.Keyword)
	private String requiredRole;

	@Transient
	private boolean complete;

	private CriteriaItem() {
	}

	public CriteriaItem(String id) {
		this.id = id;
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

	public String getRequiredRole() {
		return requiredRole;
	}

	public void setRequiredRole(String requiredRole) {
		this.requiredRole = requiredRole;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
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
				", requiredRole='" + requiredRole + '\'' +
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

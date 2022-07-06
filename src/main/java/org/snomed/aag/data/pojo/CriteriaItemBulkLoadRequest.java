package org.snomed.aag.data.pojo;

import com.fasterxml.jackson.annotation.JsonSetter;
import org.snomed.aag.data.domain.CriteriaItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CriteriaItemBulkLoadRequest {

	private List<CriteriaItem> criteriaItems;

	public CriteriaItemBulkLoadRequest() {
		criteriaItems = new ArrayList<>();
	}

	public CriteriaItemBulkLoadRequest(List<CriteriaItem> criteriaItems) {
		this.criteriaItems = criteriaItems;
	}

	public List<CriteriaItem> getCriteriaItems() {
		return criteriaItems;
	}

	@JsonSetter(value = "criteriaItems")
	public void setCriteriaItemsSafely(List<CriteriaItem> criteriaItems) {
		criteriaItems.removeIf(Objects::isNull);
		this.criteriaItems = criteriaItems;
	}
}

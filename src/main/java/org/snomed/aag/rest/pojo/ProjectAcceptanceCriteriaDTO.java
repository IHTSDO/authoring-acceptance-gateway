package org.snomed.aag.rest.pojo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.snomed.aag.data.domain.CriteriaItem;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectAcceptanceCriteriaDTO {
    private static final Comparator<CriteriaItem> COMPARATOR = CriteriaItem::compareTo;

    private final String projectKey;

    @JsonDeserialize(as = TreeSet.class)
    private final SortedSet<CriteriaItem> criteriaItems = new TreeSet<>(COMPARATOR);

    //Default constructor for (de)serialisation.
    public ProjectAcceptanceCriteriaDTO() {
        this.projectKey = null;
    }

    public ProjectAcceptanceCriteriaDTO(String projectKey, Set<CriteriaItem> criteriaItems) {
        this.projectKey = projectKey;
        this.criteriaItems.addAll(criteriaItems);
    }

    public String getProjectKey() {
        return projectKey;
    }

    public Set<CriteriaItem> getCriteriaItems() {
        return criteriaItems;
    }

    public int getNumberOfCriteriaItemsWithCompletedValue(boolean completed) {
        if (criteriaItems.isEmpty()) {
            return 0;
        }

        List<CriteriaItem> numberOfCriteriaItemsWithCompletedValue =
                criteriaItems
                        .stream()
                        .filter(criteriaItem -> criteriaItem.isComplete() == completed)
                        .collect(Collectors.toList());

        return numberOfCriteriaItemsWithCompletedValue.size();
    }
}

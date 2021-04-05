package org.snomed.aag.rest.pojo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectAcceptanceCriteriaDTO {
    private static final Comparator<CriteriaItemDTO> COMPARATOR = CriteriaItemDTO::compareTo;

    private final String projectKey;

    @JsonDeserialize(as = TreeSet.class)
    private final SortedSet<CriteriaItemDTO> criteriaItems = new TreeSet<>(COMPARATOR);

    //Default constructor for (de)serialisation.
    public ProjectAcceptanceCriteriaDTO() {
        this.projectKey = null;
    }

    public ProjectAcceptanceCriteriaDTO(String projectKey, Set<CriteriaItemDTO> criteriaItems) {
        this.projectKey = projectKey;
        this.criteriaItems.addAll(criteriaItems);
    }

    public String getProjectKey() {
        return projectKey;
    }

    public Set<CriteriaItemDTO> getCriteriaItems() {
        return criteriaItems;
    }

    public int getNumberOfCriteriaItemsWithCompletedValue(boolean completed) {
        if (criteriaItems.isEmpty()) {
            return 0;
        }

        List<CriteriaItemDTO> numberOfCriteriaItemsWithCompletedValue =
                criteriaItems
                        .stream()
                        .filter(criteriaItem -> criteriaItem.isCompleted() == completed)
                        .collect(Collectors.toList());

        return numberOfCriteriaItemsWithCompletedValue.size();
    }
}

package org.snomed.aag.rest.pojo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.snomed.aag.data.domain.CriteriaItem;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectAcceptanceCriteriaDTO {
    private static final Comparator<CriteriaItem> COMPARATOR = CriteriaItem::compareTo;

    private final String branchPath;

    @JsonDeserialize(as = TreeSet.class)
    private final SortedSet<CriteriaItem> criteriaItems = new TreeSet<>(COMPARATOR);

    //Default constructor for (de)serialisation.
    public ProjectAcceptanceCriteriaDTO() {
        this.branchPath = null;
    }

    public ProjectAcceptanceCriteriaDTO(String branchPath, Set<CriteriaItem> criteriaItems) {
        this.branchPath = branchPath;
        this.criteriaItems.addAll(criteriaItems);
    }

    public String getBranchPath() {
        return branchPath;
    }

    public Set<CriteriaItem> getCriteriaItems() {
        return criteriaItems;
    }
}

package org.snomed.aag.data.validators;

import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

@Component
public class ProjectAcceptanceCriteriaCreateValidator {
    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    public ProjectAcceptanceCriteriaCreateValidator(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    /**
     * Validate the given ProjectAcceptanceCriteria.
     *
     * @param projectAcceptanceCriteria ProjectAcceptanceCriteria to validate.
     * @throws IllegalArgumentException If ProjectAcceptanceCriteria is invalid.
     */
    public void validate(ProjectAcceptanceCriteria projectAcceptanceCriteria) {
        if (projectAcceptanceCriteria == null) {
            throw new IllegalArgumentException();
        }

        //Validate project iteration
        Integer projectIteration = projectAcceptanceCriteria.getProjectIteration();
        if (projectIteration == null) {
            throw new IllegalArgumentException("Project iteration is required.");
        }

        if (projectIteration < 0) {
            throw new IllegalArgumentException("Project iteration cannot be less than 0.");
        }

        //Validate missing criteria items
        Set<String> allIds = new HashSet<>();
        allIds.addAll(projectAcceptanceCriteria.getSelectedProjectCriteriaIds());
        allIds.addAll(projectAcceptanceCriteria.getSelectedTaskCriteriaIds());
        if (!allIds.isEmpty()) {
            final Iterable<CriteriaItem> found = findAllById(allIds);
            for (CriteriaItem criteriaItem : found) {
                allIds.remove(criteriaItem.getId());
            }
            if (!allIds.isEmpty()) {
                throw new IllegalArgumentException(format("The following criteria items were not found: %s", allIds));
            }
        }
    }

    // This method is required because the default Repository implementation uses a multi-get request
    // which is blocked by SI AWS index prefix security settings
    private List<CriteriaItem> findAllById(Set<String> allIds) {
        List<CriteriaItem> all = new ArrayList<>();
        final NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(termsQuery("id", allIds))
                .withPageable(Constants.LARGE_PAGE)
                .build();
        try (final SearchHitsIterator<CriteriaItem> hits = elasticsearchRestTemplate.searchForStream(query, CriteriaItem.class)) {
            hits.forEachRemaining(item -> all.add(item.getContent()));
        }
        return all;
    }
}

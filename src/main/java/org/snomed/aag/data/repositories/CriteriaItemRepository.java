package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CriteriaItemRepository extends ElasticsearchRepository<CriteriaItem, String> {
    List<CriteriaItem> findAllByMandatoryAndAuthoringLevel(boolean mandatory, AuthoringLevel authoringLevel);

    Set<CriteriaItem> findAllByIdIn(Collection<String> criteriaItemIdentifiers);

    List<CriteriaItem> findAllByEnabledByFlagInAndAuthoringLevel(Set<String> authorFlags, AuthoringLevel authoringLevel);
}

package org.snomed.aag.data.repositories;

import org.snomed.aag.domain.TestEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TestEntityRepository extends ElasticsearchRepository<TestEntity, String> {
}

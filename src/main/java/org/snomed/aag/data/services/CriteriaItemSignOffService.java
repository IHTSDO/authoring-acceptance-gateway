package org.snomed.aag.data.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.repositories.CriteriaItemSignOffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CriteriaItemSignOffService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CriteriaItemSignOffService.class);

    @Autowired
    private CriteriaItemSignOffRepository repository;

    public CriteriaItemSignOff create(CriteriaItemSignOff criteriaItemSignOff) {
        LOGGER.info("{} signing off {} for {}.", criteriaItemSignOff.getUserId(), criteriaItemSignOff.getCriteriaItemId(), criteriaItemSignOff.getBranch());
        return repository.save(criteriaItemSignOff);
    }

    public Optional<CriteriaItemSignOff> findByBranchAndCriteriaItemId(String branchPath, String criteriaItemIdentifier) {
        LOGGER.info("Looking for {} on branch {}.", criteriaItemIdentifier, branchPath);
        return repository.findByBranchAndCriteriaItemId(branchPath, criteriaItemIdentifier);
    }
}

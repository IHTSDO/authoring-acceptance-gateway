package org.snomed.aag.data.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.repositories.CriteriaItemSignOffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CriteriaItemSignOffService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CriteriaItemSignOffService.class);

    @Autowired
    private CriteriaItemSignOffRepository repository;

    /**
     * Save entry in database.
     *
     * @param criteriaItemSignOff Entry to save in database.
     * @return Saved entry in database.
     * @throws ServiceRuntimeException If there is already a CriteriaItemSignOff entry in database.
     */
    public CriteriaItemSignOff create(CriteriaItemSignOff criteriaItemSignOff) {
        LOGGER.info("{} signing off {} for {}.", criteriaItemSignOff.getUserId(), criteriaItemSignOff.getCriteriaItemId(), criteriaItemSignOff.getBranch());
        return repository.save(criteriaItemSignOff);
    }

    /**
     * Find all CriteriaItemSignOff from the given branch and collection containing possible corresponding CriteriaItems.
     * If there is a CriteriaItemSignOff record found, the corresponding CriteriaItem will be updated accordingly.
     *
     * @param branchPath    Branch to match against.
     * @param criteriaItems Collection of possible matches.
     * @return Collection of matched CriteriaItemSign.
     */
    public List<CriteriaItemSignOff> findAllByBranchAndIdentifier(String branchPath, Set<CriteriaItem> criteriaItems) {
        List<String> criteriaItemIdentifiers = criteriaItems.stream().map(CriteriaItem::getId).collect(Collectors.toList());
        LOGGER.info("Checking whether Criteria Items {} have been completed on branch {}.", criteriaItemIdentifiers, branchPath);

        List<CriteriaItemSignOff> criteriaItemSignOffs = repository.findAllByBranchAndCriteriaItemIdIn(branchPath, criteriaItemIdentifiers);
        for (CriteriaItemSignOff criteriaItemSignOff : criteriaItemSignOffs) {
            String criteriaItemSignOffId = criteriaItemSignOff.getCriteriaItemId();
            for (CriteriaItem criteriaItem : criteriaItems) {
                if (criteriaItem.getId().equals(criteriaItemSignOffId)) {
                    criteriaItem.setComplete(true);
                }
            }
        }

        LOGGER.debug("Branch {} has {} completed Criteria Items.", branchPath, criteriaItemSignOffs.size());
        return criteriaItemSignOffs;
    }

    /**
     * Find entry in database where entry matches query.
     *
     * @param criteriaItemId   Field used in query.
     * @param branchPath       Field used in query.
     * @param projectIteration Field used in query.
     * @return Entry in database where entry matches query.
     */
    public Optional<CriteriaItemSignOff> findBy(String criteriaItemId, String branchPath, Integer projectIteration) {
        if (criteriaItemId == null || branchPath == null || projectIteration == null || projectIteration < 0) {
            throw new IllegalArgumentException();
        }

        return repository.findByCriteriaItemIdAndBranchAndProjectIteration(criteriaItemId, branchPath, projectIteration);
    }

    /**
     * Delete entry from database where entry matches query.
     *
     * @param criteriaItemId   Field used in query.
     * @param branchPath       Field used in query.
     * @param projectIteration Field used in query.
     * @return Whether the entry in database matching query has been deleted from the database.
     */
    public boolean deleteBy(String criteriaItemId, String branchPath, Integer projectIteration) {
        if (criteriaItemId == null || branchPath == null || projectIteration == null || projectIteration < 0) {
            throw new IllegalArgumentException();
        }

        Optional<CriteriaItemSignOff> existingCriteriaItemSignOff = findBy(criteriaItemId, branchPath, projectIteration);
        if (!existingCriteriaItemSignOff.isPresent()) {
            return false;
        }

        try {
            repository.deleteByCriteriaItemIdAndBranchAndProjectIteration(criteriaItemId, branchPath, projectIteration);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

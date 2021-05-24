package org.snomed.aag.data.services;

import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.repositories.CriteriaItemSignOffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CriteriaItemSignOffService {
    private static final String INVALID_PARAMETERS = "Invalid parameters.";

    @Autowired
    private CriteriaItemSignOffRepository repository;

    private static void verifyParams(CriteriaItemSignOff criteriaItemSignOff) {
        if (criteriaItemSignOff == null) {
            throw new IllegalArgumentException(INVALID_PARAMETERS);
        }
    }

    private void verifyParams(String branchPath, Integer projectIteration, Set<CriteriaItem> criteriaItems) {
        if (branchPath == null || projectIteration == null || projectIteration < 0 || criteriaItems == null || criteriaItems.isEmpty()) {
            throw new IllegalArgumentException(INVALID_PARAMETERS);
        }
    }

    private static void verifyParams(String criteriaItemId, String branchPath, Integer projectIteration) {
        if (criteriaItemId == null || branchPath == null || projectIteration == null || projectIteration < 0) {
            throw new IllegalArgumentException(INVALID_PARAMETERS);
        }
    }

    /**
     * Save entry in database.
     *
     * @param criteriaItemSignOff Entry to save in database.
     * @return Saved entry in database.
     * @throws IllegalArgumentException If argument is invalid.
     * @throws ServiceRuntimeException  If similar entry exists in database.
     */
    public CriteriaItemSignOff create(CriteriaItemSignOff criteriaItemSignOff) {
        verifyParams(criteriaItemSignOff);
        String criteriaItemId = criteriaItemSignOff.getCriteriaItemId();
        String branch = criteriaItemSignOff.getBranch();
        Integer projectIteration = criteriaItemSignOff.getProjectIteration();

        //Cannot have multiple CriteriaItemSignOff with same branch and project iteration
        Optional<CriteriaItemSignOff> existingCriteriaItemSignOff = findByCriteriaItemIdAndBranchPathAndProjectIteration(criteriaItemId, branch, projectIteration);
        if (existingCriteriaItemSignOff.isPresent()) {
            String message = String.format("Criteria Item %s has already been signed off for branch %s and project iteration %d", criteriaItemId, branch, projectIteration);
            throw new ServiceRuntimeException(message, HttpStatus.CONFLICT);
        }

        return repository.save(criteriaItemSignOff);
    }

    /**
     * Find entries in database with matching branchPath, projectIteration and criteriaItemId fields. For each entry found,
     * update the complete flag.
     *
     * @param branchPath       Field to match in query.
     * @param projectIteration Field to match in query.
     * @param criteriaItems    Field to match in query.
     * @return Entries in database with matching branchPath, projectIteration, and criteriaItemId fields.
     * @throws IllegalArgumentException If arguments are invalid.
     */
    public List<CriteriaItemSignOff> findByBranchPathAndProjectIterationAndCriteriaItemId(String branchPath, Integer projectIteration, Set<CriteriaItem> criteriaItems) {
        verifyParams(branchPath, projectIteration, criteriaItems);
        List<String> criteriaItemIdentifiers = criteriaItems.stream().map(CriteriaItem::getId).collect(Collectors.toList());
        List<CriteriaItemSignOff> criteriaItemSignOffs = repository.findAllByBranchAndProjectIterationAndCriteriaItemIdIn(branchPath, projectIteration, criteriaItemIdentifiers);
        for (CriteriaItemSignOff criteriaItemSignOff : criteriaItemSignOffs) {
            String criteriaItemSignOffId = criteriaItemSignOff.getCriteriaItemId();
            for (CriteriaItem criteriaItem : criteriaItems) {
                if (criteriaItem.getId().equals(criteriaItemSignOffId)) {
                    criteriaItem.setComplete(true);
                }
            }
        }

        return criteriaItemSignOffs;
    }

    /**
     * Find entry in database with matching criteriaItemId, branchPath and projectIteration fields.
     *
     * @param criteriaItemId   Field to match in query.
     * @param branchPath       Field to match in query.
     * @param projectIteration Field to match in query.
     * @return Entry in database with matching criteriaItemId, branchPath and projectIteration fields.
     * @throws IllegalArgumentException If arguments are invalid.
     */
    public Optional<CriteriaItemSignOff> findByCriteriaItemIdAndBranchPathAndProjectIteration(String criteriaItemId, String branchPath, Integer projectIteration) {
        verifyParams(criteriaItemId, branchPath, projectIteration);

        return repository.findByCriteriaItemIdAndBranchAndProjectIteration(criteriaItemId, branchPath, projectIteration);
    }

    /**
     * Delete entry from database where entry matches query.
     *
     * @param criteriaItemId   Field used in query.
     * @param branchPath       Field used in query.
     * @param projectIteration Field used in query.
     * @return Whether the entry in database matching query has been deleted from the database.
     * @throws IllegalArgumentException If arguments are invalid.
     */
    public boolean deleteByCriteriaItemIdAndBranchPathAndProjectIteration(String criteriaItemId, String branchPath, Integer projectIteration) {
        verifyParams(criteriaItemId, branchPath, projectIteration);

        Optional<CriteriaItemSignOff> existingCriteriaItemSignOff = findByCriteriaItemIdAndBranchPathAndProjectIteration(criteriaItemId, branchPath, projectIteration);
        if (!existingCriteriaItemSignOff.isPresent()) {
            return false;
        }

        repository.deleteByCriteriaItemIdAndBranchAndProjectIteration(criteriaItemId, branchPath, projectIteration);
        return true;
    }
}

package org.snomed.aag.data.services;

import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.CriteriaItemSignOffFactory;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.repositories.CriteriaItemSignOffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CriteriaItemSignOffService {
    private static final String INVALID_PARAMETERS = "Invalid parameters.";

    @Autowired
    private CriteriaItemSignOffRepository repository;

	@Autowired
	private CriteriaItemSignOffFactory criteriaItemSignOffFactory;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static void verifyParams(String criteriaItemId, String branchPath, Integer projectIteration, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		if (criteriaItemId == null || branchPath == null || (projectIteration != null && projectIteration < 0) || projectAcceptanceCriteria == null) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	private void verifyParams(ProjectAcceptanceCriteria projectAcceptanceCriteria, CriteriaItemSignOff criteriaItemSignOff) {
		if (projectAcceptanceCriteria == null || criteriaItemSignOff == null) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	private void verifyParams(Set<CriteriaItem> criteriaItems, String branchPath, Integer projectIteration, Long branchHeadTimestamp, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		if (criteriaItems == null || criteriaItems.isEmpty() || branchPath == null || (projectIteration != null && projectIteration < 0) && branchHeadTimestamp == null || projectAcceptanceCriteria == null) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	/**
	 * Save entries in store. For each entry, only save if a corresponding entry is not already present.
	 *
	 * @param criteriaItems             Entries to save in store.
	 * @param branchPath                Value to set corresponding field for all entries given.
	 * @param branchHeadTimestamp       Value to set corresponding field for all entries given.
	 * @param projectAcceptanceCriteria Required for finding existing CriteriaItemSignOff.
	 * @return Saved entries in database, including entries previously saved but are still within scope.
	 * @throws IllegalArgumentException If arguments are invalid.
	 */
	public Set<CriteriaItemSignOff> createAll(Set<CriteriaItem> criteriaItems, String branchPath, Integer projectIteration, Long branchHeadTimestamp, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		verifyParams(criteriaItems, branchPath, projectIteration, branchHeadTimestamp, projectAcceptanceCriteria);

		String username = SecurityUtil.getUsername();
		List<CriteriaItemSignOff> toCreate = new ArrayList<>();
		Set<CriteriaItemSignOff> criteriaItemSignOffs = new HashSet<>();
		for (CriteriaItem criteriaItem : criteriaItems) {
			Optional<CriteriaItemSignOff> existingCriteriaItemSignOff = findByCriteriaItemIdAndBranchPathAndProjectIteration(
					criteriaItem.getId(),
					branchPath,
					projectIteration,
					projectAcceptanceCriteria
			);

			// Only create new entries, but return all in scope as to not hide data.
			if (existingCriteriaItemSignOff.isEmpty()) {
				toCreate.add(criteriaItemSignOffFactory.create(criteriaItem.getId(), branchPath, branchHeadTimestamp, projectIteration, username, projectAcceptanceCriteria));
			} else {
				criteriaItemSignOffs.add(existingCriteriaItemSignOff.get());
			}
		}

		Set<CriteriaItemSignOff> created = new HashSet<>();
		for (CriteriaItemSignOff criteriaItemSignOff : repository.saveAll(toCreate)) {
			created.add(criteriaItemSignOff);
		}

		criteriaItemSignOffs.addAll(created);
		return criteriaItemSignOffs;
	}

	/**
	 * Save entry in store.
	 *
	 * @param projectAcceptanceCriteria Required for finding existing CriteriaItemSignOff.
	 * @param criteriaItemSignOff       Entry to save in database.
	 * @return Saved entry in store.
	 * @throws IllegalArgumentException If arguments are invalid.
	 * @throws ServiceRuntimeException  If similar entry exists in database.
	 */
	public CriteriaItemSignOff create(ProjectAcceptanceCriteria projectAcceptanceCriteria, CriteriaItemSignOff criteriaItemSignOff) {
		verifyParams(projectAcceptanceCriteria, criteriaItemSignOff);

		String branch = criteriaItemSignOff.getBranch();
		String criteriaItemId = criteriaItemSignOff.getCriteriaItemId();
		Integer projectIteration = criteriaItemSignOff.getProjectIteration();

		//Cannot have multiple CriteriaItemSignOff with same branch and project iteration
		Optional<CriteriaItemSignOff> existingCriteriaItemSignOff = findByCriteriaItemIdAndBranchPathAndProjectIteration(criteriaItemId, branch, projectIteration, projectAcceptanceCriteria);
		if (existingCriteriaItemSignOff.isPresent()) {
			String message = String.format("Criteria Item %s has already been signed off for branch %s and project iteration %d", criteriaItemId, branch, projectIteration);
			throw new ServiceRuntimeException(message, HttpStatus.CONFLICT);
		}

		logger.info("Creating item sign-offs {} for branch {}, iteration {}", Collections.singleton(criteriaItemId), branch, projectIteration);
		return repository.save(criteriaItemSignOff);
	}

	/**
	 * Create CriteriaItemSignOff for all given CriteriaItem.
	 *
	 * @param itemsToAccept             Identifiers of CriteriaItems to mark as complete.
	 * @param branchPath                Branch path of CriteriaItems to mark as complete.
	 * @param projectIteration          Project iteration of CriteriaItems to mark as complete.
	 * @param headTimestamp             Head timestamp of CriteriaItems to mark as complete.
	 * @param projectAcceptanceCriteria Required for creating CriteriaItemSignOff.
	 */
	public void createFrom(Set<String> itemsToAccept, String branchPath, Integer projectIteration, long headTimestamp, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		final Set<CriteriaItemSignOff> items =
				itemsToAccept
						.stream()
						.map(id -> criteriaItemSignOffFactory.create(id, branchPath, headTimestamp, projectIteration, SecurityUtil.getUsername(), projectAcceptanceCriteria))
						.collect(Collectors.toSet());
		logger.info("Creating item sign-offs {} for branch {}, iteration {}", itemsToAccept, branchPath, projectIteration);
		repository.saveAll(items);
	}

	/**
	 * Find entry in database with matching criteriaItemId, branchPath and projectIteration fields.
	 *
	 * @param criteriaItemId            Field to match in query.
	 * @param branchPath                Field to match in query.
	 * @param projectIteration          Field to match in query.
	 * @param projectAcceptanceCriteria Required for determining which query to run.
	 * @return Entry in database with matching criteriaItemId, branchPath and projectIteration fields.
	 * @throws IllegalArgumentException If arguments are invalid.
	 */
	public Optional<CriteriaItemSignOff> findByCriteriaItemIdAndBranchPathAndProjectIteration(String criteriaItemId, String branchPath, Integer projectIteration, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		verifyParams(criteriaItemId, branchPath, projectIteration, projectAcceptanceCriteria);

		return doFindCriteriaItemSignOff(criteriaItemId, branchPath, projectIteration, projectAcceptanceCriteria);
	}

    /**
     * Find entries in database with matching branchPath, projectIteration and criteriaItemId fields. For each entry found,
     * update the complete flag.
     *
     * @param branchPath       Field to match in query.
     * @param criteriaItems    Field to match in query.
     * @return Entries in database with matching branchPath, projectIteration, and criteriaItemId fields.
     * @throws IllegalArgumentException If arguments are invalid.
     */
	public List<CriteriaItemSignOff> markSignedOffItems(Set<CriteriaItem> criteriaItems, String branchPath, Integer projectIteration, ProjectAcceptanceCriteria criteria) {
		Map<String, CriteriaItem> criteriaItemMap = criteriaItems.stream().collect(Collectors.toMap(CriteriaItem::getId, Function.identity()));
		List<CriteriaItemSignOff> criteriaItemSignOffs = doFindCriteriaItemSignOff(criteriaItemMap.keySet(), branchPath, projectIteration, criteria);
		for (CriteriaItemSignOff criteriaItemSignOff : criteriaItemSignOffs) {
			final CriteriaItem criteriaItem = criteriaItemMap.get(criteriaItemSignOff.getCriteriaItemId());
			if (criteriaItem != null) {
				criteriaItem.setComplete(true);
			}
        }

        return criteriaItemSignOffs;
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
	public boolean deleteByCriteriaItemIdAndBranchPathAndProjectIteration(String criteriaItemId, String branchPath, Integer projectIteration, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		verifyParams(criteriaItemId, branchPath, projectIteration, projectAcceptanceCriteria);

        Optional<CriteriaItemSignOff> existingCriteriaItemSignOff = findByCriteriaItemIdAndBranchPathAndProjectIteration(criteriaItemId, branchPath, projectIteration, projectAcceptanceCriteria);
        if (!existingCriteriaItemSignOff.isPresent()) {
            return false;
        }

        logger.info("Deleting item sign-offs {} for branch {}, iteration {}", Collections.singleton(criteriaItemId), branchPath, projectIteration);
		doDeleteCriteriaItemSignOff(criteriaItemId, branchPath, projectIteration, projectAcceptanceCriteria);
		return true;
    }

	/**
	 * Delete CriteriaItemSignOff for all given CriteriaItem.
	 *
	 * @param itemsToUnaccept           Identifiers of CriteriaItems to mark as incomplete.
	 * @param branchPath                Branch path of CriteriaItems to mark as incomplete.
	 * @param projectIteration          Project iteration of CriteriaItems to mark as incomplete.
	 * @param projectAcceptanceCriteria Determine whether to mark Project Criteria or Task Criteria incomplete.
	 */
	public void deleteFrom(Set<String> itemsToUnaccept, String branchPath, Integer projectIteration, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		logger.info("Deleting item sign-offs {} for branch {}, iteration {}", itemsToUnaccept, branchPath);
		for (String itemId : itemsToUnaccept) {
			doDeleteCriteriaItemSignOff(itemId, branchPath, projectIteration, projectAcceptanceCriteria);
		}
	}

	// Find differently for PROJECT & TASK CriteriaItemSignOff
	private List<CriteriaItemSignOff> doFindCriteriaItemSignOff(Collection<String> criteriaItemIdentifiers, String branchPath, Integer projectIteration, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		boolean branchProjectLevel = projectAcceptanceCriteria.isBranchProjectLevel(branchPath);
		if (branchProjectLevel) {
			return repository.findAllByBranchAndProjectIterationAndCriteriaItemIdIn(branchPath, projectIteration, criteriaItemIdentifiers);
		}

		return repository.findAllByBranchAndCriteriaItemIdIn(branchPath, criteriaItemIdentifiers);
	}

	// Find differently for PROJECT & TASK CriteriaItemSignOff
	private Optional<CriteriaItemSignOff> doFindCriteriaItemSignOff(String criteriaItemId, String branchPath, Integer projectIteration, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		if (projectAcceptanceCriteria.isBranchProjectLevel(branchPath)) {
			return repository.findByCriteriaItemIdAndBranchAndProjectIteration(criteriaItemId, branchPath, projectIteration);
		}

		return repository.findByCriteriaItemIdAndBranch(criteriaItemId, branchPath);
	}

	// Delete differently for PROJECT & TASK CriteriaItemSignOff
	private void doDeleteCriteriaItemSignOff(String criteriaItemId, String branchPath, Integer projectIteration, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		if (projectAcceptanceCriteria.isBranchProjectLevel(branchPath)) {
			repository.deleteByCriteriaItemIdAndBranchAndProjectIteration(criteriaItemId, branchPath, projectIteration);
		} else {
			repository.deleteByCriteriaItemIdAndBranch(criteriaItemId, branchPath);
		}
	}
}

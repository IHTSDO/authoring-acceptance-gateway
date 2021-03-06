package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.repositories.ProjectAcceptanceCriteriaRepository;
import org.snomed.aag.data.validators.ProjectAcceptanceCriteriaCreateValidator;
import org.snomed.aag.rest.util.BranchPathUtil;
import org.snomed.aag.rest.util.MetadataUtil;
import org.snomed.aag.rest.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;

import static java.lang.String.format;
import static org.snomed.aag.data.domain.CriteriaItem.TASK_VALIDATION_CLEAN;

@Service
public class ProjectAcceptanceCriteriaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectAcceptanceCriteriaService.class);
    private static final String INVALID_PARAMETERS = "Invalid parameters.";

    @Autowired
    private ProjectAcceptanceCriteriaRepository repository;

    @Autowired
    private ProjectAcceptanceCriteriaCreateValidator projectAcceptanceCriteriaCreateValidator;

    @Autowired
    private CriteriaItemService criteriaItemService;

	@Autowired
    private CriteriaItemSignOffService criteriaItemSignOffService;

    @Autowired
    private BranchSecurityService branchSecurityService;

	private static void verifyParams(String branchPath, Integer projectIteration) {
        if (branchPath == null || (projectIteration != null && projectIteration < 0)) {
            throw new IllegalArgumentException(INVALID_PARAMETERS);
        }
    }

    private static void verifyParams(PageRequest pageRequest) {
        if (pageRequest == null) {
            throw new IllegalArgumentException(INVALID_PARAMETERS);
        }
    }

    private static void verifyParams(String branchPath) {
        if (branchPath == null) {
            throw new IllegalArgumentException(INVALID_PARAMETERS);
        }
    }

    private static void verifyParams(ProjectAcceptanceCriteria projectAcceptanceCriteria) {
        if (projectAcceptanceCriteria == null) {
            throw new IllegalArgumentException();
        }
    }

    private static void verifyParams(ProjectAcceptanceCriteria projectAcceptanceCriteria, String branchPath) {
        if (projectAcceptanceCriteria == null || branchPath == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Save entry in database.
     *
     * @param projectAcceptanceCriteria Entry to save in database.
     * @throws IllegalArgumentException If entry is not valid.
     * @throws ServiceRuntimeException  If similar entry exists in database.
     */
    public void create(ProjectAcceptanceCriteria projectAcceptanceCriteria) {
        projectAcceptanceCriteriaCreateValidator.validate(projectAcceptanceCriteria);
        String branchPath = projectAcceptanceCriteria.getBranchPath();
        Integer projectIteration = projectAcceptanceCriteria.getProjectIteration();
        ProjectAcceptanceCriteria existing = repository.findByBranchPathAndProjectIteration(branchPath, projectIteration);
        if (existing != null) {
            String message = format("Project Acceptance Criteria already exists for branch %s and iteration %d.", branchPath, projectIteration);
            throw new ServiceRuntimeException(message, HttpStatus.CONFLICT);
        }

        projectAcceptanceCriteria.setCreationDate(new Date());
        repository.save(projectAcceptanceCriteria);
    }

    /**
     * Find entries in database matching page request, and return as new page.
     *
     * @param pageRequest Page configuration for database query.
     * @return Entries in database matching page request.
     * @throws IllegalArgumentException If argument is invalid.
     */
    public Page<ProjectAcceptanceCriteria> findAll(PageRequest pageRequest) {
        verifyParams(pageRequest);
        return repository.findAll(pageRequest);
    }

    /**
     * Find entry in database with matching branchPath and projectIteration fields.
     *
     * @param branchPath       Field to match in query.
     * @param projectIteration Field to match in query.
     * @return Entry in database with matching branchPath and projectIteration fields.
     * @throws IllegalArgumentException If arguments are invalid.
     */
    public ProjectAcceptanceCriteria findByBranchPathAndProjectIteration(String branchPath, Integer projectIteration) {
        verifyParams(branchPath, projectIteration);
        return repository.findByBranchPathAndProjectIteration(branchPath, projectIteration);
    }

    /**
     * Find entry in database with matching branchPath and projectIteration fields. If no entry is found
     * in database matching the query, then throw an exception.
     *
     * @param branchPath       Field to match in query.
     * @param projectIteration Field to match in query.
     * @return Entry in database with matching branchPath and projectIteration fields.
     * @throws IllegalArgumentException If arguments are invalid.
     * @throws ServiceRuntimeException  If no entry found in database matching query.
     */
    public ProjectAcceptanceCriteria findByBranchPathAndProjectIterationOrThrow(String branchPath, Integer projectIteration) {
        ProjectAcceptanceCriteria projectAcceptanceCriteria = findByBranchPathAndProjectIteration(branchPath, projectIteration);
        if (projectAcceptanceCriteria == null) {
            throw new ServiceRuntimeException("Not found", HttpStatus.NOT_FOUND);
        }

        return projectAcceptanceCriteria;
    }

    /**
     * Return ProjectAcceptanceCriteria with relevant CriteriaItems. If no ProjectAcceptanceCriteria can be found from the given
     * branchPath, the parent Branch will be queried.
     *
     * @param branchPath Branch path to query for ProjectAcceptanceCriteria
     * @return ProjectAcceptanceCriteria with relevant CriteriaItems
     */
    public ProjectAcceptanceCriteria findByBranchPathWithRelevantCriteriaItems(String branchPath, boolean matchAuthorFlags) {
        ProjectAcceptanceCriteria criteria = getFromBranchOrParent(branchPath);
        if (criteria == null) {
            return null;
        }

        // Required data for processing
        Branch branch = getBranchOrThrow(branchPath);
        String codeSystem = BranchPathUtil.extractCodeSystem(branchPath);
        Map<String, Object> branchAuthorFlags = MetadataUtil.getAuthorFlags(branch);
        Set<String> authorFlagsEnabled = MetadataUtil.getTrueAuthorFlags(branchAuthorFlags);
        Set<String> authorFlagsAll = branchAuthorFlags.keySet();

        // Get project, task and mandatory CriteriaItems. Also get CriteriaItem if enabledByFlag is enabled.
        Set<CriteriaItem> relevantCriteriaItems = getRelevantCriteriaItems(criteria, authorFlagsEnabled, matchAuthorFlags);

        // Remove item from collection if:
        //- notForCodeSystems field is not blank and contains the identified code system
        // OR
        //- forCodeSystems field is not blank and does not contain the identified code system
        relevantCriteriaItems.removeIf(criteriaItem -> (!CollectionUtils.isEmpty(criteriaItem.getForCodeSystems()) && !criteriaItem.getForCodeSystems().contains(codeSystem))
                                                    || (!CollectionUtils.isEmpty(criteriaItem.getNotForCodeSystems()) && criteriaItem.getNotForCodeSystems().contains(codeSystem)));

        // Remove from collection if Criteria is no longer relevant in comparison to Branch metadata.
        relevantCriteriaItems.removeIf(isConflictBetweenAuthorFlags(branchAuthorFlags, authorFlagsAll, authorFlagsEnabled));

        // Add to collection if task Branch received content via batch
        if (criteria.isBranchTaskLevel(branchPath) && authorFlagsEnabled.contains(Constants.AUTHOR_FLAG_BATCH_CHANGE)) {
            criteria.setBatch(true);
            addRelevantCriteriaItemsForBatchTask(relevantCriteriaItems);
        }

        criteria.setSelectedCriteria(relevantCriteriaItems);
        return criteria;
    }

    /**
     * Find entry in database with the highest projectIteration for the given branchPath.
     *
     * @param branchPath Field to match in query.
     * @return Entry in database with the highest projectIteration for the given branchPath.
     * @throws IllegalArgumentException If argument is invalid.
     */
    public ProjectAcceptanceCriteria getLatestProjectAcceptanceCriteria(String branchPath) {
        verifyParams(branchPath);

        List<ProjectAcceptanceCriteria> projectAcceptanceCriteria = repository.findAllByBranchPathOrderByProjectIterationDesc(branchPath);
        if (projectAcceptanceCriteria == null || projectAcceptanceCriteria.isEmpty()) {
            return null;
        }

        return projectAcceptanceCriteria.get(0);
    }

    /**
     * Find entry in database with the highest projectIteration for the given branchPath. If no entry is found in
     * database, then throw an exception.
     *
     * @param branchPath Field to match in query.
     * @return Entry in database with the highest projectIteration for the given branchPath.
     * @throws IllegalArgumentException If argument is invalid.
     */
    public ProjectAcceptanceCriteria getLatestProjectAcceptanceCriteriaOrThrow(String branchPath) {
        ProjectAcceptanceCriteria latestProjectAcceptanceCriteria = getLatestProjectAcceptanceCriteria(branchPath);
        if (latestProjectAcceptanceCriteria == null) {
            String message = String.format("Branch %s has no Acceptance Criteria.", branchPath);
            throw new ServiceRuntimeException(message, HttpStatus.NOT_FOUND);
        }

        return latestProjectAcceptanceCriteria;
    }

    /**
     * Find highest projectIteration in database for the given branchPath.
     *
     * @param branchPath Field to match in query.
     * @return Highest projectIteration in database for the given branchPath.
     * @throws IllegalArgumentException If argument is invalid.
     */
    public Integer getLatestProjectIteration(String branchPath) {
        ProjectAcceptanceCriteria latestProjectAcceptanceCriteria = getLatestProjectAcceptanceCriteria(branchPath);
        if (latestProjectAcceptanceCriteria == null) {
            return null;
        }

        return latestProjectAcceptanceCriteria.getProjectIteration();
    }

    /**
     * Find highest projectIteration in database for the given branchPath. If no entry is found in database,
     * then throw an exception.
     *
     * @param branchPath Field to match in query.
     * @return Highest projectIteration for the given branchPath.
     * @throws IllegalArgumentException If argument is invalid.
     * @throws ServiceRuntimeException  If no entry found in database matching query.
     */
    public Integer getLatestProjectIterationOrThrow(String branchPath) {
        Integer latestProjectIteration = getLatestProjectIteration(branchPath);
        if (latestProjectIteration == null) {
            throw new ServiceRuntimeException("Cannot find ProjectAcceptanceCriteria.", HttpStatus.NOT_FOUND);
        }

        return latestProjectIteration;
    }

    /**
     * Find entry in database with branchPath and projectIteration fields matching properties in
     * given ProjectAcceptanceCriteria. If there is a matching entry, replace it with given ProjectAcceptanceCriteria.
     *
     * @param projectAcceptanceCriteria Entry to replace the existing one.
     * @return Updated entry from database.
     * @throws IllegalArgumentException If argument is invalid.
     * @throws ServiceRuntimeException  If no entry found in database matching query.
     */
    public ProjectAcceptanceCriteria update(ProjectAcceptanceCriteria projectAcceptanceCriteria) {
        verifyParams(projectAcceptanceCriteria);
        // Must exist
        ProjectAcceptanceCriteria existing = findByBranchPathAndProjectIterationOrThrow(projectAcceptanceCriteria.getBranchPath(), projectAcceptanceCriteria.getProjectIteration());
        projectAcceptanceCriteria.setCreationDate(existing.getCreationDate());
        projectAcceptanceCriteriaCreateValidator.validate(projectAcceptanceCriteria);
        return repository.save(projectAcceptanceCriteria);
    }

    /**
     * Delete entry in database with branchPath and projectIteration fields matching properties in
     * given ProjectAcceptanceCriteria.
     *
     * @param projectAcceptanceCriteria Entry to delete from database.
     */
    public void delete(ProjectAcceptanceCriteria projectAcceptanceCriteria) {
        verifyParams(projectAcceptanceCriteria);
        repository.delete(projectAcceptanceCriteria);
    }

	public Set<CriteriaItem> findItemsAndMarkSignOff(ProjectAcceptanceCriteria criteria, String branchPath) {
		if (criteria == null) {
			return Collections.emptySet();
		}

        Set<String> criteriaIdentifiers = criteria.getAllCriteriaIdentifiers();
        if (criteriaIdentifiers.isEmpty()) {
            return Collections.emptySet();
        }

        Set<CriteriaItem> criteriaItems = criteriaItemService.findAllByIdentifiers(criteriaIdentifiers);
        criteriaItemSignOffService.markSignedOffItems(criteriaItems, branchPath, criteria.getProjectIteration(), criteria);

        if (criteria.isBatch()) {
            setMandatory(criteriaItems);
        }

        return criteriaItems;
	}

    /**
     * Return whether the given ProjectAcceptanceCriteria for the given branch is complete. If the given branch is for a project,
     * then only project level CriteriaItems will be checked. Likewise, if the given branch is for a task, then only
     * task level CriteriaItems will be checked. If the given branchPath is for the project and
     * the ProjectAcceptanceCriteria has been completed, a new entry will be added to the store.
     *
     * @param projectAcceptanceCriteria Entry to check if complete
     * @param branchPath                Branch to cross reference
     * @return Whether the given ProjectAcceptanceCriteria for the given branch is complete.
     * @throws IllegalArgumentException If arguments are invalid.
     */
    public boolean incrementIfComplete(ProjectAcceptanceCriteria projectAcceptanceCriteria, String branchPath) {
        verifyParams(projectAcceptanceCriteria, branchPath);

        Set<CriteriaItem> criteriaItems = findItemsAndMarkSignOff(projectAcceptanceCriteria, branchPath);
        boolean allCriteriaItemsComplete = false;
        boolean branchProjectLevel = projectAcceptanceCriteria.isBranchProjectLevel(branchPath);
        if (branchProjectLevel) {
            allCriteriaItemsComplete = criteriaItems.stream().filter(criteriaItem -> AuthoringLevel.PROJECT == criteriaItem.getAuthoringLevel()).allMatch(CriteriaItem::isComplete);
        } else if (projectAcceptanceCriteria.isBranchTaskLevel(branchPath)) {
            allCriteriaItemsComplete = criteriaItems.stream().filter(criteriaItem -> AuthoringLevel.TASK == criteriaItem.getAuthoringLevel()).allMatch(CriteriaItem::isComplete);
        }

        if (allCriteriaItemsComplete && branchProjectLevel) {
            // New entry to get new creation date.
            ProjectAcceptanceCriteria incrementedProjectAcceptanceCriteria = projectAcceptanceCriteria.cloneWithNextProjectIteration();
            create(incrementedProjectAcceptanceCriteria);
            LOGGER.info("{} has all criteria complete, and will have its projectIteration value incremented.", branchPath);
            LOGGER.debug("New ProjectAcceptanceCriteria for {}: {}", branchPath, incrementedProjectAcceptanceCriteria);
        }

        return allCriteriaItemsComplete;
    }

    private Branch getBranchOrThrow(String branchPath) {
        try {
            return branchSecurityService.getBranchOrThrow(branchPath);
        } catch (RestClientException e) {
            throw new ServiceRuntimeException(String.format("Cannot find branch %s", branchPath), HttpStatus.NOT_FOUND);
        }
    }

    private ProjectAcceptanceCriteria getFromBranchOrParent(String branchPath) {
        ProjectAcceptanceCriteria criteria = getLatestProjectAcceptanceCriteria(branchPath);

        if (criteria == null) {
            String parentPath = PathUtil.getParentPath(branchPath);
            if (parentPath != null) {
                criteria = getLatestProjectAcceptanceCriteria(parentPath);
            }
        }

        return criteria;
    }

    private Set<CriteriaItem> getRelevantCriteriaItems(ProjectAcceptanceCriteria criteria, Set<String> enabledByFlag, boolean matchAuthorFlags) {
        Set<CriteriaItem> relevantCriteriaItems = new HashSet<>();

        // Collect mandatory
        relevantCriteriaItems.addAll(criteriaItemService.findAllByMandatoryAndAuthoringLevel(true, AuthoringLevel.PROJECT));
        relevantCriteriaItems.addAll(criteriaItemService.findAllByMandatoryAndAuthoringLevel(true, AuthoringLevel.TASK));

        // Collect CriteriaItems domain from ProjectAcceptanceCriteria String identifiers
        for (String criteriaId : criteria.getAllCriteriaIdentifiers()) {
            CriteriaItem projectCriteria = criteriaItemService.findByIdOrThrow(criteriaId);
            relevantCriteriaItems.add(projectCriteria);
        }

        if (matchAuthorFlags) {
            // Collect those with enabledByFlag
            relevantCriteriaItems.addAll(criteriaItemService.findAllByEnabledByFlag(enabledByFlag));
        }

        return relevantCriteriaItems;
    }

    // See AcceptanceControllerTest for matrix showcasing possible scenarios.
    private Predicate<CriteriaItem> isConflictBetweenAuthorFlags(Map<String, Object> branchAuthorFlags, Set<String> authorFlagsAll, Set<String> authorFlagsEnabled) {
        return criteriaItem -> {
            Set<String> enabledByFlag = criteriaItem.getEnabledByFlag();
            boolean enabledByFlagEmpty = enabledByFlag.isEmpty();
            boolean branchFlagsEmpty = authorFlagsAll.isEmpty();
            boolean isMandatory = criteriaItem.isMandatory();

            if (enabledByFlagEmpty && branchFlagsEmpty) {
                return false;
            }

            if (!enabledByFlagEmpty && branchFlagsEmpty && isMandatory) {
                return true;
            }

            if (!enabledByFlagEmpty) {
                // If item is mandatory, remove as overwritten from author flags
                if (authorFlagsEnabled.isEmpty()) {
                    return isMandatory;
                }

                // If Branch metadata doesn't contain enabledByFlag, remove
                boolean noneMatch = enabledByFlag.stream().noneMatch(authorFlagsAll::contains);
                if (noneMatch) {
                    return true;
                }

                // If Branch metadata does contain enabledByFlag, remove if flag not enabled
                boolean remove = false;
                for (String flag : enabledByFlag) {
                    Object authorFlag = branchAuthorFlags.get(flag);
                    if (authorFlag != null) {
                        boolean enabled = Boolean.parseBoolean(authorFlag.toString());
                        if (!enabled) {
                            remove = true;
                            break;
                        }
                    }
                }

                return remove;
            }

            return false;
        };
    }

    private void addRelevantCriteriaItemsForBatchTask(Set<CriteriaItem> relevantCriteriaItems) {
        // Check current CriteriaItems to prevent call to store
        boolean readFromStore = true;
        for (CriteriaItem relevantCriteriaItem : relevantCriteriaItems) {
            if (TASK_VALIDATION_CLEAN.equals(relevantCriteriaItem.getId())) {
                readFromStore = false;
                break;
            }
        }

        if (readFromStore) {
            CriteriaItem taskValidationClean = criteriaItemService.findByIdOrThrow(TASK_VALIDATION_CLEAN);
            relevantCriteriaItems.add(taskValidationClean);
        }
    }

    private void setMandatory(Set<CriteriaItem> criteriaItems) {
        for (CriteriaItem criteriaItem : criteriaItems) {
            if (TASK_VALIDATION_CLEAN.equals(criteriaItem.getId())) {
                criteriaItem.setMandatory(true); // Force mandatory regardless of config
                break;
            }
        }
    }
}

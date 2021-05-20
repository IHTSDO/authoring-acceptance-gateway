package org.snomed.aag.data.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.repositories.ProjectAcceptanceCriteriaRepository;
import org.snomed.aag.data.validators.ProjectAcceptanceCriteriaValidator;
import org.snomed.aag.rest.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Service
public class ProjectAcceptanceCriteriaService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectAcceptanceCriteriaService.class);

	@Autowired
	private ProjectAcceptanceCriteriaRepository repository;

	@Autowired
	private ProjectAcceptanceCriteriaValidator projectAcceptanceCriteriaValidator;

	@Autowired
	private CriteriaItemService criteriaItemService;

	/**
	 * Save entry in database.
	 *
	 * @param projectAcceptanceCriteria The entry to save in database.
	 * @throws IllegalArgumentException If the given ProjectAcceptanceCriteria is invalid.
	 * @throws ServiceRuntimeException If there is already a ProjectAcceptanceCriteria entry in database.
	 */
	public void create(ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		projectAcceptanceCriteriaValidator.validate(projectAcceptanceCriteria);
		Optional<ProjectAcceptanceCriteria> existingProjectAcceptanceCriteria = repository.findByBranchPathAndProjectIteration(projectAcceptanceCriteria.getBranchPath(), projectAcceptanceCriteria.getProjectIteration());
		if (existingProjectAcceptanceCriteria.isPresent()) {
			String message = format("Project Acceptance Criteria already exists for branch %s and iteration %d.", projectAcceptanceCriteria.getBranchPath(), projectAcceptanceCriteria.getProjectIteration());
			throw new ServiceRuntimeException(message, HttpStatus.CONFLICT);
		}

		repository.save(projectAcceptanceCriteria);
	}

	/**
	 * Find all entries in database.
	 *
	 * @param pageRequest The pageable configuration for this request.
	 * @return All entries in database, paged.
	 */
	public Page<ProjectAcceptanceCriteria> findAll(PageRequest pageRequest) {
		return repository.findAll(pageRequest);
	}

	/**
	 * Find entry in database matching query.
	 *
	 * @param branchPath Query will look for entries in database with the same branchPath value.
	 * @return Entry in database matching query.
	 */
	public ProjectAcceptanceCriteria findByBranchPath(String branchPath) {
		return repository.findByBranchPath(branchPath);
	}

	/**
	 * Find entry in database matching query.
	 *
	 * @param branchPath       Field used in query.
	 * @param projectIteration Field used in query.
	 * @return Entry in database matching query.
	 */
	public Optional<ProjectAcceptanceCriteria> findBy(String branchPath, Integer projectIteration) {
		if (branchPath == null || projectIteration == null || projectIteration < 0) {
			return Optional.empty();
		}

		return repository.findByBranchPathAndProjectIteration(branchPath, projectIteration);
	}

	/**
	 * Find entry in database matching query, or throw exception if no entry found.
	 *
	 * @param branchPath Query will look for entries in database with the same branchPath value.
	 * @return Entry in database matching query.
	 * @throws NotFoundException If no entry found in database matching query.
	 */
	public ProjectAcceptanceCriteria findByBranchPathOrThrow(String branchPath) {
		final ProjectAcceptanceCriteria projectAcceptanceCriteria = findByBranchPath(branchPath);
		if (projectAcceptanceCriteria == null) {
			throw new NotFoundException(format("No project acceptance criteria found for branch path '%s'.", branchPath));
		}
		return projectAcceptanceCriteria;
	}

	/**
	 * Find ProjectAcceptanceCriteria for the given branch. If the appropriate flag is set and no
	 * ProjectAcceptanceCriteria exists for the given branch, then the given branch's parent will
	 * subsequently be searched.
	 *
	 * @param branch                               Branch to look for ProjectAcceptanceCriteria.
	 * @param includeGloballyRequiredCriteriaItems Flag to indicate whether to include mandatory Project and Task level Criteria Items.
	 * @param checkParent                          Flag to indicate whether to check parent branch for ProjectAcceptanceCriteria.
	 * @return ProjectAcceptanceCriteria for given branch.
	 * @throws NotFoundException If ProjectAcceptanceCriteria cannot be found for branch.
	 */
	public ProjectAcceptanceCriteria findByBranchPathOrThrow(String branch, boolean includeGloballyRequiredCriteriaItems, boolean checkParent) {
		if (!includeGloballyRequiredCriteriaItems) {
			return findByBranchPathOrThrow(branch);
		}

		ProjectAcceptanceCriteria projectAcceptanceCriteria = findByBranchPath(branch);
		if (projectAcceptanceCriteria == null) {
			LOGGER.info("No ProjectAcceptanceCriteria found for '{}'.", branch);
			if (!checkParent) {
				LOGGER.debug("Flag to check parent is false; throwing exception.");
				throw new NotFoundException("No project acceptance criteria found for this branch path.");
			}

			LOGGER.info("Looking for ProjectAcceptanceCriteria for parent of '{}'.", branch);
			String parentPath = PathUtil.getParentPath(branch);
			if (parentPath == null || parentPath.equals(branch)) {
				LOGGER.debug("Branch '{}' does not have parent.", branch);
				throw new NotFoundException("No project acceptance criteria found for this branch path.");
			}

			return findByBranchPathOrThrow(parentPath, includeGloballyRequiredCriteriaItems, false); //Not recursive
		}

		for (CriteriaItem criteriaItem : criteriaItemService.findAllByMandatoryAndAuthoringLevel(true, AuthoringLevel.PROJECT)) {
			projectAcceptanceCriteria.addToSelectedProjectCriteria(criteriaItem);
		}

		for (CriteriaItem criteriaItem : criteriaItemService.findAllByMandatoryAndAuthoringLevel(true, AuthoringLevel.TASK)) {
			projectAcceptanceCriteria.addToSelectedTaskCriteria(criteriaItem);
		}

		return projectAcceptanceCriteria;
	}

	/**
	 * Get the latest project iteration for the given branch path.
	 *
	 * @param branchPath Query will match on branchPath.
	 * @return The latest project iteration for the given branch path.
	 */
	public Optional<Integer> getLatestProjectIteration(String branchPath) {
		if (branchPath == null) {
			throw new IllegalArgumentException();
		}

		List<ProjectAcceptanceCriteria> projectAcceptanceCriteria = repository.findAllByBranchPathOrderByProjectIterationDesc(branchPath);
		if (projectAcceptanceCriteria == null || projectAcceptanceCriteria.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(projectAcceptanceCriteria.get(0).getProjectIteration());
	}

	/**
	 * Update the entry in the database.
	 *
	 * @param projectAcceptanceCriteria The desired state of entry in database.
	 * @return The updated entry from database.
	 * @throws NotFoundException        If no entry found in database.
	 * @throws IllegalArgumentException If the given ProjectAcceptanceCriteria is invalid.
	 */
	public ProjectAcceptanceCriteria update(ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		// Must exist
		findByBranchPathOrThrow(projectAcceptanceCriteria.getBranchPath());
		projectAcceptanceCriteriaValidator.validate(projectAcceptanceCriteria);
		return repository.save(projectAcceptanceCriteria);
	}

	/**
	 * Delete entry from database.
	 *
	 * @param projectAcceptanceCriteria The entry to remove from the database.
	 */
	public void delete(ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		repository.delete(projectAcceptanceCriteria);
	}
}

package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.services.ProjectAcceptanceCriteriaService;
import org.snomed.aag.data.validators.ProjectAcceptanceCriteriaUpdateValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@Tag(name = "Acceptance Criteria Maintenance")
@RequestMapping(value = "/criteria", produces = "application/json")
public class AcceptanceCriteriaController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceCriteriaController.class);

	private final ProjectAcceptanceCriteriaService service;
	private final ProjectAcceptanceCriteriaUpdateValidator updateValidator;

	public AcceptanceCriteriaController(ProjectAcceptanceCriteriaService service, ProjectAcceptanceCriteriaUpdateValidator projectAcceptanceCriteriaUpdateValidator) {
		this.service = service;
		this.updateValidator = projectAcceptanceCriteriaUpdateValidator;
	}

	@GetMapping
	@Operation(summary = "Get page of criteria")
	public Page<ProjectAcceptanceCriteria> findAll(
			@RequestParam(required = false, defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "100") int size) {
		LOGGER.info("Finding all ProjectAcceptanceCriteria with page {} and size {}.", page, size);
		return service.findAll(PageRequest.of(page, size));
	}

	@GetMapping("/{branch}")
	@Operation(summary = "Get criteria by branch")
	public ProjectAcceptanceCriteria findForBranch(@PathVariable String branch, @RequestParam(required = false) Integer projectIteration) {
		branch = BranchPathUriUtil.decodePath(branch);
		LOGGER.info("Finding ProjectAcceptanceCriteria for branch {}.", branch);
		boolean requestingLatestProjectIteration = projectIteration == null;
		if (requestingLatestProjectIteration) {
			LOGGER.debug("Optional parameter omitted; will find ProjectAcceptanceCriteria using latest project iteration.");
			Integer latestProjectIteration = service.getLatestProjectIterationOrThrow(branch);
			return service.findByBranchPathAndProjectIterationOrThrow(branch, latestProjectIteration);
		}

		return service.findByBranchPathAndProjectIterationOrThrow(branch, projectIteration);
	}

	@PostMapping
	@PreAuthorize("hasPermission('PROJECT_LEAD', #criteria.branchPath)")
	@Operation(summary = "Create new criteria")
	public ResponseEntity<Void> createProjectCriteria(@RequestBody @Valid ProjectAcceptanceCriteria criteria) {
		LOGGER.info("Creating ProjectAcceptanceCriteria.");
		LOGGER.debug("Creating {}.", criteria);
		service.create(criteria);
		return ControllerHelper.getCreatedResponse(criteria.getBranchPath());
	}

	@PutMapping("/{branch}")
	@PreAuthorize("hasPermission('PROJECT_LEAD', #branch)")
	@Operation(summary = "Update existing criteria by branch")
	public ProjectAcceptanceCriteria updateProjectCriteria(@PathVariable String branch, @RequestBody @Valid ProjectAcceptanceCriteria criteria,
														   @RequestParam(required = false) Integer projectIteration) {
		branch = BranchPathUriUtil.decodePath(branch);
		LOGGER.info("Updating ProjectAcceptanceCriteria for branch {}.", branch);
		boolean requestingLatestProjectIteration = projectIteration == null;
		if (requestingLatestProjectIteration) {
			LOGGER.debug("Optional parameter omitted; will update ProjectAcceptanceCriteria using latest project iteration.");
			projectIteration = service.getLatestProjectIterationOrThrow(branch);
		}

		boolean projectIterationToUpdateNotSpecified = criteria.getProjectIteration() == null;
		if (projectIterationToUpdateNotSpecified) {
			criteria.setProjectIteration(projectIteration);
		}

		updateValidator.validate(criteria, branch, projectIteration);
		return service.update(criteria);
	}

	@DeleteMapping("/{branch}")
	@PreAuthorize("hasPermission('ADMIN', 'global')")
	@Operation(summary = "Delete existing criteria by branch")
	public void deleteProjectCriteria(@PathVariable String branch, @RequestParam(required = false) Integer projectIteration) {
		branch = BranchPathUriUtil.decodePath(branch);
		LOGGER.info("Deleting ProjectAcceptanceCriteria on branch {}.", branch);
		boolean requestingLatestProjectIteration = projectIteration == null;
		ProjectAcceptanceCriteria projectAcceptanceCriteria;
		if (requestingLatestProjectIteration) {
			LOGGER.debug("Optional parameter omitted; will delete ProjectAcceptanceCriteria using latest project iteration.");
			projectAcceptanceCriteria = service.getLatestProjectAcceptanceCriteriaOrThrow(branch);
		} else {
			projectAcceptanceCriteria = service.findByBranchPathAndProjectIterationOrThrow(branch, projectIteration);
		}

		service.delete(projectAcceptanceCriteria);
	}

}

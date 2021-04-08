package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.Api;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.services.ProjectAcceptanceCriteriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@Api(tags = "Acceptance Criteria Maintenance", description = "-")
@RequestMapping(value = "/criteria", produces = "application/json")
public class AcceptanceCriteriaController {

	private final ProjectAcceptanceCriteriaService service;

	public AcceptanceCriteriaController(ProjectAcceptanceCriteriaService service) {
		this.service = service;
	}

	@GetMapping
	public Page<ProjectAcceptanceCriteria> findAll(
			@RequestParam(required = false, defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "100") int size) {

		return service.findAll(PageRequest.of(page, size));
	}

	@GetMapping("/{branch}")
	public ProjectAcceptanceCriteria findForBranch(@PathVariable String branch) {
		branch = BranchPathUriUtil.decodePath(branch);
		return service.findByBranchPathOrThrow(branch);
	}

	@PostMapping
	@PreAuthorize("hasPermission('PROJECT_MANAGER', #criteria.branchPath)")
	public ResponseEntity<Void> createProjectCriteria(@RequestBody @Valid ProjectAcceptanceCriteria criteria) {
		service.create(criteria);
		return ControllerHelper.getCreatedResponse(criteria.getBranchPath());
	}

	@PutMapping("/{branch}")
	@PreAuthorize("hasPermission('PROJECT_MANAGER', #branch)")
	public ProjectAcceptanceCriteria updateProjectCriteria(@PathVariable String branch, @RequestBody @Valid ProjectAcceptanceCriteria criteria) {
		branch = BranchPathUriUtil.decodePath(branch);

		final String criteriaBranchPath = criteria.getBranchPath();
		if (criteriaBranchPath != null && !criteriaBranchPath.equals(branch)) {
			throw new IllegalArgumentException("Branch in URL does not match branch in criteria.");
		}
		criteria.setBranchPath(branch);

		return service.update(criteria);
	}

	@DeleteMapping("/{branch}")
	@PreAuthorize("hasPermission('ADMIN', 'global')")
	public void deleteProjectCriteria(@PathVariable String branch) {
		branch = BranchPathUriUtil.decodePath(branch);
		final ProjectAcceptanceCriteria criteria = service.findOrThrow(branch);
		service.delete(criteria);
	}

}

package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.Api;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.pojo.CriteriaItemBulkLoadRequest;
import org.snomed.aag.data.services.CriteriaItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RestController
@Api(tags = "Criteria Item Library", description = "-")
@RequestMapping(value = "/criteria-items", produces = "application/json")
public class CriteriaItemLibraryController {

	@Autowired
	private CriteriaItemService service;

	@GetMapping(value = "/{branch}")
	public Page<CriteriaItem> findCriteriaItems(
			@PathVariable String branch,
			@RequestParam(required = false, defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "100") int size) {
		branch = BranchPathUriUtil.decodePath(branch);
		return service.findByBranch(branch, PageRequest.of(page, size));
	}

	@PostMapping
	@PreAuthorize("hasPermission('ADMIN', 'global')")
	public ResponseEntity<Void> createCriteriaItem(@RequestBody @Valid CriteriaItem item) {
		service.create(item);
		return ControllerHelper.getCreatedResponse(item.getId());
	}

	@PostMapping(value = "/bulk-load")
	@PreAuthorize("hasPermission('ADMIN', 'global')")
	public Collection<String> createCriteriaItemsInBulk(@RequestBody @Valid CriteriaItemBulkLoadRequest bulkItems) {
		List<String> idList = new ArrayList<>();

		for (CriteriaItem item : bulkItems.getCriteriaItems()) {
			service.create(item);
			idList.add(item.getId());
		}
		return idList;
	}

	@PutMapping(value = "/{id}")
	@PreAuthorize("hasPermission('ADMIN', 'global')")
	public CriteriaItem updateCriteriaItem(@PathVariable String id, @RequestBody @Valid CriteriaItem item) {
		if (!id.equals(item.getId())) {
			throw new IllegalArgumentException("The id in the request URI does not match the id in the body.");
		}
		return service.update(item);
	}

	@DeleteMapping(value = "/{id}")
	@PreAuthorize("hasPermission('ADMIN', 'global')")
	public void deleteCriteriaItem(@PathVariable String id) {
		CriteriaItem item = service.findByIdOrThrow(id);
		service.delete(item);
	}

}

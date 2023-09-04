package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.services.AcceptanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@Tag(name = "Admin")
@RequestMapping(value = "/admin", produces = "application/json")
public class AdminController {
	private final AcceptanceService acceptanceService;

	public AdminController(AcceptanceService acceptanceService) {
		this.acceptanceService = acceptanceService;
	}

	@PostMapping("/criteria/{branchPath}/accept")
	@PreAuthorize("hasPermission('ADMIN', #branchPath)")
	public ResponseEntity<?> signOffAllCriteriaItems(@PathVariable(name = "branchPath") String branchPath) throws RestClientException {
		branchPath = BranchPathUriUtil.decodePath(branchPath);
		Set<CriteriaItemSignOff> criteriaItemSignOffs = acceptanceService.acceptAllItemsForBranch(branchPath);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(criteriaItemSignOffs);
	}

	@DeleteMapping("/criteria/{branchPath}/accept")
	@PreAuthorize("hasPermission('ADMIN', #branchPath)")
	public ResponseEntity<?> rejectAllCriteriaItems(@PathVariable(name = "branchPath") String branchPath) throws RestClientException {
		branchPath = BranchPathUriUtil.decodePath(branchPath);
		acceptanceService.rejectAllItemsForBranch(branchPath);

		return ResponseEntity
				.status(HttpStatus.NO_CONTENT)
				.build();
	}
}


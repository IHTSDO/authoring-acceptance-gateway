package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.services.AcceptanceService;
import org.snomed.aag.data.services.BranchSecurityService;
import org.snomed.aag.data.services.ProjectAcceptanceCriteriaService;
import org.snomed.aag.data.services.ServiceRuntimeException;
import org.snomed.aag.rest.pojo.ProjectAcceptanceCriteriaDTO;
import org.snomed.aag.rest.util.BranchPathUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Acceptance")
@RequestMapping(value = "/acceptance", produces = "application/json")
public class AcceptanceController {

    private final BranchSecurityService securityService;
    private final ProjectAcceptanceCriteriaService projectAcceptanceCriteriaService;
    private final AcceptanceService acceptanceService;

	public AcceptanceController(BranchSecurityService securityService, ProjectAcceptanceCriteriaService projectAcceptanceCriteriaService, AcceptanceService acceptanceService) {
        this.securityService = securityService;
        this.projectAcceptanceCriteriaService = projectAcceptanceCriteriaService;
        this.acceptanceService = acceptanceService;
    }

    @Operation(summary = "View all Criteria Items for a branch.",
            description = "This request will retrieve all Criteria Items, both complete and incomplete, for a given branch. " +
                    "If the branch does not have any Criteria Items, the branch's parent will be checked."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "&bull; When the branch has no acceptance criteria."),
            @ApiResponse(responseCode = "403", description = "&bull; When the Branch cannot be found from the given branch path. <br/> &bull; When individual Criteria Items do not exist for the branch's expected acceptance criteria."),
            @ApiResponse(responseCode = "200", description = "When the branch (or its parent) has acceptance criteria.")
    })
	@GetMapping("/{branchPath}")
    public ResponseEntity<?> viewCriteriaItems(@Parameter(description = "The branch path.") @PathVariable(name = "branchPath") String branchPath,
											   @RequestParam(defaultValue = "true") boolean matchAuthorFlags) throws RestClientException {
		branchPath = BranchPathUriUtil.decodePath(branchPath);
        String codeSystem = BranchPathUtil.extractCodeSystem(branchPath);

		// Check branch exists
		securityService.getBranchOrThrow(branchPath);

		//Find ProjectAcceptanceCriteria. Optionally include CriteriaItem with matching author flags.
		ProjectAcceptanceCriteria projectAcceptanceCriteria = projectAcceptanceCriteriaService.findByBranchPathWithRelevantCriteriaItems(branchPath, matchAuthorFlags);
		if (projectAcceptanceCriteria == null) {
			throw new ServiceRuntimeException(String.format("Cannot find Acceptance Criteria for %s.", branchPath), HttpStatus.NOT_FOUND);
		}

		// Update complete flag for all Criteria Items on this branch
		Set<CriteriaItem> items = projectAcceptanceCriteriaService.findItemsAndMarkSignOff(projectAcceptanceCriteria, branchPath);

		// Filter criteria items:
        //- notForCodeSystems field must not have the identified code system
        //- forCodeSystems field is blank or contain the identified code system
		items = items.stream().filter(criteriaItem -> (CollectionUtils.isEmpty(criteriaItem.getForCodeSystems()) || criteriaItem.getForCodeSystems().contains(codeSystem))
                            && (CollectionUtils.isEmpty(criteriaItem.getNotForCodeSystems()) || !criteriaItem.getNotForCodeSystems().contains(codeSystem)))
                .collect(Collectors.toSet());

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(new ProjectAcceptanceCriteriaDTO(projectAcceptanceCriteria.getBranchPath(), items));
	}

    @Operation(summary = "Manually accept a Criteria Item.",
            description = "This request will mark a Criteria Item as accepted for a given branch.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "&bull; When the Criteria Item cannot be found from the given identifier"),
            @ApiResponse(responseCode = "403", description = "&bull; When the Criteria Item cannot be accepted manually, or <br /> &bull; When the user does not have the desired role, or <br/> &bull; When the Branch cannot be found from the given branch path."),
            @ApiResponse(responseCode = "200", description = "When the Criteria Item has been accepted successfully.")
    })
    @PostMapping("/{branch}/item/{item-id}/accept")
    public ResponseEntity<?> signOffCriteriaItem(@Parameter(description = "The branch path.") @PathVariable(name = "branch") String branchPath,
                                                 @Parameter(description = "The identifier of the CriteriaItem to accept.") @PathVariable(name = "item-id") String itemId) throws RestClientException {
        branchPath = BranchPathUriUtil.decodePath(branchPath);

        try {
			CriteriaItemSignOff savedCriteriaItemSignOff = acceptanceService.acceptItem(branchPath, itemId);
			return ResponseEntity
					.status(HttpStatus.OK)
					.body(savedCriteriaItemSignOff);

		} catch (Exception e) {
        	e.printStackTrace();
        	throw e;
		}

    }

    @Operation(summary = "Manually reject a Criteria Item.",
            description = "This request will revert the signing off of a Criteria Item for a given branch.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "&bull; When the Criteria Item cannot be found from the given identifier, or <br /> &bull; When there is no Project Acceptance " +
                    "Criteria for the given branch, or <br /> When the Criteria Item has not been previously signed off."),
            @ApiResponse(responseCode = "403", description = "&bull; When the Criteria Item cannot be unaccepted manually, or <br /> &bull; When the user does not have the desired role, or " +
                    "<br/> &bull; When the Branch cannot be found from the given branch path."),
            @ApiResponse(responseCode = "200", description = "When the Criteria Item has been unaccepted successfully.")
    })
    @DeleteMapping("/{branch}/item/{item-id}/accept")
    public ResponseEntity<?> rejectCriteriaItem(@Parameter(description = "The branch path.") @PathVariable(name = "branch") String branchPath,
                                                @Parameter(description = "The identifier of the CriteriaItem to reject.") @PathVariable(name = "item-id") String itemId) {
        branchPath = BranchPathUriUtil.decodePath(branchPath);

		acceptanceService.rejectOrThrow(branchPath, itemId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

}

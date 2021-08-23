package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.*;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.services.AcceptanceService;
import org.snomed.aag.data.services.BranchSecurityService;
import org.snomed.aag.data.services.ProjectAcceptanceCriteriaService;
import org.snomed.aag.data.services.ServiceRuntimeException;
import org.snomed.aag.rest.pojo.ProjectAcceptanceCriteriaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@Api(tags = "Acceptance")
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

    @ApiOperation(value = "View all Criteria Items for a branch.",
            notes = "This request will retrieve all Criteria Items, both complete and incomplete, for a given branch. " +
                    "If the branch does not have any Criteria Items, the branch's parent will be checked."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "&bull; When the branch has no acceptance criteria."),
            @ApiResponse(code = 403, message = "&bull; When the Branch cannot be found from the given branch path. <br/> &bull; When individual Criteria Items do not exist for the branch's expected acceptance criteria."),
            @ApiResponse(code = 200, message = "When the branch (or its parent) has acceptance criteria.", response = ProjectAcceptanceCriteriaDTO.class)
    })
	@GetMapping("/{branchPath}")
	public ResponseEntity<?> viewCriteriaItems(@ApiParam("The branch path.") @PathVariable(name = "branchPath") String branchPath) throws RestClientException {
		branchPath = BranchPathUriUtil.decodePath(branchPath);

		// Check branch exists
		securityService.getBranchOrThrow(branchPath);

		//Find ProjectAcceptanceCriteria (including mandatory items)
		ProjectAcceptanceCriteria projectAcceptanceCriteria = projectAcceptanceCriteriaService.findByBranchPathWithRelevantCriteriaItems(branchPath);
		if (projectAcceptanceCriteria == null) {
			throw new ServiceRuntimeException(String.format("Cannot find Acceptance Criteria for %s.", branchPath), HttpStatus.NOT_FOUND);
		}

		// Update complete flag for all Criteria Items on this branch
		Set<CriteriaItem> items = projectAcceptanceCriteriaService.findItemsAndMarkSignOff(projectAcceptanceCriteria, branchPath);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(new ProjectAcceptanceCriteriaDTO(projectAcceptanceCriteria.getBranchPath(), items));
	}

    @ApiOperation(value = "Manually accept a Criteria Item.",
            notes = "This request will mark a Criteria Item as accepted for a given branch.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "&bull; When the Criteria Item cannot be found from the given identifier"),
            @ApiResponse(code = 403, message = "&bull; When the Criteria Item cannot be accepted manually, or <br /> &bull; When the user does not have the desired role, or <br/> &bull; When the Branch cannot be found from the given branch path."),
            @ApiResponse(code = 200, message = "When the Criteria Item has been accepted successfully.", response = CriteriaItemSignOff.class)
    })
    @PostMapping("/{branch}/item/{item-id}/accept")
    public ResponseEntity<?> signOffCriteriaItem(@ApiParam("The branch path.") @PathVariable(name = "branch") String branchPath,
                                                 @ApiParam("The identifier of the CriteriaItem to accept.") @PathVariable(name = "item-id") String itemId) throws RestClientException {
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

    @ApiOperation(value = "Manually reject a Criteria Item.",
            notes = "This request will revert the signing off of a Criteria Item for a given branch.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "&bull; When the Criteria Item cannot be found from the given identifier, or <br /> &bull; When there is no Project Acceptance " +
                    "Criteria for the given branch, or <br /> When the Criteria Item has not been previously signed off."),
            @ApiResponse(code = 403, message = "&bull; When the Criteria Item cannot be unaccepted manually, or <br /> &bull; When the user does not have the desired role, or " +
                    "<br/> &bull; When the Branch cannot be found from the given branch path."),
            @ApiResponse(code = 200, message = "When the Criteria Item has been unaccepted successfully.", response = CriteriaItemSignOff.class)
    })
    @DeleteMapping("/{branch}/item/{item-id}/accept")
    public ResponseEntity<?> rejectCriteriaItem(@ApiParam("The branch path.") @PathVariable(name = "branch") String branchPath,
                                                @ApiParam("The identifier of the CriteriaItem to reject.") @PathVariable(name = "item-id") String itemId) {
        branchPath = BranchPathUriUtil.decodePath(branchPath);

		acceptanceService.rejectOrThrow(branchPath, itemId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

}

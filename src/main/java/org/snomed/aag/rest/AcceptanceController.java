package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.*;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.services.BranchService;
import org.snomed.aag.data.services.CriteriaItemService;
import org.snomed.aag.data.services.CriteriaItemSignOffService;
import org.snomed.aag.data.services.SecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "Acceptance")
@RequestMapping(value = "/acceptance", produces = "application/json")
public class AcceptanceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceController.class);

    private final CriteriaItemService criteriaItemService;
    private final BranchService branchService;
    private final CriteriaItemSignOffService criteriaItemSignOffService;
    private final SecurityService securityService;

    public AcceptanceController(CriteriaItemService criteriaItemService, BranchService branchService,
                                CriteriaItemSignOffService criteriaItemSignOffService, SecurityService securityService) {
        this.criteriaItemService = criteriaItemService;
        this.branchService = branchService;
        this.criteriaItemSignOffService = criteriaItemSignOffService;
        this.securityService = securityService;
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

        //Verify CriteriaItems
        CriteriaItem criteriaItem = criteriaItemService.findOrThrow(itemId);
        criteriaItemService.verifyManual(criteriaItem, true);

        //Verify branch
        branchService.verifyBranchPermission(branchPath, criteriaItem.getRequiredRole());

        //Verification complete; add record.
        CriteriaItemSignOff savedCriteriaItemSignOff = criteriaItemSignOffService.create(new CriteriaItemSignOff(
                itemId,
                branchPath,
                securityService.getBranchOrThrow(branchPath).getHeadTimestamp(),
                SecurityUtil.getUsername()
        ));
        String savedCriteriaItemSignOffId = savedCriteriaItemSignOff.getId();
        LOGGER.info("Created CriteriaItemSignOff {} for {}.", savedCriteriaItemSignOffId, itemId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(CriteriaItemSignOff.Fields.ID, savedCriteriaItemSignOffId)
                .body(savedCriteriaItemSignOff);
    }
}

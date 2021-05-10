package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.*;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.services.*;
import org.snomed.aag.rest.pojo.ProjectAcceptanceCriteriaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@Api(tags = "Acceptance")
@RequestMapping(value = "/acceptance", produces = "application/json")
public class AcceptanceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceController.class);

    private final CriteriaItemService criteriaItemService;
    private final BranchService branchService;
    private final CriteriaItemSignOffService criteriaItemSignOffService;
    private final SecurityService securityService;
    private final ProjectAcceptanceCriteriaService projectAcceptanceCriteriaService;

    public AcceptanceController(CriteriaItemService criteriaItemService, BranchService branchService,
                                CriteriaItemSignOffService criteriaItemSignOffService, SecurityService securityService,
                                ProjectAcceptanceCriteriaService projectAcceptanceCriteriaService) {
        this.criteriaItemService = criteriaItemService;
        this.branchService = branchService;
        this.criteriaItemSignOffService = criteriaItemSignOffService;
        this.securityService = securityService;
        this.projectAcceptanceCriteriaService = projectAcceptanceCriteriaService;
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

    @ApiOperation(value = "View all Criteria Items for a branch.",
            notes = "This request will retrieve all Criteria Items, both complete and incomplete, for a given branch. " +
                    "If the branch does not have any Criteria Items, the branch's parent will be checked."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "&bull; When the branch has no acceptance criteria."),
            @ApiResponse(code = 403, message = "&bull; When the Branch cannot be found from the given branch path. <br/> &bull; When individual Criteria Items do not exist for the branch's expected acceptance criteria."),
            @ApiResponse(code = 200, message = "When the branch (or its parent) has acceptance criteria.", response = ProjectAcceptanceCriteriaDTO.class)
    })
    @GetMapping("/{branch}")
    public ResponseEntity<?> viewCriteriaItems(@ApiParam("The branch path.") @PathVariable(name = "branch") String branch) throws RestClientException {
        final String branchPath = BranchPathUriUtil.decodePath(branch);
        LOGGER.info("Finding all Criteria Items for {}.", branchPath);

        LOGGER.debug("Verifying branch {} exists.", branchPath);
        securityService.getBranchOrThrow(branchPath);
        LOGGER.debug("Branch {} exists.", branchPath);

        Set<String> allCriteriaIdentifiers = projectAcceptanceCriteriaService.findByBranchPathOrThrow(branchPath, true, true).getAllCriteriaIdentifiers();
        LOGGER.debug("Found {} Criteria Items for {}.", allCriteriaIdentifiers.size(), branchPath);
        Set<CriteriaItem> criteriaItems = criteriaItemService.findAllByIdentifiers(allCriteriaIdentifiers);
        criteriaItemSignOffService.findAllByBranchAndIdentifier(branchPath, criteriaItems);

        ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = new ProjectAcceptanceCriteriaDTO(branchPath, criteriaItems);
        LOGGER.info(
                "Branch {} has {} Criteria Items remaining and {} Criteria Items completed.",
                branchPath,
                projectAcceptanceCriteriaDTO.getNumberOfCriteriaItemsWithCompletedValue(false),
                projectAcceptanceCriteriaDTO.getNumberOfCriteriaItemsWithCompletedValue(true)
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(projectAcceptanceCriteriaDTO);
    }
}

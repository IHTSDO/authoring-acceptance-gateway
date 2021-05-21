package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.*;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.services.*;
import org.snomed.aag.rest.pojo.ProjectAcceptanceCriteriaDTO;
import org.snomed.aag.rest.util.PathUtil;
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
        CriteriaItem criteriaItem = criteriaItemService.findByIdOrThrow(itemId);
        criteriaItemService.verifyManual(criteriaItem, true);

        //Verify branch
        branchService.verifyBranchPermission(branchPath, criteriaItem.getRequiredRole());

        //Verify project acceptance criteria
        Integer latestProjectIteration = projectAcceptanceCriteriaService.getLatestProjectIteration(branchPath);
        if (latestProjectIteration == null) {
            String message = String.format("Branch %s does not have any acceptance criteria.", branchPath);
            throw new ServiceRuntimeException(message, HttpStatus.NOT_FOUND);
        }

        //Verification complete; add record.
        CriteriaItemSignOff savedCriteriaItemSignOff = criteriaItemSignOffService.create(new CriteriaItemSignOff(
                itemId,
                branchPath,
                latestProjectIteration,
                securityService.getBranchOrThrow(branchPath).getHeadTimestamp(),
                SecurityUtil.getUsername()
        ));
        String savedCriteriaItemSignOffId = savedCriteriaItemSignOff.getId();
        LOGGER.info("Created CriteriaItemSignOff {} for {}.", savedCriteriaItemSignOffId, itemId);

        return ResponseEntity
                .status(HttpStatus.OK)
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
    public ResponseEntity<?> viewCriteriaItems(@ApiParam("The branch path.") @PathVariable(name = "branch") String branch,
                                               @RequestParam(required = false, defaultValue = "-1") Integer projectIteration) throws RestClientException {
        String branchPath = BranchPathUriUtil.decodePath(branch);
        securityService.getBranchOrThrow(branchPath);

        boolean checkParent = true;
        boolean requestingLatestProjectIteration = -1 == projectIteration;
        if (requestingLatestProjectIteration) {
            projectIteration = projectAcceptanceCriteriaService.getLatestProjectIteration(branchPath);

            //If branch has no project iteration, check parent branch.
            if (projectIteration == null) {
                branchPath = PathUtil.getParentPath(branchPath);
                projectIteration = projectAcceptanceCriteriaService.getLatestProjectIterationOrThrow(branchPath); //If parent branch has no project iteration, throw exception.
                checkParent = false; //Already checked parent branch, don't need to do it again.
            }
        }

        ProjectAcceptanceCriteria projectAcceptanceCriteria = projectAcceptanceCriteriaService.findByBranchPathAndProjectIterationAndMandatoryOrThrow(branchPath, projectIteration, checkParent);
        Set<CriteriaItem> criteriaItems = criteriaItemService.findAllByIdentifiers(projectAcceptanceCriteria.getAllCriteriaIdentifiers());
        criteriaItemSignOffService.findByBranchPathAndProjectIterationAndCriteriaItemId(branchPath, projectIteration, criteriaItems);

        ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = new ProjectAcceptanceCriteriaDTO(branchPath, criteriaItems);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(projectAcceptanceCriteriaDTO);
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

        //Verify CriteriaItems
        CriteriaItem criteriaItem = criteriaItemService.findByIdOrThrow(itemId);
        criteriaItemService.verifyManual(criteriaItem, true);

        //Verify branch
        branchService.verifyBranchPermission(branchPath, criteriaItem.getRequiredRole());

        //Verify project acceptance criteria
        Integer latestProjectIteration = projectAcceptanceCriteriaService.getLatestProjectIteration(branchPath);
        if (latestProjectIteration == null) {
            String message = String.format("Branch %s does not have any acceptance criteria.", branchPath);
            throw new ServiceRuntimeException(message, HttpStatus.NOT_FOUND);
        }

        //Verification complete; remove record
        boolean deleted = criteriaItemSignOffService.deleteByCriteriaItemIdAndBranchPathAndProjectIteration(itemId, branchPath, latestProjectIteration);
        if (!deleted) {
            String message = String.format("Cannot delete %s for branch %s and project iteration %d", itemId, branchPath, latestProjectIteration);
            throw new ServiceRuntimeException(message, HttpStatus.NOT_FOUND);
        }

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}

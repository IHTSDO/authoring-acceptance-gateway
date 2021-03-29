package org.snomed.aag.rest;

import io.swagger.annotations.Api;
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

    @PostMapping("/{branch}/item/{item-id}/accept")
    public ResponseEntity<?> signOffCriteriaItem(@PathVariable(name = "branch") String branchPath, @PathVariable(name = "item-id") String itemId) throws RestClientException {
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

package org.snomed.aag.rest;

import io.swagger.annotations.Api;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.services.CriteriaItemService;
import org.snomed.aag.data.services.CriteriaItemSignOffService;
import org.snomed.aag.data.services.NotFoundException;
import org.snomed.aag.data.services.SecurityService;
import org.snomed.aag.rest.pojo.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final SecurityService securityService;
    private final CriteriaItemSignOffService criteriaItemSignOffService;

    public AcceptanceController(CriteriaItemService criteriaItemService, SecurityService securityService,
                                CriteriaItemSignOffService criteriaItemSignOffService) {
        this.criteriaItemService = criteriaItemService;
        this.securityService = securityService;
        this.criteriaItemSignOffService = criteriaItemSignOffService;
    }

    @PostMapping("/{branch}/item/{item-id}/accept")
    public ResponseEntity<?> signOffCriteriaItem(@PathVariable(name = "branch") String branchPath, @PathVariable(name = "item-id") String itemId) throws RestClientException {
        //Verify CriteriaItems
        CriteriaItem criteriaItem;
        try {
            criteriaItem = criteriaItemService.findOrThrow(itemId);
        } catch (NotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(HttpStatus.NOT_FOUND.toString(), e.getMessage()));
        }

        if (criteriaItem.isNotManual()) {
            LOGGER.error("User attempted to sign off non-manual CriteriaItem ({}).", itemId);
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorMessage(HttpStatus.FORBIDDEN.toString(), "Criteria Item cannot be changed manually."));
        }

        //Verify Branch
        String requiredRole = criteriaItem.getRequiredRole();
        boolean currentUserHasRoleOnBranch;
        try {
            currentUserHasRoleOnBranch = securityService.currentUserHasRoleOnBranch(requiredRole, branchPath);
        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(HttpStatus.NOT_FOUND.toString(), e.getMessage()));
        }

        if (!currentUserHasRoleOnBranch) {
            LOGGER.error("User does not have desired role of {}.", requiredRole);
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorMessage(HttpStatus.FORBIDDEN.toString(), "User does not have desired role."));
        }

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

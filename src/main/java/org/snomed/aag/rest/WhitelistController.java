package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.services.BranchSecurityService;
import org.snomed.aag.data.services.ServiceRuntimeException;
import org.snomed.aag.data.services.WhitelistService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Set;

@RestController
@Tag(name = "Whitelist")
@RequestMapping(value = "/whitelist-items", produces = "application/json")
public class WhitelistController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistController.class);

    private final WhitelistService whitelistService;
    private final BranchSecurityService securityService;

    public WhitelistController(WhitelistService whitelistService, BranchSecurityService securityService) {
        this.whitelistService = whitelistService;
        this.securityService = securityService;
    }

    @GetMapping
    @Operation(summary = "Get page of whitelist items")
    public Page<WhitelistItem> findWhitelistItems(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int size) {
        return whitelistService.findAll(PageRequest.of(page, size));
    }

    @GetMapping(value = "/item/{id}")
    @Operation(summary = "Find whitelist item by id")
    public ResponseEntity<WhitelistItem> retrieveWhitelistItem(@PathVariable String id) {
        WhitelistItem item = whitelistService.findOrThrow(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(item);
    }

    @Operation(summary = "Find whitelist items by validation rule ID")
    @GetMapping(value = "/validation-rules/{validationRuleId}")
    public List<WhitelistItem> findWhitelistItemsByValidationRuleId(@PathVariable String validationRuleId) {
        return whitelistService.findAllByValidationRuleId(validationRuleId);
    }

    @Operation(summary = "Find whitelist items by list of validation rule ID")
    @PostMapping(value = "/validation-rules")
    public List<WhitelistItem> findWhitelistItemsByValidationRuleIds(@RequestBody Set<String> validationRuleIds) {
        return whitelistService.findAllByValidationRuleIds(validationRuleIds);
    }

    @Operation(summary = "Validate components against whitelist",
            description = "This will be checking components if they are still whitelisted")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Return a list of valid components.")
    })
    @PostMapping(value = "/bulk-validate")
    public List<WhitelistItem> bulkValidate(@RequestBody Set<WhitelistItem> whitelistItems) {
        return whitelistService.validateWhitelistComponents(whitelistItems);
    }

    @PostMapping
    @Operation(summary = "Create a new whitelist item")
    public ResponseEntity<WhitelistItem> addWhitelistItemOld(@RequestBody WhitelistItem whitelistItem) {
        try {
            validateSingleWhiteListItem(whitelistItem);

            WhitelistItem savedWhitelistItem = whitelistService.create(whitelistItem);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedWhitelistItem);
        } catch (ServiceRuntimeException ex) {
            LOGGER.error("Unable to add whitelist item {}", whitelistItem);
            LOGGER.error(ex.getMessage(), ex);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    /**
     * Validates a single WhitelistItem.
     *
     * @param whitelistItem The WhitelistItem to validate.
     * @return true if the WhitelistItem is valid.
     * @throws ServiceRuntimeException if the WhitelistItem is invalid.
     */
    private boolean validateSingleWhiteListItem(WhitelistItem whitelistItem) {
        String error = "";

        if (StringUtils.isEmpty(whitelistItem.getComponentId())) {
            error += "Invalid component ID: '" + whitelistItem.getComponentId() + "'.\n";
        }

        if (StringUtils.isEmpty(whitelistItem.getConceptId()) || !whitelistItem.getConceptId().matches("^\\d+$")) {
            error += "Invalid concept ID: '" + whitelistItem.getConceptId() + "'.\n";
        }

        if (StringUtils.isEmpty(whitelistItem.getBranch())) {
            error += "Branch is mandatory.\n";
        }

        if (!error.isEmpty()) {
            throw new ServiceRuntimeException(error);
        }

        return true;
    }

    @PutMapping(value = "/item/{id}")
    @Operation(summary = "Update an existing whitelist item by id")
    public ResponseEntity<WhitelistItem> updateWhitelistItem(@PathVariable String id, @RequestBody WhitelistItem whitelistItem) {
        WhitelistItem persistedWhitelistItem = whitelistService.findOrThrow(id);
        persistedWhitelistItem.setAdditionalFields(whitelistItem.getAdditionalFields());
        persistedWhitelistItem.setBranch(whitelistItem.getBranch());
        persistedWhitelistItem.setComponentId(whitelistItem.getComponentId());
        persistedWhitelistItem.setConceptId(whitelistItem.getConceptId());
        persistedWhitelistItem.setValidationRuleId(whitelistItem.getValidationRuleId());
        persistedWhitelistItem.setReason(whitelistItem.getReason());
        persistedWhitelistItem.setTemporary(whitelistItem.isTemporary());

        WhitelistItem savedWhitelistItem = whitelistService.update(persistedWhitelistItem);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(savedWhitelistItem);
    }

    @DeleteMapping(value = "/item/{id}")
    @Operation(summary = "Delete an existing whitelist item by id")
    public void deleteWhitelistItem(@PathVariable String id) {
        WhitelistItem item = whitelistService.findOrThrow(id);
        whitelistService.delete(item);
    }

    @GetMapping("/{branch}")
    @Operation(summary = "Find whitelist items by branch path")
    public ResponseEntity<?> findForBranch(@PathVariable String branch, @RequestParam(required = false) Long creationDate,
                                           @RequestParam(required = false, defaultValue = "true") boolean includeDescendants,
                                           @RequestParam(required = false, defaultValue = "ALL") WhitelistItem.WhitelistItemType type,
                                           @RequestParam(required = false, defaultValue = "0") int page,
                                           @RequestParam(required = false, defaultValue = "100") int size) throws RestClientException {
        branch = BranchPathUriUtil.decodePath(branch);
        securityService.getBranchOrThrow(branch);

        Date date = null;
        if (creationDate != null) {
            date = new Date(creationDate);
        }

        List<WhitelistItem> whitelistItems = whitelistService.findAllByBranchAndMinimumCreationDate(branch, date, type, includeDescendants, PageRequest.of(page, size));
        HttpStatus httpStatus = HttpStatus.OK;
        if (whitelistItems.isEmpty()) {
            httpStatus = HttpStatus.NO_CONTENT;
        }

        return ResponseEntity.status(httpStatus).body(whitelistItems);
    }
}

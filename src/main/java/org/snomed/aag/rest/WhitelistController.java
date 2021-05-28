package org.snomed.aag.rest;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.services.SecurityService;
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
@Api(tags = "Whitelist")
@RequestMapping(value = "/whitelist-items", produces = "application/json")
public class WhitelistController {

    private final WhitelistService whitelistService;
    private final SecurityService securityService;

    public WhitelistController(WhitelistService whitelistService, SecurityService securityService) {
        this.whitelistService = whitelistService;
        this.securityService = securityService;
    }

    @GetMapping
    public Page<WhitelistItem> findWhitelistItems(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int size) {
        return whitelistService.findAll(PageRequest.of(page, size));
    }

    @ApiOperation(value = "Find whitelist items by validation rule ID")
    @GetMapping(value = "/validation-rules/{validationRuleId}")
    public List<WhitelistItem> findWhitelistItemsByValidationRuleId(@PathVariable String validationRuleId) {
        return whitelistService.findAllByValidationRuleId(validationRuleId);
    }

    @ApiOperation(value = "Find whitelist items by list of validation rule ID")
    @PostMapping(value = "/validation-rules")
    public List<WhitelistItem> findWhitelistItemsByValidationRuleIds(@RequestBody Set<String> validationRuleIds) {
        return whitelistService.findAllByValidationRuleIds(validationRuleIds);
    }

    @ApiOperation(value = "Validate components against whitelist",
            notes = "This will be checking components if they are still whitelisted")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Return a list of valid components.")
    })
    @PostMapping(value = "/bulk-validate")
    public List<WhitelistItem> bulkValidate(@RequestBody Set<WhitelistItem> whitelistItems) {
        return whitelistService.validateWhitelistComponents(whitelistItems);
    }

    @PostMapping
    public ResponseEntity<WhitelistItem> addWhitelistItem(@RequestBody WhitelistItem whitelistItem) {
        WhitelistItem savedWhitelistItem = whitelistService.create(whitelistItem);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedWhitelistItem);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<WhitelistItem> updateWhitelistItem(@PathVariable String id, @RequestBody WhitelistItem whitelistItem) {
        WhitelistItem persistedWhitelistItem = whitelistService.findOrThrow(id);
        persistedWhitelistItem.setAdditionalFields(whitelistItem.getAdditionalFields());
        persistedWhitelistItem.setBranch(whitelistItem.getBranch());
        persistedWhitelistItem.setComponentId(whitelistItem.getComponentId());
        persistedWhitelistItem.setConceptId(whitelistItem.getConceptId());
        persistedWhitelistItem.setValidationRuleId(whitelistItem.getValidationRuleId());

        WhitelistItem savedWhitelistItem = whitelistService.update(persistedWhitelistItem);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(savedWhitelistItem);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteWhitelistItem(@PathVariable String id) {
        WhitelistItem item = whitelistService.findOrThrow(id);
        whitelistService.delete(item);
    }

    @GetMapping("/{branch}")
    public ResponseEntity<?> findForBranch(@PathVariable String branch, @RequestParam Long creationDate,
                                           @RequestParam(required = false, defaultValue = "true") boolean includeDescendants) throws RestClientException {
        branch = BranchPathUriUtil.decodePath(branch);
        securityService.getBranchOrThrow(branch);

        List<WhitelistItem> whitelistItems = whitelistService.findAllByBranchAndCreationDateGreaterThanEquals(branch, new Date(creationDate), includeDescendants);
        if (whitelistItems.isEmpty()) {
            String message = String.format("No WhitelistItems found for branch %s and created after %d", branch, creationDate);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(message);
        }

        return ResponseEntity.status(HttpStatus.OK).body(whitelistItems);
    }
}

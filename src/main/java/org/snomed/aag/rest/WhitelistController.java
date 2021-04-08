package org.snomed.aag.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.services.WhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@RestController
@Api(tags = "Whitelist")
@RequestMapping(value = "/whitelist-items", produces = "application/json")
public class WhitelistController {

    @Autowired
    private WhitelistService whitelistService;

    @GetMapping
    public Page<WhitelistItem> findWhitelistItems(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int size) {
        return whitelistService.findAll(PageRequest.of(page, size));
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
    public ResponseEntity<WhitelistItem> addWhitelistItem(@RequestBody @Valid WhitelistItem whitelistItem) {
        WhitelistItem savedWhitelistItem = whitelistService.create(whitelistItem);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedWhitelistItem);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<WhitelistItem> updateWhitelistItem(@PathVariable String id, @RequestBody @Valid WhitelistItem whitelistItem) {
        whitelistService.findOrThrow(id);
        whitelistItem.setId(id);
        WhitelistItem savedWhitelistItem = whitelistService.update(whitelistItem);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(savedWhitelistItem);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteWhitelistItem(@PathVariable String id) {
        WhitelistItem item = whitelistService.findOrThrow(id);
        whitelistService.delete(item);
    }
}

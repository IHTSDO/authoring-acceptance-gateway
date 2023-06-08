package org.snomed.aag.data.services;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.repositories.WhitelistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhitelistServiceTest extends AbstractTest {
    @Autowired
    private  WhitelistService whitelistService;

    @Autowired
    private WhitelistItemRepository whitelistItemRepository;

    @Test
    void testFindAllByValidationRuleId() {
        // Set up whitelist items over 10k
        createWhitelistItemsForTest("4ee9cfeb-3ce5-48bf-b238-de7498fde042", 10_001);
        List<WhitelistItem> results = whitelistService.findAllByValidationRuleId("4ee9cfeb-3ce5-48bf-b238-de7498fde042");
        assertEquals(10_001, results.size());

    }

    @Test
    void testFindAllByValidationRuleIdAndExpirationDate() throws ParseException {
        String validationRuleId = "d99dbbf0-4ec9-4e1d-b4c8-5e17160abd27";
        Set<WhitelistItem> itemsToCheck = new HashSet<>();
        WhitelistItem firstItem = new WhitelistItem();
        firstItem.setComponentId("4170222010");
        firstItem.setConceptId("81835008");
        firstItem.setValidationRuleId(validationRuleId);
        firstItem.setAdditionalFields("Test");

        itemsToCheck.add(firstItem);

        WhitelistItem secondItem = new WhitelistItem();
        secondItem.setComponentId("4170223010");
        secondItem.setConceptId("81845008");
        secondItem.setValidationRuleId(validationRuleId);
        secondItem.setAdditionalFields("Test");

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);

        secondItem.setExpirationDate(yesterday.getTime());

        itemsToCheck.add(secondItem);
        whitelistItemRepository.saveAll(itemsToCheck);

        List<WhitelistItem> results = whitelistService.findAllByValidationRuleIds(Collections.singleton(validationRuleId), true);
        assertEquals(1, results.size());

        results = whitelistService.findAllByValidationRuleIds(Collections.singleton(validationRuleId), false);
        assertEquals(2, results.size());

    }

    @Test
    void testValidateWhenItemsAreOver10K() {
        // Set up whitelist items over 10k for two rules
        createWhitelistItemsForTest("4ee9cfeb-3ce5-48bf-b238-de7498fde042", 5_001);
        createWhitelistItemsForTest("4ee9cfeb-3ce5-48bf-b238-de7498fde041", 5_000);
        Set<WhitelistItem> itemsToCheck = new HashSet<>();
        // First item
        WhitelistItem first = new WhitelistItem();
        first.setValidationRuleId("4ee9cfeb-3ce5-48bf-b238-de7498fde042");
        first.setAdditionalFields("1,900000000000207008,895684003,en,900000000000013009,Air 950,000 ppm and carbon dioxide 50,000 ppm gas for inhalation,900000000000020002");
        first.setComponentId("4170222010");
        first.setConceptId("81835007");
        itemsToCheck.add(first);

        // Second item
        WhitelistItem second = new WhitelistItem();
        second.setValidationRuleId("4ee9cfeb-3ce5-48bf-b238-de7498fde041");
        second.setAdditionalFields("1,900000000000207008,895684003,en,900000000000013009,Air 950,000 ppm,900000000000020002");
        second.setComponentId("4170222011");
        second.setConceptId("81835008");
        itemsToCheck.add(second);

        List<WhitelistItem> results = whitelistService.validateWhitelistComponents(itemsToCheck);
        assertTrue(results.isEmpty());
    }

    private void createWhitelistItemsForTest(String validationRuleId, int maxItems) {
        List<WhitelistItem> items = new ArrayList<>();
        for (int i = 0; i < maxItems; i++) {
            WhitelistItem item = new WhitelistItem();
            item.setComponentId("4170222010");
            item.setConceptId("81835008");
            item.setValidationRuleId(validationRuleId);
            item.setAdditionalFields("Test " + i);
            items.add(item);
        }
        Iterables.partition(items, 1_000).forEach(batch -> whitelistItemRepository.saveAll(batch));
    }
}
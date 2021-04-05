package org.snomed.aag.rest.pojo;

import org.junit.jupiter.api.Test;
import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;

import static org.junit.jupiter.api.Assertions.*;

class CriteriaItemToCriteriaItemDTOConverterTest {
    private final CriteriaItemToCriteriaItemDTOConverter target = new CriteriaItemToCriteriaItemDTOConverter();

    @Test
    public void convert_ShouldThrowException_WhenGivenNull() {
        //then
        assertThrows(IllegalArgumentException.class, () -> {
            //when
            target.convert(null);
        });
    }

    @Test
    public void convert_ShouldReturnObjectWithExpectedProperties() {
        //given
        CriteriaItem criteriaItem = new CriteriaItem("ABC");
        criteriaItem.setLabel("test_label");
        criteriaItem.setDescription("test_description");
        criteriaItem.setOrder(5);
        criteriaItem.setAuthoringLevel(AuthoringLevel.CODE_SYSTEM);
        criteriaItem.setMandatory(true);
        criteriaItem.setManual(true);
        criteriaItem.setExpiresOnCommit(true);
        criteriaItem.setRequiredRole("test_required_role");

        //when
        CriteriaItemDTO result = target.convert(criteriaItem);

        //then
        assertEquals("ABC", result.getCriteriaItemId());
        assertEquals("test_label", result.getLabel());
        assertEquals("test_description", result.getDescription());
        assertEquals(5, result.getOrder());
        assertEquals(AuthoringLevel.CODE_SYSTEM, result.getAuthoringLevel());
        assertTrue(result.isMandatory());
        assertTrue(result.isManual());
        assertTrue(result.isExpiresOnCommit());
        assertEquals("test_required_role", result.getRequiredRole());
        assertFalse(result.isCompleted());
    }
}

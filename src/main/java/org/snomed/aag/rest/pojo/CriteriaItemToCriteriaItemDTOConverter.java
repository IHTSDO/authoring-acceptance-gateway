package org.snomed.aag.rest.pojo;

import org.snomed.aag.data.domain.CriteriaItem;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CriteriaItemToCriteriaItemDTOConverter implements Converter<CriteriaItem, CriteriaItemDTO> {
    @Override
    public CriteriaItemDTO convert(CriteriaItem criteriaItem) {
        if (criteriaItem == null) {
            throw new IllegalArgumentException("Cannot convert to CriteriaItemDTO.");
        }

        return new CriteriaItemDTO
                .Builder(criteriaItem.getId())
                .withLabel(criteriaItem.getLabel())
                .withDescription(criteriaItem.getDescription())
                .withOrder(criteriaItem.getOrder())
                .withAuthoringLevel(criteriaItem.getAuthoringLevel())
                .isMandatory(criteriaItem.isMandatory())
                .isManual(criteriaItem.isManual())
                .expiresOnCommit(criteriaItem.isExpiresOnCommit())
                .withRequiredRole(criteriaItem.getRequiredRole())
                .build();
    }
}

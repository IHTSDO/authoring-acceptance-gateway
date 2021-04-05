package org.snomed.aag.rest.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.snomed.aag.data.domain.AuthoringLevel;

import java.util.Objects;

public class CriteriaItemDTO implements Comparable<CriteriaItemDTO> {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String criteriaItemId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String label;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private int order;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AuthoringLevel authoringLevel;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private boolean mandatory;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private boolean manual;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private boolean expiresOnCommit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String requiredRole;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private boolean completed;

    public String getCriteriaItemId() {
        return criteriaItemId;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }

    public AuthoringLevel getAuthoringLevel() {
        return authoringLevel;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public boolean isManual() {
        return manual;
    }

    public boolean isExpiresOnCommit() {
        return expiresOnCommit;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public int compareTo(CriteriaItemDTO that) {
        if (this.getCriteriaItemId().equals(that.getCriteriaItemId())) {
            return 0;
        }

        int o1Order = this.getOrder();
        int o2Order = that.getOrder();

        if (o1Order > o2Order) {
            return 1;
        }

        if (o1Order < o2Order) {
            return -1;
        }

        String thisLabel = this.getLabel();
        String thatLabel = that.getLabel();
        if (Objects.nonNull(thisLabel) && Objects.nonNull(thatLabel)) {
            return thisLabel.compareTo(thatLabel);
        }

        return this.getCriteriaItemId().compareTo(that.getCriteriaItemId());
    }

    public static final class Builder {
        private final String criteriaItemId;
        private String label;
        private String description;
        private int order;
        private AuthoringLevel authoringLevel;
        private boolean mandatory;
        private boolean manual;
        private boolean expiresOnCommit;
        private String requiredRole;

        public Builder(String criteriaItemId) {
            this.criteriaItemId = criteriaItemId;
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withOrder(int order) {
            this.order = order;
            return this;
        }

        public Builder withAuthoringLevel(AuthoringLevel authoringLevel) {
            this.authoringLevel = authoringLevel;
            return this;
        }

        public Builder isMandatory(boolean mandatory) {
            this.mandatory = mandatory;
            return this;
        }

        public Builder isManual(boolean manual) {
            this.manual = manual;
            return this;
        }

        public Builder expiresOnCommit(boolean expiresOnCommit) {
            this.expiresOnCommit = expiresOnCommit;
            return this;
        }

        public Builder withRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
            return this;
        }

        public CriteriaItemDTO build() {
            CriteriaItemDTO criteriaItemDTO = new CriteriaItemDTO();
            criteriaItemDTO.criteriaItemId = this.criteriaItemId;
            criteriaItemDTO.label = this.label;
            criteriaItemDTO.description = this.description;
            criteriaItemDTO.order = this.order;
            criteriaItemDTO.authoringLevel = this.authoringLevel;
            criteriaItemDTO.mandatory = this.mandatory;
            criteriaItemDTO.manual = this.manual;
            criteriaItemDTO.expiresOnCommit = this.expiresOnCommit;
            criteriaItemDTO.requiredRole = this.requiredRole;

            return criteriaItemDTO;
        }
    }
}

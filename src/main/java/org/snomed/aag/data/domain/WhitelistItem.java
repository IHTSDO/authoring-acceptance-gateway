package org.snomed.aag.data.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Document(indexName = "whitelist-item")
public class WhitelistItem {
    public interface Fields {
        String ID = "id";
        String USER_ID = "userId";
        String CREATION_DATE = "creationDate";
        String VALIDATION_RULE_ID = "validationRuleId";
        String COMPONENT_ID = "componentId";
        String CONCEPT_ID = "conceptId";
        String BRANCH = "branch";
        String ADDITIONAL_FIELDS = "additionalFields";
    }

    @Id
    @Field(type = FieldType.Keyword)
    @NotBlank
    private String id;

    @Field(type = FieldType.Keyword )
    @NotBlank
    private String userId;

    @Field(type = FieldType.Long)
    private Date creationDate;

    @Field(type = FieldType.Keyword)
    @NotBlank
    private String validationRuleId;

    @Field(type = FieldType.Keyword)
    @NotBlank
    private String componentId;

    @Field(type = FieldType.Keyword)
    @NotBlank
    private String conceptId;

    @Field(type = FieldType.Keyword)
    @NotBlank
    private String branch;

    @Field(type = FieldType.Keyword)
    private String additionalFields;

    @Transient
    private Long creationDateLong;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;

        if (creationDate != null) {
            this.creationDateLong = creationDate.getTime();
        }
    }

    public Long getCreationDateLong() {
        return creationDateLong;
    }

    public void setCreationDateLong(Long creationDateLong) {
        this.creationDateLong = creationDateLong;
    }

    public String getValidationRuleId() {
        return validationRuleId;
    }

    public void setValidationRuleId(String validationRuleId) {
        this.validationRuleId = validationRuleId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(String additionalFields) {
        this.additionalFields = additionalFields;
    }
}

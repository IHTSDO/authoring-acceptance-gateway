package org.snomed.aag.data.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.Map;

@Document(indexName = "whitelist-item")
public class WhitelistItem {
    @Id
    @Field(type = FieldType.Keyword)
    @NotBlank
    private String id;

    @Field(type = FieldType.Text)
    @NotBlank
    private String userId;

    @Field(type = FieldType.Long)
    private Date creationDate;

    @Field(type = FieldType.Text)
    @NotBlank
    private String validationRuleId;

    @Field(type = FieldType.Text)
    @NotBlank
    private String componentId;

    @Field(type = FieldType.Keyword)
    @NotBlank
    private String conceptId;

    @Field(type = FieldType.Text)
    @NotBlank
    private String branch;

    @Field(type = FieldType.Object)
    private Map <String, String> additionalFields;

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

    public Map <String, String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map <String, String> additionalFields) {
        this.additionalFields = additionalFields;
    }
}

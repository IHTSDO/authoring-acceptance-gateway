package org.snomed.aag.data.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.Objects;

@Document(indexName = "#{@indexNameProvider.getIndexNameWithPrefix('whitelist-item')}")
@Setting(settingPath = "elasticsearch-settings.json")
public class WhitelistItem {
    public interface Fields {
        String CREATION_DATE = "creationDate";
        String VALIDATION_RULE_ID = "validationRuleId";
        String BRANCH = "branch";
        String TEMPORARY = "temporary";
    }

    public enum WhitelistItemType {
        ALL, PERMANENT, TEMPORARY
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

    @Field(type = FieldType.Keyword)
    @Size(max = 300)
    private String assertionFailureText;

    @Field(type = FieldType.Boolean)
    private boolean temporary;

    @Field(type = FieldType.Keyword)
    private String reason;

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

    @JsonProperty(value = "creationDateLong")
    public Long getCreationDateLong() {
        if (this.creationDate == null) {
            return null;
        }

        return this.creationDate.getTime();
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

    public String getAssertionFailureText() {
        return assertionFailureText;
    }

    public void setAssertionFailureText(String assertionFailureText) {
        this.assertionFailureText = assertionFailureText;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhitelistItem item = (WhitelistItem) o;
        return Objects.equals(validationRuleId, item.validationRuleId) && Objects.equals(componentId, item.componentId) && Objects.equals(conceptId, item.conceptId) && Objects.equals(additionalFields, item.additionalFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validationRuleId, componentId, conceptId, additionalFields);
    }

    @Override
    public String toString() {
        return "WhitelistItem{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", creationDate=" + creationDate +
                ", validationRuleId='" + validationRuleId + '\'' +
                ", componentId='" + componentId + '\'' +
                ", conceptId='" + conceptId + '\'' +
                ", branch='" + branch + '\'' +
                ", additionalFields='" + additionalFields + '\'' +
                ", assertionFailureText='" + assertionFailureText + '\'' +
                ", temporary=" + temporary +
                ", reason='" + reason + '\'' +
                '}';
    }

    public String toString(boolean shouldTruncate, int maxLength) {
        return "{\n\t" +
                "\"conceptId\": " + getTextOrNull(getConceptId()) + ",\n\t" +
                "\"componentId\": " + getTextOrNull(getComponentId()) + ",\n\t" +
                "\"branchPath\": " + getTextOrNull(getBranch()) + ",\n\t" +
                (getReason() != null ? "\"reason\": " + getTextOrNull(getReason()) + ",\n\t" : "") +
                "\"fullComponent\": " + (getAdditionalFields() != null && shouldTruncate ? '\"' + truncateText(getAdditionalFields(), maxLength) + '\"' : getTextOrNull(getAdditionalFields())) + "\n" +
                "}";
    }

    String getTextOrNull(String text) {
        return text != null ? '\"' + text + '\"' : null;
    }

    private String truncateText(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}

package org.snomed.aag.data.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotBlank;

@Document(indexName = "criteria-item-sign-off")
public class CriteriaItemSignOff {
    public interface Fields {
        String ID = "id";
        String CRITERIA_ITEM_ID = "criteriaItemId";
        String TIMESTAMP = "timestamp";
        String BRANCH = "branch";
        String PROJECT_ITERATION = "projectIteration";
        String BRANCH_HEAD_TIMESTAMP = "branchHeadTimestamp";
        String USER_ID = "userId";
    }

    @Id
    @Field(type = FieldType.Keyword)
    @NotBlank
    private String id;

    @Field(type = FieldType.Keyword)
    @NotBlank
    private final String criteriaItemId;

    @Field(type = FieldType.Keyword)
    @NotBlank
    private final String branch;

    @Field(type = FieldType.Integer)
    private final Integer projectIteration;

    @Field(type = FieldType.Keyword)
    @NotBlank
    private final String userId;

    @Field(type = FieldType.Long)
    @NotBlank
    private final Long timestamp;

    @Field(type = FieldType.Long)
    @NotBlank
    private final Long branchHeadTimestamp;

    public CriteriaItemSignOff() {
        this.criteriaItemId = null;
        this.branch = null;
        this.projectIteration = null;
        this.userId = null;
        this.timestamp = null;
        this.branchHeadTimestamp = null;
    }

    public CriteriaItemSignOff(String criteriaItemId, String branch, Long branchHeadTimestamp, Integer projectIteration, String userId) {
        this.criteriaItemId = criteriaItemId;
        this.branch = branch;
        this.projectIteration = projectIteration;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
        this.branchHeadTimestamp = branchHeadTimestamp;
    }

    public String getId() {
        return id;
    }

    public String getCriteriaItemId() {
        return criteriaItemId;
    }

    public String getBranch() {
        return branch;
    }

    public Integer getProjectIteration() {
        return projectIteration;
    }

    public String getUserId() {
        return userId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Long getBranchHeadTimestamp() {
        return branchHeadTimestamp;
    }
}

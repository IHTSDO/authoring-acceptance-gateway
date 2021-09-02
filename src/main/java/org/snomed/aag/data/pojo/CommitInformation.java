package org.snomed.aag.data.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class CommitInformation {

	public static final String INTERNAL = "internal";
	public static final String CLASSIFIED = "classified";

	public enum CommitType {
		CONTENT, REBASE, PROMOTION
	}

	private String sourceBranchPath;
	private String targetBranchPath;
	private CommitType commitType;
	private long headTime;
	private Map<String, Object> metadata;

	public CommitInformation() {
	}

	public CommitInformation(String sourceBranchPath, CommitType commitType, long headTime, Map<String, Object> metadata) {
		this.sourceBranchPath = sourceBranchPath;
		this.commitType = commitType;
		this.headTime = headTime;
		this.metadata = metadata;
	}

	public CommitInformation(String sourceBranchPath, String targetBranchPath, CommitType commitType, long headTime, Map<String, Object> metadata) {
		this.sourceBranchPath = sourceBranchPath;
		this.targetBranchPath = targetBranchPath;
		this.commitType = commitType;
		this.headTime = headTime;
		this.metadata = metadata;
	}

	public boolean isClassified() {
		if (metadata != null && metadata.containsKey(INTERNAL)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> internalMap = (Map<String, Object>) metadata.get(INTERNAL);
			return Boolean.parseBoolean((String) internalMap.get(CLASSIFIED));
		}
		return false;
	}

	public String getSourceBranchPath() {
		return sourceBranchPath;
	}

	public String getTargetBranchPath() {
		return targetBranchPath;
	}

	public CommitType getCommitType() {
		return commitType;
	}

	public long getHeadTime() {
		return headTime;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	@JsonIgnore
	public boolean isContent() {
		return CommitType.CONTENT.equals(commitType);
	}

	@JsonIgnore
	public boolean isRebase() {
		return CommitType.REBASE.equals(commitType);
	}

	@JsonIgnore
	public boolean isPromotion() {
		return CommitType.PROMOTION.equals(commitType);
	}

	@JsonIgnore
	public String getBranchPathReceivingChanges() {
		// Content results in source Branch changing
		if (isContent()) {
			return getSourceBranchPath();
		}

		// Rebase & promotion results in target Branch changing
		return getTargetBranchPath();
	}

	@Override
	public String toString() {
		return "CommitInformation{" +
				"sourceBranchPath='" + sourceBranchPath + '\'' +
				", targetBranchPath='" + targetBranchPath + '\'' +
				", commitType=" + commitType +
				", headTime=" + headTime +
				", metadata=" + metadata +
				'}';
	}
}

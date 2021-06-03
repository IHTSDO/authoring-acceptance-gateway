package org.snomed.aag.data.pojo;

import java.util.Map;

public class CommitInformation {

	public static final String INTERNAL = "internal";
	public static final String CLASSIFIED = "classified";

	public enum CommitType {
		CONTENT, REBASE, PROMOTION;

	}

	private String path;
	private CommitType commitType;
	private long headTime;
	private Map<String, Object> metadata;

	public CommitInformation() {
	}

	public CommitInformation(String path, CommitType commitType, long headTime, Map<String, Object> metadata) {
		this.path = path;
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

	public String getPath() {
		return path;
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

	@Override
	public String toString() {
		return "CommitInformation{" +
				"path='" + path + '\'' +
				", commitType=" + commitType +
				", headTime=" + headTime +
				", metadata=" + metadata +
				'}';
	}
}

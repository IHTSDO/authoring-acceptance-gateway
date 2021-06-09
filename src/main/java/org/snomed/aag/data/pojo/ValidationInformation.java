package org.snomed.aag.data.pojo;

public class ValidationInformation {

	private String branchPath;
	private String validationStatus;
	private String reportUrl;

	public String getBranchPath() {
		return branchPath;
	}

	public String getValidationStatus() {
		return validationStatus;
	}

	public String getReportUrl() {
		return reportUrl;
	}

	@Override
	public String toString() {
		return "ValidationInformation{" +
				"branchPath='" + branchPath + '\'' +
				", validationStatus='" + validationStatus + '\'' +
				", reportUrl='" + reportUrl + '\'' +
				'}';
	}
}

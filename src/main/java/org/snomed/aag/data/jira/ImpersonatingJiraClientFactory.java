package org.snomed.aag.data.jira;

import net.rcarz.jiraclient.JiraClient;

public interface ImpersonatingJiraClientFactory {

	JiraClient getImpersonatingInstance(String username);
	JiraClient getAdminInstance();

}

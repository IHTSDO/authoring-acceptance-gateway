package org.snomed.aag.data.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.jira.ImpersonatingJiraClientFactory;
import org.snomed.aag.data.jira.JiraConfigMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class JMSListenerSnowstormService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JMSListenerSnowstormService.class);
	public static final int JIRA_SUMMARY_MAX_LENGTH = 255;

	@Value("${snowstorm.url}")
	private String snowstormUrl;

	@Value("${aag.jira.ticket.project}")
	private String project;

	@Value("${aag.jira.ticket.issueType}")
	private String issueType;

	@Value("${aag.jira.ticket.priority}")
	private String priority;

	@Value("${aag.jira.ticket.reporter}")
	private String reporter;

	@Value("${aag.jira.ticket.customField.snomedct.product}")
	private String snomedCtProduct;

	@Value("${aag.jira.ticket.customField.reporting.entity}")
	private String reportingEntity;

	@Value("${aag.jira.ticket.customField.reporting.entity.default.value}")
	private String reportingEntityDefaultValue;

	@Value("${aag.jira.ticket.customField.reporting.stage}")
	private String reportingStage;

	@Value("${aag.jira.ticket.customField.reporting.stage.default.value}")
	private String reportingStageDefaultValue;

	@Autowired
	private WhitelistService whitelistService;

	@Autowired
	private JiraConfigMapping jiraConfigMapping;

	@Autowired
	private ImpersonatingJiraClientFactory jiraClientFactory;

	@JmsListener(destination = "${snowstorm.jms.queue.prefix}.versioning.complete", containerFactory = "topicJmsListenerContainerFactory")
	void messageConsumer(TextMessage textMessage) throws JMSException, BusinessServiceException {
		try {
			LOGGER.info("receiveVersionCompleteEvent {}", textMessage);
			ObjectMapper objectMapper =  new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			final Map <String, Object> message = objectMapper.readValue(textMessage.getText(), Map.class);

			final String codeSystemShortname = (String) message.get("codeSystemShortName");
			final String codeSystemBranchPath = (String) message.get("codeSystemBranchPath");
			final String effectiveDate = (String) message.get("effectiveDate");
			List <WhitelistItem> whitelistItems = whitelistService.findAllByBranchAndMinimumCreationDate(codeSystemBranchPath, null, WhitelistItem.WhitelistItemType.TEMPORARY, true, Constants.LARGE_PAGE);
			for (WhitelistItem whitelistItem : whitelistItems) {
				Issue newIssue = createJiraIssue(codeSystemShortname, generateSummary(whitelistItem, codeSystemShortname, effectiveDate), generateDescription(whitelistItem));
				LOGGER.info("New {} ticket has been created.", newIssue.getKey());
				whitelistService.delete(whitelistItem);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to parse message. Message: {}.", textMessage);
		}
	}

	private String generateSummary(WhitelistItem whitelistItem, String codeSystemShortname, String effectiveDate) {
		String date = effectiveDate.substring(0, 4) + "-" + effectiveDate.substring(4,6) + "-" + effectiveDate.substring(6,8);
		String product = null;
		if (!CollectionUtils.isEmpty(jiraConfigMapping.getSnomedCtProducts()) &&
				jiraConfigMapping.getSnomedCtProducts().containsKey(codeSystemShortname)) {
			product = jiraConfigMapping.getSnomedCtProducts().get(codeSystemShortname);
		}

		String summary = product + ", " + date + ", " + whitelistItem.getValidationRuleId() + ", " + whitelistItem.getAssertionFailureText();
		if (summary.length() > JIRA_SUMMARY_MAX_LENGTH) {
			summary = summary.substring(0, JIRA_SUMMARY_MAX_LENGTH - 1);
		}

		return summary;
	}

	private String generateDescription(WhitelistItem whitelistItem) {
		StringBuilder result = new StringBuilder();
		result.append(whitelistItem.getAssertionFailureText()).append("\n")
				.append("Concept ID: ").append(whitelistItem.getConceptId()).append("\n")
				.append("Component ID: ").append(whitelistItem.getComponentId()).append("\n")
				.append("Full Component: ").append(whitelistItem.getAdditionalFields()).append("\n")
				.append("Branch Path: ").append(whitelistItem.getBranch()).append("\n");
		if (whitelistItem.getReason() != null) {
			result.append("Reason: ").append(whitelistItem.getReason()).append("\n");
		}

		result.append("Environment: ").append(getEnvironment()).append("\n");

		return result.toString();
	}

	private String getEnvironment() {
		URI uri;
		try {
			uri = new URI(snowstormUrl);
		} catch (URISyntaxException e) {
			LOGGER.error("Failed to detect environment", e);
			return null;
		}
		String domain = uri.getHost();
		domain = domain.startsWith("www.") ? domain.substring(4) : domain;
		return (domain.contains("-") ? domain.substring(0, domain.lastIndexOf("-")) : domain.substring(0, domain.indexOf("."))).toUpperCase();
	}

	private Issue createJiraIssue(String codeSystemShortname, String summary, String description) throws BusinessServiceException {
		Issue jiraIssue;
		try {
			jiraIssue = getJiraClient().createIssue(project, issueType)
					.field(Field.SUMMARY, summary)
					.field(Field.DESCRIPTION, description)
					.execute();

			final Issue.FluentUpdate updateRequest = jiraIssue.update();
			updateRequest.field(Field.PRIORITY, priority);
			updateRequest.field(Field.ASSIGNEE, "");
			updateRequest.field(Field.REPORTER, reporter);

			updateRequest.field(reportingEntity, Arrays.asList(reportingEntityDefaultValue));
			updateRequest.field(reportingStage, Arrays.asList(reportingStageDefaultValue));

			if (!CollectionUtils.isEmpty(jiraConfigMapping.getSnomedCtProducts()) &&
				jiraConfigMapping.getSnomedCtProducts().containsKey(codeSystemShortname)) {
				updateRequest.field(snomedCtProduct, Arrays.asList(jiraConfigMapping.getSnomedCtProducts().get(codeSystemShortname)));
			}

			updateRequest.execute();
		} catch (JiraException e) {
			LOGGER.error(e.getMessage());
			throw new BusinessServiceException("Failed to create Jira task. Error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
		}

		return jiraIssue;
	}

	private JiraClient getJiraClient() {
		return jiraClientFactory.getImpersonatingInstance(reporter);
	}
}

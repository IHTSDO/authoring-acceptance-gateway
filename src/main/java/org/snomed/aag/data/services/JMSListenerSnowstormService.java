package org.snomed.aag.data.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JMSListenerSnowstormService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JMSListenerSnowstormService.class);
	private static final int FULL_COMPONENT_MAX_LENGTH = 1000;
	public static final int JIRA_SUMMARY_MAX_LENGTH = 255;

	@Value("${snowstorm.url}")
	private String snowstormUrl;

	@Value("${aag.jira.ticket.project}")
	private String project;

	@Value("${aag.jira.ticket.issueType}")
	private String issueType;

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

	@Value("${aag.jira.ticket.customField.product.release.date}")
	private String productReleaseDate;

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
			List<WhitelistItem> whitelistItems = whitelistService.findAllByBranchAndMinimumCreationDate(codeSystemBranchPath, null, WhitelistItem.WhitelistItemType.TEMPORARY, true, Constants.LARGE_PAGE);
			Map<String, List<WhitelistItem>> assertionToWhitelistItemsMap = whitelistItems.stream().collect(
					Collectors.groupingBy(WhitelistItem::getValidationRuleId, Collectors.toCollection(ArrayList::new))
			);
			for (Map.Entry<String, List<WhitelistItem>> entry : assertionToWhitelistItemsMap.entrySet()) {
				Issue newIssue = createJiraIssue(codeSystemShortname, generateSummary(entry, codeSystemShortname, effectiveDate), generateDescription(entry), effectiveDate);
				LOGGER.info("New {} ticket has been created.", newIssue.getKey());

				// Add attachment and update JIRA custom fields
				Issue.NewAttachment[] attachments = new Issue.NewAttachment[1];
				attachments[0] = new Issue.NewAttachment(entry.getKey() + ".json", getPrettyString(generateAttachment(entry)).getBytes());
				newIssue.addAttachments(attachments);

				whitelistService.deleteAll(entry.getValue());
			}
		} catch (IOException e) {
			LOGGER.error("Failed to parse message. Message: {}.", textMessage);
		} catch (JiraException e) {
            throw new RuntimeException(e);
        }
    }

	private String generateSummary(Map.Entry<String, List<WhitelistItem>> entry, String codeSystemShortname, String effectiveDate) {
		String date = getDateAsString(effectiveDate);
		String product = null;
		if (!CollectionUtils.isEmpty(jiraConfigMapping.getSnomedCtProducts()) &&
				jiraConfigMapping.getSnomedCtProducts().containsKey(codeSystemShortname)) {
			product = jiraConfigMapping.getSnomedCtProducts().get(codeSystemShortname);
		}
		WhitelistItem firstItem = entry.getValue().get(0);
		String summary = product + ", " + date + ", " + entry.getKey() + ", " + firstItem.getAssertionFailureText();
		if (summary.length() > JIRA_SUMMARY_MAX_LENGTH) {
			summary = summary.substring(0, JIRA_SUMMARY_MAX_LENGTH - 1);
		}

		return summary;
	}

	private String getDateAsString(String effectiveDate) {
		return effectiveDate.substring(0, 4) + "-" + effectiveDate.substring(4,6) + "-" + effectiveDate.substring(6,8);
	}

	private String generateDescription(Map.Entry<String, List<WhitelistItem>> entry) {
		WhitelistItem firstItem = entry.getValue().get(0);
		StringBuilder result = new StringBuilder(firstItem.getAssertionFailureText() + "\n"
				+ "Total number of failures: " + entry.getValue().size() + "\n");
		result.append("Environment: ").append(getEnvironment()).append("\n");
		List<WhitelistItem> firstNInstances = getFirstNInstances(entry.getValue(), 10);
		if (!firstNInstances.isEmpty()) {
			result.append("First ").append(firstNInstances.size()).append(" failures: \n");
			for (WhitelistItem whitelistItem: firstNInstances) {
				result.append("* ").append(whitelistItem.toString(true, FULL_COMPONENT_MAX_LENGTH)).append("\n");
			}
		}
		return result.toString();
	}

	private String generateAttachment(Map.Entry<String, List<WhitelistItem>> entry) {
		WhitelistItem firstItem = entry.getValue().get(0);
		return "{" +
				"\"assertionUuid\": \"" + firstItem.getValidationRuleId() + '\"' +
				", \"assertionText\": \"" + firstItem.getAssertionFailureText() + '\"' +
				", \"failureCount\": " + entry.getValue().size() +
				", \"firstNInstances\": [" + entry.getValue().stream().map(item -> item.toString(false, FULL_COMPONENT_MAX_LENGTH)).collect(Collectors.joining(",")) + "]" +
				'}';
	}

	private List<WhitelistItem> getFirstNInstances(List<WhitelistItem> instances, int numberOfItem) {
		if (instances == null) {
			return Collections.emptyList();
		}
		if (numberOfItem < 0) {
			return instances;
		}

		int firstNCount = Math.min(numberOfItem, instances.size());
		return instances.subList(0, firstNCount);
	}

	private String getPrettyString(String input) {
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(input);
		return gson.toJson(je);
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

	private Issue createJiraIssue(String codeSystemShortname, String summary, String description, String releaseDate) throws BusinessServiceException {
		Issue jiraIssue;
		try {
			jiraIssue = getJiraClient().createIssue(project, issueType)
					.field(Field.SUMMARY, summary)
					.field(Field.DESCRIPTION, description)
					.execute();

			final Issue.FluentUpdate updateRequest = jiraIssue.update();
			updateRequest.field(Field.ASSIGNEE, "");
			updateRequest.field(Field.REPORTER, reporter);

			updateRequest.field(reportingEntity, Arrays.asList(reportingEntityDefaultValue));
			updateRequest.field(reportingStage, Arrays.asList(reportingStageDefaultValue));
			updateRequest.field(productReleaseDate, getDateAsString(releaseDate));

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

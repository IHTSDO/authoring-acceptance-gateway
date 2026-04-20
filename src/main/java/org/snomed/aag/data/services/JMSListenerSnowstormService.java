package org.snomed.aag.data.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import net.sf.json.JSONObject;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.jira.JiraCloudClient;
import org.snomed.aag.data.jira.JiraConfigMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JMSListenerSnowstormService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JMSListenerSnowstormService.class);
	private static final int FULL_COMPONENT_MAX_LENGTH = 1000;
	public static final int JIRA_SUMMARY_MAX_LENGTH = 255;

	public static final String VALUE = "value";

	@Value("${aag.jira.ticket.generation.enabled}")
	private boolean ticketGenrationEnabled;

	@Value("${snowstorm.url}")
	private String snowstormUrl;

	@Value("${aag.jira.ticket.project}")
	private String project;

	@Value("${aag.jira.ticket.issueType}")
	private String issueType;

	@Value("${jira.cloud.reporter-accountid}")
	private String reporterAccountId;

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
	private JiraConfigMapping jiraConfigMapping;

	@Autowired
	private WhitelistService whitelistService;

	@Autowired
	private JiraCloudClient jiraCloudClient;

	@JmsListener(destination = "${snowstorm.jms.queue.prefix}.versioning.complete", containerFactory = "topicJmsListenerContainerFactory")
	void messageConsumer(TextMessage textMessage) throws JMSException, BusinessServiceException {
		try {
			LOGGER.info("receiveVersionCompleteEvent {}", textMessage);
			if (!ticketGenrationEnabled) return;

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
				String issueKey = createJiraIssue(generateSummary(entry, codeSystemShortname, effectiveDate), generateDescription(entry));
				LOGGER.info("New {} ticket has been created.", issueKey);

				// Add attachment and update JIRA custom fields
				jiraCloudClient.addAttachment(issueKey, entry.getKey() + ".json", getPrettyString(generateAttachment(entry)).getBytes());

				// Update other fields
				updateJiraIssue(issueKey, codeSystemShortname, effectiveDate);

				whitelistService.deleteAll(entry.getValue());
			}
		} catch (IOException e) {
			LOGGER.error("Failed to parse message. Message: {}.", textMessage);
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
		String summary = (product != null ? product : codeSystemShortname) + ", " + date + ", " + entry.getKey() + ", " + firstItem.getAssertionFailureText();
		if (summary.length() > JIRA_SUMMARY_MAX_LENGTH) {
			summary = summary.substring(0, JIRA_SUMMARY_MAX_LENGTH - 1);
		}

		return summary;
	}

	private String getDateAsString(String effectiveDate) {
		return effectiveDate != null ?  effectiveDate.substring(0, 4) + "-" + effectiveDate.substring(4,6) + "-" + effectiveDate.substring(6,8) : null;
	}

	private String generateDescription(Map.Entry<String, List<WhitelistItem>> entry) {
		WhitelistItem firstItem = entry.getValue().get(0);
		StringBuilder result = new StringBuilder();
		result.append("Environment: ").append(getEnvironment()).append("\n");
		result.append("User: ").append(reporter).append("\n").append("\n");

		result.append(firstItem.getAssertionFailureText()).append("\n").append("Total number of failures: ").append(entry.getValue().size()).append("\n");
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

	private String createJiraIssue(String summary, String description) throws BusinessServiceException {
		try {
			JSONObject issue = jiraCloudClient.createIssue(project, summary, description, issueType, reporterAccountId);
			return issue.getString("key");
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to create Jira ticket. Error: " + e.getMessage(), e);
		}
	}

	private void updateJiraIssue(String issueKey, String codeSystemShortname, String releaseDate) throws BusinessServiceException {
		try {
			JSONObject issueFields = new JSONObject();

			issueFields.put(productReleaseDate, getDateAsString(releaseDate));

			JSONObject reportingEntityField = new JSONObject();
			reportingEntityField.put(VALUE, reportingEntityDefaultValue);
			issueFields.put(reportingEntity, reportingEntityField);

			JSONObject reportingStageField = new JSONObject();
			reportingStageField.put(VALUE, reportingStageDefaultValue);
			issueFields.put(reportingStage, Collections.singletonList(reportingStageField));

			if (StringUtils.hasLength(codeSystemShortname) && jiraConfigMapping.getSnomedCtProducts().containsKey(codeSystemShortname)) {
				JSONObject snomedCtProductField = new JSONObject();
				snomedCtProductField.put(VALUE, jiraConfigMapping.getSnomedCtProducts().get(codeSystemShortname));
				issueFields.put(snomedCtProduct, snomedCtProductField);
			}

			jiraCloudClient.updateIssue(issueKey, issueFields);
		} catch (IOException e) {
			throw new BusinessServiceException("Jira ticket has been created successfully but failed to update. Error: " + e.getMessage(), e);
		}
	}
}

package org.snomed.aag.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.TestConfig;
import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.pojo.CommitInformation;
import org.snomed.aag.rest.pojo.ProjectAcceptanceCriteriaDTO;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
class ServiceIntegrationControllerTest extends AbstractTest {
	private ServiceIntegrationController serviceIntegrationController;
	private AcceptanceController acceptanceController;
	private AcceptanceCriteriaController acceptanceCriteriaController;
	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() {
		this.serviceIntegrationController = new ServiceIntegrationController(commitInformationValidator, acceptanceService, projectAcceptanceCriteriaService);
		this.acceptanceController = new AcceptanceController(securityService, projectAcceptanceCriteriaService, acceptanceService);
		this.acceptanceCriteriaController = new AcceptanceCriteriaController(projectAcceptanceCriteriaService, projectAcceptanceCriteriaUpdateValidator);
		this.mockMvc = MockMvcBuilders
				.standaloneSetup(serviceIntegrationController, acceptanceController, acceptanceCriteriaController)
				.setControllerAdvice(new RestControllerAdvice())
				.build();
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenNoCommitTypeGiven() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		CommitInformation commitInformation = new CommitInformation("path", null, 10L, Collections.emptyMap());

		// when
		ResultActions resultActions = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);

		// then
		assertResponseStatus(resultActions, 400);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.BAD_REQUEST, "No CommitType specified."));
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenNoPathGiven() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		CommitInformation commitInformation = new CommitInformation(null, CommitInformation.CommitType.PROMOTION, 10L, Collections.emptyMap());

		// when
		ResultActions resultActions = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);

		// then
		assertResponseStatus(resultActions, 400);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.BAD_REQUEST, "No sourceBranchPath specified."));
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenNoHeadTimeGiven() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		CommitInformation commitInformation = new CommitInformation("path", CommitInformation.CommitType.PROMOTION, 0L, Collections.emptyMap());

		// when
		ResultActions resultActions = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);

		// then
		assertResponseStatus(resultActions, 400);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.BAD_REQUEST, "No headTime specified."));
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenGivenCommitTypeIsNotPromotion() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		CommitInformation commitInformation = new CommitInformation("path", CommitInformation.CommitType.CONTENT, 1L, Collections.emptyMap());

		// when
		ResultActions resultActions = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);

		// then
		assertResponseStatus(resultActions, 200);
		assertResponseBodyIsEmpty(resultActions);
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenNoPACFoundForBranch() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		String branchPath = "MAIN/projectA/taskB";
		CommitInformation commitInformation = new CommitInformation(branchPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());

		// when
		ResultActions resultActions = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);

		// then
		assertResponseStatus(resultActions, 204);
		assertResponseBody(resultActions, String.format("No Project Acceptance Criteria found for branch %s. Returning " + HttpStatus.NO_CONTENT + ".", branchPath));
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenPACHasNoCriteriaItems() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		String branchPath = "MAIN/projectA/taskB";
		CommitInformation commitInformation = new CommitInformation(branchPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());

		givenProjectAcceptanceCriteriaExists(branchPath, 1, Collections.emptySet(), Collections.emptySet());

		// when
		ResultActions resultActions = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);

		// then
		assertResponseStatus(resultActions, 409);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.CONFLICT.value(), "Project Acceptance Criteria has no Criteria Items."));
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenPACIsComplete() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskB";
		CommitInformation commitInformation = new CommitInformation(taskPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenProjectAcceptanceCriteriaExists(projectPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId, AuthoringLevel.PROJECT);
		givenCriteriaItemExists(taskCriteriaId, true, 1, taskCriteriaId, AuthoringLevel.TASK);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaId); // Only task CriteriaItem has been approved.
		givenBranchDoesExist(taskPath);

		// when
		ResultActions resultActions = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);

		// then
		assertResponseStatus(resultActions, 200);
		assertResponseBodyIsEmpty(resultActions);
	}

	@Test
	void receiveCommitInformation_ShouldIncrementPAC_WhenPACComplete() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String receiveCommitInformation = receiveCommitInformation();
		String findForBranch = findForBranch(withPipeInsteadOfSlash(branchPath));
		CommitInformation commitInformation = new CommitInformation(branchPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenProjectAcceptanceCriteriaExists(branchPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId, AuthoringLevel.PROJECT);
		givenCriteriaItemExists(taskCriteriaId, true, 1, taskCriteriaId, AuthoringLevel.TASK);
		givenCriteriaItemSignOffExists(branchPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(branchPath, taskCriteriaId);
		givenBranchDoesExist(branchPath);

		mockMvc.perform(post(receiveCommitInformation).contentType(MediaType.APPLICATION_JSON).content(asJson(commitInformation)));

		// when
		ResultActions resultActions = mockMvc.perform(get(findForBranch).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteria projectAcceptanceCriteria = toProjectAcceptanceCriteria(getResponseBody(resultActions));

		// then
		assertEquals(2, projectAcceptanceCriteria.getProjectIteration()); //Project iteration has been incremented
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenPACIsIncomplete() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		String projectPath = "MAIN/projectA";
		String taskBranch = "MAIN/projectA/taskB";
		CommitInformation commitInformation = new CommitInformation(taskBranch, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenBranchDoesExist(taskBranch);
		givenProjectAcceptanceCriteriaExists(projectPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId, AuthoringLevel.PROJECT);
		givenCriteriaItemExists(taskCriteriaId, true, 1, taskCriteriaId, AuthoringLevel.TASK);

		// when
		ResultActions resultActions = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);

		// then
		assertResponseStatus(resultActions, 409);
		assertResponseBodyIsEmpty(resultActions);
	}

	@Test
	void receiveCommitInformation_ShouldReturnExpectedResponse_WhenRequestingTwice() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskB";
		CommitInformation commitInformation = new CommitInformation(taskPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenProjectAcceptanceCriteriaExists(projectPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId, AuthoringLevel.PROJECT);
		givenCriteriaItemExists(taskCriteriaId, true, 1, taskCriteriaId, AuthoringLevel.TASK);
		givenCriteriaItemSignOffExists(taskPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaId);
		givenBranchDoesExist(taskPath);

		// first request
		ResultActions firstRequest = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);
		assertResponseStatus(firstRequest, 200);

		// second request
		ResultActions secondRequest = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(commitInformation))
				);
		assertResponseStatus(secondRequest, 200); // Project has not been incremented as Project only increments if Project itself is promoted
	}

	@Test
	void receiveCommitInformation_ShouldOnlyIncrementProject_WhenProjectIsPromoted() throws Exception {
		// given
		String requestUrl = receiveCommitInformation();
		String codeSystemPath = "MAIN";
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskB";
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";
		String findForBranch = findForBranch(withPipeInsteadOfSlash(projectPath));

		givenProjectAcceptanceCriteriaExists(projectPath, 0, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId, AuthoringLevel.PROJECT);
		givenCriteriaItemExists(taskCriteriaId, true, 1, taskCriteriaId, AuthoringLevel.TASK);
		givenCriteriaItemSignOffExists(projectPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(taskPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(projectPath, taskCriteriaId);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaId);
		givenBranchDoesExist(codeSystemPath);
		givenBranchDoesExist(projectPath);

		// Promoting task does not increment project iteration
		CommitInformation taskPromotionRequest = new CommitInformation(taskPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());
		ResultActions promoteTaskResponse = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(taskPromotionRequest))
				);
		assertResponseStatus(promoteTaskResponse, 200);
		ResultActions findForTaskResponse = mockMvc.perform(get(findForBranch).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteria findForTaskPAC = toProjectAcceptanceCriteria(getResponseBody(findForTaskResponse));
		assertEquals(0, findForTaskPAC.getProjectIteration());

		// Promoting project increments project iteration
		CommitInformation projectPromotionRequest = new CommitInformation(projectPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());
		ResultActions promoteProject = mockMvc
				.perform(post(requestUrl)
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJson(projectPromotionRequest))
				);
		assertResponseStatus(promoteProject, 200);
		ResultActions findForProjectResponse = mockMvc.perform(get(findForBranch).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteria findForProjectPAC = toProjectAcceptanceCriteria(getResponseBody(findForProjectResponse));
		assertEquals(1, findForProjectPAC.getProjectIteration());
	}

	@Test
	void receiveCommitInformation_ShouldExpireExpectedCriteria_WhenActioningRebase() throws Exception {
		// Set up
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskA";
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaIdA = "task-criteria-id-a";
		String taskCriteriaIdB = "task-criteria-id-b";

		givenBranchDoesExist();
		givenCriteriaItemExists(projectCriteriaId, true, 1, projectCriteriaId, AuthoringLevel.PROJECT);
		givenCriteriaItemExists(taskCriteriaIdA, true, 2, taskCriteriaIdA, AuthoringLevel.TASK, true);
		givenCriteriaItemExists(taskCriteriaIdB, true, 3, taskCriteriaIdB, AuthoringLevel.TASK, false);
		givenProjectAcceptanceCriteriaExists(projectPath, 0, Set.of(projectCriteriaId), Set.of(taskCriteriaIdA, taskCriteriaIdB));

		// Complete criteria for task
		givenCriteriaItemSignOffExists(taskPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaIdA);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaIdB);

		// View task's PAC before rebase
		String viewCriteriaItemsForTaskA = viewCriteriaItems(withPipeInsteadOfSlash(taskPath));
		ResultActions viewPACFirstRequest = mockMvc.perform(get(viewCriteriaItemsForTaskA).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteriaDTO pacFirstRequest = OBJECT_MAPPER.readValue(getResponseBody(viewPACFirstRequest), ProjectAcceptanceCriteriaDTO.class);
		assertEquals(3, pacFirstRequest.getCriteriaItems().stream().filter(CriteriaItem::isComplete).collect(Collectors.toSet()).size());

		// Rebase project onto task
		String receiveCommitInformation = receiveCommitInformation();
		CommitInformation projectRebase = new CommitInformation(projectPath, taskPath, CommitInformation.CommitType.REBASE, 1L, Collections.emptyMap());
		ResultActions rebaseProject = mockMvc.perform(post(receiveCommitInformation).contentType(MediaType.APPLICATION_JSON).content(asJson(projectRebase)));

		// View task's PAC after rebase
		Thread.sleep(5000); // Pause for async request to complete
		ResultActions viewPACSecondRequest = mockMvc.perform(get(viewCriteriaItemsForTaskA).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteriaDTO pacSecondRequest = OBJECT_MAPPER.readValue(getResponseBody(viewPACSecondRequest), ProjectAcceptanceCriteriaDTO.class);
		assertEquals(2, pacSecondRequest.getCriteriaItems().stream().filter(CriteriaItem::isComplete).collect(Collectors.toSet()).size()); // 1 has been expired
	}

	@Test
	void receiveCommitInformation_ShouldExpireExpectedCriteria_WhenActioningPromotion() throws Exception {
		// Set up
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskA";
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaIdA = "task-criteria-id-a";
		String taskCriteriaIdB = "task-criteria-id-b";

		givenBranchDoesExist();
		givenCriteriaItemExists(projectCriteriaId, true, 1, projectCriteriaId, AuthoringLevel.PROJECT);
		givenCriteriaItemExists(taskCriteriaIdA, true, 2, taskCriteriaIdA, AuthoringLevel.TASK, true);
		givenCriteriaItemExists(taskCriteriaIdB, true, 3, taskCriteriaIdB, AuthoringLevel.TASK, false);
		givenProjectAcceptanceCriteriaExists(projectPath, 0, Set.of(projectCriteriaId), Set.of(taskCriteriaIdA, taskCriteriaIdB));

		// Complete criteria for project (1 will expire after promotion)
		givenCriteriaItemSignOffExists(projectPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(projectPath, taskCriteriaIdA);
		givenCriteriaItemSignOffExists(projectPath, taskCriteriaIdB);

		// Complete criteria for task (in order to promote task)
		givenCriteriaItemSignOffExists(taskPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaIdA);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaIdB);

		// View project's PAC before promotion
		String viewCriteriaItemsForProject = viewCriteriaItems(withPipeInsteadOfSlash(projectPath));
		ResultActions viewPACFirstRequest = mockMvc.perform(get(viewCriteriaItemsForProject).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteriaDTO pacFirstRequest = OBJECT_MAPPER.readValue(getResponseBody(viewPACFirstRequest), ProjectAcceptanceCriteriaDTO.class);
		assertEquals(3, pacFirstRequest.getCriteriaItems().stream().filter(CriteriaItem::isComplete).collect(Collectors.toSet()).size());

		// Promote task to project
		String receiveCommitInformation = receiveCommitInformation();
		CommitInformation taskPromotion = new CommitInformation(taskPath, projectPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());
		ResultActions taskPromotionResponse = mockMvc.perform(post(receiveCommitInformation).contentType(MediaType.APPLICATION_JSON).content(asJson(taskPromotion)));

		// View project's PAC after task promotion
		Thread.sleep(5000); // Pause for async request to complete
		ResultActions viewPACSecondRequest = mockMvc.perform(get(viewCriteriaItemsForProject).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteriaDTO pacSecondRequest = OBJECT_MAPPER.readValue(getResponseBody(viewPACSecondRequest), ProjectAcceptanceCriteriaDTO.class);
		assertEquals(2, pacSecondRequest.getCriteriaItems().stream().filter(CriteriaItem::isComplete).collect(Collectors.toSet()).size()); // 1 has been expired
	}

	@Test
	void receiveCommitInformation_ShouldNotResultInExpiredTaskCriteria_WhenProjectIsPromotedToCodeSystem() throws Exception {
		// Set up
		String codeSystemPath = "MAIN";
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskA";
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaIdA = "task-criteria-id-a";
		String taskCriteriaIdB = "task-criteria-id-b";

		givenBranchDoesExist();
		givenCriteriaItemExists(projectCriteriaId, true, 1, projectCriteriaId, AuthoringLevel.PROJECT);
		givenCriteriaItemExists(taskCriteriaIdA, true, 2, taskCriteriaIdA, AuthoringLevel.TASK, true);
		givenCriteriaItemExists(taskCriteriaIdB, true, 3, taskCriteriaIdB, AuthoringLevel.TASK, false);
		givenProjectAcceptanceCriteriaExists(projectPath, 0, Set.of(projectCriteriaId), Set.of(taskCriteriaIdA, taskCriteriaIdB));

		// Complete criteria for project (all will expire after promotion)
		givenCriteriaItemSignOffExists(projectPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(projectPath, taskCriteriaIdA);
		givenCriteriaItemSignOffExists(projectPath, taskCriteriaIdB);

		// Complete criteria for task (none expire after promotion)
		givenCriteriaItemSignOffExists(taskPath, projectCriteriaId);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaIdA);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaIdB);

		// View project's PAC before promotion
		String viewCriteriaItemsForProject = viewCriteriaItems(withPipeInsteadOfSlash(projectPath));
		ResultActions viewProjectPACFirstRequest = mockMvc.perform(get(viewCriteriaItemsForProject).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteriaDTO projectPacFirstRequest = OBJECT_MAPPER.readValue(getResponseBody(viewProjectPACFirstRequest), ProjectAcceptanceCriteriaDTO.class);
		assertEquals(3, projectPacFirstRequest.getCriteriaItems().stream().filter(CriteriaItem::isComplete).collect(Collectors.toSet()).size());

		// View task's PAC before promotion
		String viewCriteriaItemsForTaskA = viewCriteriaItems(withPipeInsteadOfSlash(taskPath));
		ResultActions viewTaskPACFirstRequest = mockMvc.perform(get(viewCriteriaItemsForTaskA).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteriaDTO taskPacFirstRequest = OBJECT_MAPPER.readValue(getResponseBody(viewTaskPACFirstRequest), ProjectAcceptanceCriteriaDTO.class);
		assertEquals(3, taskPacFirstRequest.getCriteriaItems().stream().filter(CriteriaItem::isComplete).collect(Collectors.toSet()).size());

		// Promote project to codesystem
		String receiveCommitInformation = receiveCommitInformation();
		CommitInformation projectPromotion = new CommitInformation(projectPath, codeSystemPath, CommitInformation.CommitType.PROMOTION, 1L, Collections.emptyMap());
		ResultActions projectPromotionResponse = mockMvc.perform(post(receiveCommitInformation).contentType(MediaType.APPLICATION_JSON).content(asJson(projectPromotion)));

		// Pause for async request to complete
		Thread.sleep(5000);

		// View project's PAC after task promotion
		ResultActions viewPACSecondRequest = mockMvc.perform(get(viewCriteriaItemsForProject).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteriaDTO pacSecondRequest = OBJECT_MAPPER.readValue(getResponseBody(viewPACSecondRequest), ProjectAcceptanceCriteriaDTO.class);
		assertEquals(0, pacSecondRequest.getCriteriaItems().stream().filter(CriteriaItem::isComplete).collect(Collectors.toSet()).size()); // all have been expired

		// View task's PAC before promotion
		ResultActions viewTaskPACSecondRequest = mockMvc.perform(get(viewCriteriaItemsForTaskA).contentType(MediaType.APPLICATION_JSON));
		ProjectAcceptanceCriteriaDTO taskPacSecondResponse = OBJECT_MAPPER.readValue(getResponseBody(viewTaskPACSecondRequest), ProjectAcceptanceCriteriaDTO.class);
		assertEquals(3, taskPacSecondResponse.getCriteriaItems().stream().filter(CriteriaItem::isComplete).collect(Collectors.toSet()).size()); // none have been expired
	}

	private String receiveCommitInformation() {
		return "/integration/snowstorm/commit";
	}

	private String signOffCriteriaItem(String branchPath, String criteriaItemId) {
		return "/acceptance/" + branchPath + "/item/" + criteriaItemId + "/accept";
	}

	private String findForBranch(String branchPath) {
		return "/criteria/" + branchPath;
	}

	private String viewCriteriaItems(String branchPath) {
		return "/acceptance/" + branchPath;
	}

	private void givenProjectAcceptanceCriteriaExists(String branchPath, Integer projectIteration, String projectCriteriaId, String taskCriteriaId) {
		ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, projectIteration);
		projectAcceptanceCriteria.setSelectedProjectCriteriaIds(Collections.singleton(projectCriteriaId));
		projectAcceptanceCriteria.setSelectedTaskCriteriaIds(Collections.singleton(taskCriteriaId));
		projectAcceptanceCriteriaRepository.save(projectAcceptanceCriteria);
	}

	private void givenProjectAcceptanceCriteriaExists(String branchPath, Integer projectIteration, Set<String> projectCriteria, Set<String> taskCriteria) {
		ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, projectIteration);
		projectAcceptanceCriteria.setSelectedProjectCriteriaIds(projectCriteria);
		projectAcceptanceCriteria.setSelectedTaskCriteriaIds(taskCriteria);
		projectAcceptanceCriteriaRepository.save(projectAcceptanceCriteria);
	}

	private void givenCriteriaItemExists(String criteriaItemId, boolean manual, int order, String label, AuthoringLevel authoringLevel) {
		CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
		criteriaItem.setManual(manual);
		criteriaItem.setRequiredRole("ROLE_SERVICE_INTEGRATION_CONTROLLER_TEST");
		criteriaItem.setOrder(order);
		criteriaItem.setLabel(label);
		criteriaItem.setAuthoringLevel(authoringLevel);

		criteriaItemRepository.save(criteriaItem);
	}


	private void givenCriteriaItemExists(String criteriaItemId, boolean manual, int order, String label, AuthoringLevel authoringLevel, String enabledByFlag) {
		CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
		criteriaItem.setManual(manual);
		criteriaItem.setRequiredRole("ROLE_SERVICE_INTEGRATION_CONTROLLER_TEST");
		criteriaItem.setOrder(order);
		criteriaItem.setLabel(label);
		criteriaItem.setAuthoringLevel(authoringLevel);
		criteriaItem.setEnabledByFlag(Set.of(enabledByFlag));

		criteriaItemRepository.save(criteriaItem);
	}

	private void givenCriteriaItemSignOffExists(String branchPath, String criteriaItemId) throws Exception {
		String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash(branchPath), criteriaItemId);
		givenUserDoesHavePermissionForBranch();
		givenBranchDoesExist(System.currentTimeMillis());
		givenAuthenticatedUser("ServiceIntegrationControllerTest");

		ResultActions resultActions = mockMvc.perform(post(requestUrl));

		assertResponseStatus(resultActions, 200);
	}

	private ProjectAcceptanceCriteria toProjectAcceptanceCriteria(String response) throws JsonProcessingException {
		return OBJECT_MAPPER.readValue(response, new TypeReference<>() {
		});
	}
}

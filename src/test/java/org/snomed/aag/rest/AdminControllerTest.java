package org.snomed.aag.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.TestConfig;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.rest.pojo.ProjectAcceptanceCriteriaDTO;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
class AdminControllerTest extends AbstractTest {
	private AdminController adminController;
	private AcceptanceController acceptanceController;
	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() {
		this.adminController = new AdminController(acceptanceService);
		this.acceptanceController = new AcceptanceController(
				securityService,
				projectAcceptanceCriteriaService,
				acceptanceService
		);
		this.mockMvc = MockMvcBuilders
				.standaloneSetup(adminController, acceptanceController)
				.setControllerAdvice(new RestControllerAdvice())
				.build();
	}

	@Test
	void signOffAllCriteriaItems_ShouldReturnExpectedResponse_WhenBranchDoesNotExist() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = signOffAllCriteriaItems(withPipeInsteadOfSlash(branchPath));

		givenBranchDoesNotExist();

		// when
		ResultActions resultActions = mockMvc.perform(post(requestUrl));

		// then
		assertResponseStatus(resultActions, 403);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Branch does not exist."));
	}

	@Test
	void signOffAllCriteriaItems_ShouldReturnExpectedResponse_WhenBranchHasNoProjectAcceptance() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = signOffAllCriteriaItems(withPipeInsteadOfSlash(branchPath));

		givenBranchDoesExist();

		// when
		ResultActions resultActions = mockMvc.perform(post(requestUrl));

		// then
		assertResponseStatus(resultActions, 404);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND.value(), "Cannot find Acceptance Criteria for " + branchPath + "."));
	}

	@Test
	void signOffAllCriteriaItems_ShouldReturnExpectedResponse_WhenProjectAcceptanceIsEmpty() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = signOffAllCriteriaItems(withPipeInsteadOfSlash(branchPath));

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(branchPath, 1);

		// when
		ResultActions resultActions = mockMvc.perform(post(requestUrl));

		// then
		assertResponseStatus(resultActions, 409);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.CONFLICT.value(), "Acceptance Criteria for " + branchPath + " has no Criteria Items configured."));
	}

	@Test
	void signOffAllCriteriaItems_ShouldReturnExpectedResponse_WhenProjectAcceptanceHasCriteriaItemThatDoesNotExist() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = signOffAllCriteriaItems(withPipeInsteadOfSlash(branchPath));
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(branchPath, 1, projectCriteriaId, taskCriteriaId);

		// when
		ResultActions resultActions = mockMvc.perform(post(requestUrl));

		// then
		assertResponseStatus(resultActions, 404);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND, "Criteria Item with id '" + projectCriteriaId + "' not found."));
	}

	@Test
	void signOffAllCriteriaItems_ShouldReturnExpectedResponseStatus_WhenAllCriteriaItemsHaveBeenSignedOffForProjectAcceptanceCriteria() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = signOffAllCriteriaItems(withPipeInsteadOfSlash(branchPath));
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(branchPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId);
		givenCriteriaItemExists(taskCriteriaId, false, 1, taskCriteriaId);

		// when
		ResultActions resultActions = mockMvc.perform(post(requestUrl));

		// then
		assertResponseStatus(resultActions, 200);
	}

	@Test
	void signOffAllCriteriaItems_ShouldReturnExpectedResponseBody_WhenAllCriteriaItemsHaveBeenSignedOffForProjectAcceptanceCriteria() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = signOffAllCriteriaItems(withPipeInsteadOfSlash(branchPath));
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(branchPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId);
		givenCriteriaItemExists(taskCriteriaId, false, 1, taskCriteriaId);

		// when
		ResultActions resultActions = mockMvc.perform(post(requestUrl));
		List<CriteriaItemSignOff> criteriaItemSignOffs = toCriteriaItemSignOffs(getResponseBody(resultActions));

		// then
		assertEquals(2, criteriaItemSignOffs.size());
	}

	@Test
	void signOffAllCriteriaItems_ShouldNotChangeParent_WhenChildApproved() throws Exception {
		// given
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskB";
		String signOffAllCriteriaItems = signOffAllCriteriaItems(withPipeInsteadOfSlash(taskPath));
		String viewCriteriaItems = viewCriteriaItems(withPipeInsteadOfSlash(projectPath));
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(projectPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId);
		givenCriteriaItemExists(taskCriteriaId, false, 1, taskCriteriaId);

		mockMvc.perform(post(signOffAllCriteriaItems)); //Approve all Criteria Items for child

		// when
		ResultActions resultActions = mockMvc.perform(get(viewCriteriaItems));
		String responseBody = getResponseBody(resultActions);
		ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = toProjectAcceptanceCriteria(responseBody);
		Set<CriteriaItem> criteriaItems = projectAcceptanceCriteriaDTO.getCriteriaItems();

		// then
		for (CriteriaItem criteriaItem : criteriaItems) {
			assertFalse(criteriaItem.isComplete()); //None should be complete as child was actioned.
		}
	}

	@Test
	void signOffAllCriteriaItems_ShouldReturnExpectedNumberOfSignOffs_WhenSignOffPreviouslyExisted() throws Exception {
		// given
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskB";
		String signOffAllCriteriaItems = signOffAllCriteriaItems(withPipeInsteadOfSlash(taskPath));
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(projectPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, false, 0, projectCriteriaId);
		givenCriteriaItemExists(taskCriteriaId, true, 1, taskCriteriaId);
		givenCriteriaItemSignOffExists(taskPath, taskCriteriaId);

		// when
		ResultActions resultActions = mockMvc.perform(post(signOffAllCriteriaItems));
		List<CriteriaItemSignOff> criteriaItemSignOffs = toCriteriaItemSignOffs(getResponseBody(resultActions));

		// then
		assertEquals(2, criteriaItemSignOffs.size()); // All approved items should be returned (including those previously created).
	}

	@Test
	void rejectAllCriteriaItems_ShouldReturnExpectedResponse_WhenBranchDoesNotExist() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = rejectAllCriteriaItems(withPipeInsteadOfSlash(branchPath));

		givenBranchDoesNotExist();

		// when
		ResultActions resultActions = mockMvc.perform(delete(requestUrl));

		// then
		assertResponseStatus(resultActions, 403);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Branch does not exist."));
	}

	@Test
	void rejectAllCriteriaItems_ShouldReturnExpectedResponse_WhenBranchHasNoProjectAcceptance() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = rejectAllCriteriaItems(withPipeInsteadOfSlash(branchPath));

		givenBranchDoesExist();

		// when
		ResultActions resultActions = mockMvc.perform(delete(requestUrl));

		// then
		assertResponseStatus(resultActions, 404);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND.value(), "Cannot find Acceptance Criteria for " + branchPath + "."));
	}

	@Test
	void rejectAllCriteriaItems_ShouldReturnExpectedResponse_WhenProjectAcceptanceIsEmpty() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = rejectAllCriteriaItems(withPipeInsteadOfSlash(branchPath));

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(branchPath, 1);

		// when
		ResultActions resultActions = mockMvc.perform(delete(requestUrl));

		// then
		assertResponseStatus(resultActions, 409);
		assertResponseBody(resultActions, buildErrorResponse(HttpStatus.CONFLICT.value(), "Acceptance Criteria for " + branchPath + " has no Criteria Items configured."));
	}

	@Test
	void rejectAllCriteriaItems_ShouldReturnExpectedResponseStatus_WhenAllCriteriaItemsHaveBeenDeletedForProjectAcceptanceCriteria() throws Exception {
		// given
		String branchPath = "MAIN/projectA/taskB";
		String requestUrl = rejectAllCriteriaItems(withPipeInsteadOfSlash(branchPath));
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(branchPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId);
		givenCriteriaItemExists(taskCriteriaId, false, 1, taskCriteriaId);

		// when
		ResultActions resultActions = mockMvc.perform(delete(requestUrl));

		// then
		assertResponseStatus(resultActions, 204);
	}

	@Test
	void rejectAllCriteriaItems_ShouldNotChangeParent_WhenChildRejected() throws Exception {
		// given
		String projectPath = "MAIN/projectA";
		String taskPath = "MAIN/projectA/taskB";
		String approveParent = signOffAllCriteriaItems(withPipeInsteadOfSlash(projectPath));
		String approveChild = signOffAllCriteriaItems(withPipeInsteadOfSlash(taskPath));
		String rejectChild = rejectAllCriteriaItems(withPipeInsteadOfSlash(taskPath));
		String viewCriteriaItems = viewCriteriaItems(withPipeInsteadOfSlash(projectPath));
		String projectCriteriaId = "project-criteria-id";
		String taskCriteriaId = "task-criteria-id";

		givenBranchDoesExist();
		givenProjectAcceptanceCriteriaExists(projectPath, 1, projectCriteriaId, taskCriteriaId);
		givenCriteriaItemExists(projectCriteriaId, true, 0, projectCriteriaId);
		givenCriteriaItemExists(taskCriteriaId, false, 1, taskCriteriaId);

		mockMvc.perform(post(approveParent)); // Accept all Criteria Items for parent
		mockMvc.perform(post(approveChild)); // Accept all Criteria Items for child
		mockMvc.perform(delete(rejectChild)); // Reject all Criteria Items for child

		// when
		ResultActions resultActions = mockMvc.perform(get(viewCriteriaItems));
		String responseBody = getResponseBody(resultActions);
		ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = toProjectAcceptanceCriteria(responseBody);
		Set<CriteriaItem> criteriaItems = projectAcceptanceCriteriaDTO.getCriteriaItems();

		// then
		for (CriteriaItem criteriaItem : criteriaItems) {
			assertTrue(criteriaItem.isComplete()); // All should be complete as child was actioned.
		}
	}

	private String signOffAllCriteriaItems(String branchPath) {
		return "/admin/criteria/" + branchPath + "/accept";
	}

	private String viewCriteriaItems(String branchPath) {
		return "/acceptance/" + branchPath;
	}

	private String signOffCriteriaItem(String branchPath, String criteriaItemId) {
		return "/acceptance/" + branchPath + "/item/" + criteriaItemId + "/accept";
	}

	private String rejectAllCriteriaItems(String branchPath) {
		return "/admin/criteria/" + branchPath + "/accept";
	}

	private void givenProjectAcceptanceCriteriaExists(String branchPath, Integer projectIteration) {
		ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, projectIteration);
		projectAcceptanceCriteriaRepository.save(projectAcceptanceCriteria);
	}

	private void givenProjectAcceptanceCriteriaExists(String branchPath, Integer projectIteration, String projectCriteriaId, String taskCriteriaId) {
		ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, projectIteration);
		projectAcceptanceCriteria.setSelectedProjectCriteriaIds(Collections.singleton(projectCriteriaId));
		projectAcceptanceCriteria.setSelectedTaskCriteriaIds(Collections.singleton(taskCriteriaId));
		projectAcceptanceCriteriaRepository.save(projectAcceptanceCriteria);
	}

	private void givenCriteriaItemExists(String criteriaItemId, boolean manual, int order, String label) {
		CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
		criteriaItem.setManual(manual);
		criteriaItem.setRequiredRoles(Collections.singleton("ROLE_ADMIN_CONTROLLER_TEST"));
		criteriaItem.setOrder(order);
		criteriaItem.setLabel(label);

		criteriaItemRepository.save(criteriaItem);
	}

	private void givenCriteriaItemSignOffExists(String branchPath, String criteriaItemId) throws Exception {
		String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash(branchPath), criteriaItemId);
		givenUserDoesHavePermissionForBranch();
		givenBranchDoesExist(System.currentTimeMillis());
		givenAuthenticatedUser("AdminControllerTest");

		ResultActions resultActions = mockMvc.perform(post(requestUrl));

		assertResponseStatus(resultActions, 200);
	}

	private List<CriteriaItemSignOff> toCriteriaItemSignOffs(String response) throws JsonProcessingException {
		return OBJECT_MAPPER.readValue(response, new TypeReference<>() {
		});
	}

	private ProjectAcceptanceCriteriaDTO toProjectAcceptanceCriteria(String response) throws JsonProcessingException {
		return OBJECT_MAPPER.readValue(response, new TypeReference<>() {
		});
	}
}

package org.snomed.aag.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.TestConfig;
import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.rest.pojo.ProjectAcceptanceCriteriaDTO;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
class AcceptanceControllerTest extends AbstractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AcceptanceController acceptanceController;
    private AcceptanceCriteriaController acceptanceCriteriaController;
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        this.acceptanceController = new AcceptanceController(
                securityService,
                projectAcceptanceCriteriaService,
				acceptanceService
        );
        this.acceptanceCriteriaController = new AcceptanceCriteriaController(
                projectAcceptanceCriteriaService,
                projectAcceptanceCriteriaUpdateValidator
        );
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(acceptanceCriteriaController, acceptanceController)
                .setControllerAdvice(new RestControllerAdvice())
                .build();
    }

    @AfterEach
    public void tearDown() {
        this.acceptanceController = null;
        this.acceptanceCriteriaController = null;
        this.mockMvc = null;
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenCriteriaItemCannotBeFoundFromId() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND, "Criteria Item with id '" + criteriaItemId + "' not found."));
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenCriteriaItemCannotBeModified() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, false, 0, criteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Criteria Item cannot be changed manually."));
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenBranchDoesNotExist() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenBranchDoesNotExist();

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Branch does not exist."));
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenUserDoesNotHaveDesiredRole() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
		final String branchPath = "MAIN";
		String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        givenProjectAcceptanceCriteriaExists(branchPath, 1, criteriaItemId);
        givenCriteriaItemExists(criteriaItemId, true, 1, criteriaItemId);
        givenBranchDoesExist(1);
        givenUserDoesNotHavePermissionForBranch();

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "User does not have desired role."));
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenNoAcceptanceCriteriaFoundForBranch() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash("MAIN/projectA/taskA"), criteriaItemId);
        String expectedErrorMessage = String.format("Cannot find Acceptance Criteria for %s.", "MAIN/projectA/taskA");

        givenProjectAcceptanceCriteriaExists("MAIN", 1); //Project and Task do not have visibility of this.
        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(System.currentTimeMillis());

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, HttpStatus.NOT_FOUND.value());
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND.value(), expectedErrorMessage));
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenSigningOffItemOutwithAcceptanceCriteria() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash("MAIN/projectA/taskA"), criteriaItemId);
        String expectedErrorMessage = String.format("Branch %s does not have %s included in its Acceptance Criteria, and can, therefore, not be accepted/rejected.", "MAIN/projectA/taskA", criteriaItemId);

        givenProjectAcceptanceCriteriaExists("MAIN/projectA", 1);
        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(System.currentTimeMillis());

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, HttpStatus.BAD_REQUEST.value());
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.BAD_REQUEST.value(), expectedErrorMessage));
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenTryingToSignOffCriteriaItemTwice() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);
        String expectedErrorMessage = String.format("Criteria Item %s has already been signed off for branch %s and project iteration %d", criteriaItemId, "MAIN", 1);

        givenProjectAcceptanceCriteriaExists("MAIN", 1, criteriaItemId);
        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(System.currentTimeMillis());

        mockMvc.perform(post(requestUrl)); //first attempt

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl)); //second attempt

        //then
        assertResponseStatus(resultActions, HttpStatus.CONFLICT.value());
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.CONFLICT.value(), expectedErrorMessage));
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenSuccessfullySigningOffCriteriaItem() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);

        givenProjectAcceptanceCriteriaExists("MAIN", 1, criteriaItemId);
        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(System.currentTimeMillis());

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedBody_WhenSuccessfullySigningOffCriteriaItem() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);
        long timestamp = System.currentTimeMillis();
        String username = "AcceptanceControllerTest";

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenProjectAcceptanceCriteriaExists("MAIN", 1, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(timestamp);
        givenAuthenticatedUser(username);

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));
        CriteriaItemSignOff criteriaItemSignOff = OBJECT_MAPPER.readValue(getResponseBody(resultActions), CriteriaItemSignOff.class);

        //then
        assertEquals(criteriaItemId, criteriaItemSignOff.getCriteriaItemId());
        assertEquals("MAIN", criteriaItemSignOff.getBranch());
        assertEquals(timestamp, criteriaItemSignOff.getBranchHeadTimestamp());
        assertEquals(username, criteriaItemSignOff.getUserId());
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedBody_WhenSuccessfullySigningOffCriteriaItemOnProjectBranch() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash("MAIN/projectA"), criteriaItemId);
        long timestamp = System.currentTimeMillis();
        String username = "AcceptanceControllerTest";

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenProjectAcceptanceCriteriaExists("MAIN/projectA", 1, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(timestamp);
        givenAuthenticatedUser(username);

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));
        CriteriaItemSignOff criteriaItemSignOff = OBJECT_MAPPER.readValue(getResponseBody(resultActions), CriteriaItemSignOff.class);

        //then
        assertEquals(criteriaItemId, criteriaItemSignOff.getCriteriaItemId());
        assertEquals("MAIN/projectA", criteriaItemSignOff.getBranch());
        assertEquals(timestamp, criteriaItemSignOff.getBranchHeadTimestamp());
        assertEquals(username, criteriaItemSignOff.getUserId());
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedBody_WhenSuccessfullySigningOffCriteriaItemOnTaskBranch() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash("MAIN/projectA/taskB"), criteriaItemId);
        long timestamp = System.currentTimeMillis();
        String username = "AcceptanceControllerTest";

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenProjectAcceptanceCriteriaExists("MAIN/projectA/taskB", 1, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(timestamp);
        givenAuthenticatedUser(username);

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));
        CriteriaItemSignOff criteriaItemSignOff = OBJECT_MAPPER.readValue(getResponseBody(resultActions), CriteriaItemSignOff.class);

        //then
        assertEquals(criteriaItemId, criteriaItemSignOff.getCriteriaItemId());
        assertEquals("MAIN/projectA/taskB", criteriaItemSignOff.getBranch());
        assertEquals(timestamp, criteriaItemSignOff.getBranchHeadTimestamp());
        assertEquals(username, criteriaItemSignOff.getUserId());
    }

    @Test
    void signOffCriteriaItem_ShouldAddRecordToStore_WhenSuccessfullySigningOffCriteriaItem() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);
        long timestamp = System.currentTimeMillis();
        String username = "AcceptanceControllerTest";

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenProjectAcceptanceCriteriaExists("MAIN", 1, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(timestamp);
        givenAuthenticatedUser(username);

        ResultActions resultActions = mockMvc.perform(post(requestUrl));
        CriteriaItemSignOff criteriaItemSignOff = OBJECT_MAPPER.readValue(getResponseBody(resultActions), CriteriaItemSignOff.class);

        //when
        CriteriaItem result = criteriaItemService.findByIdOrThrow(criteriaItemSignOff.getCriteriaItemId());

        //then
        assertNotNull(result);
    }

    @Test
    void viewCriteriaItems_ShouldReturnExpectedResponse_WhenBranchNotFound() throws Exception {
        //given
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash("MAIN/projectA"));
        givenBranchDoesNotExist();

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
    }

    @Test
    void viewCriteriaItems_ShouldReturnExpectedResponse_WhenBranchHasNoCriteria() throws Exception {
        //given
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash("MAIN/projectA"));
        givenBranchDoesExist(System.currentTimeMillis());

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(404, "Cannot find Acceptance Criteria for MAIN/projectA."));
    }

    @Test
    void viewCriteriaItems_ShouldReturnExpectedResponse_WhenBranchAndParentBranchHasNoCriteria() throws Exception {
        //given
        String projectBranch = "MAIN/projectA";
        String taskBranch = projectBranch + "/taskA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(taskBranch));

        givenBranchDoesExist(System.currentTimeMillis());

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(404, "Cannot find Acceptance Criteria for MAIN/projectA/taskA."));
    }

    @Test
    void viewCriteriaItems_ShouldReturnExpectedStatus_WhenBranchHasCriteria() throws Exception {
        //given
        String branchPath = "MAIN/projectA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(branchPath));
        String projectCriteriaItemId = UUID.randomUUID().toString();
        String taskCriteriaItemId = UUID.randomUUID().toString();

        givenBranchDoesExist(System.currentTimeMillis());
        givenCriteriaItemExists(projectCriteriaItemId, false, 1, projectCriteriaItemId);
        givenCriteriaItemExists(taskCriteriaItemId, true, 0, taskCriteriaItemId);
        givenAcceptanceCriteriaExists(branchPath, 1, Collections.singleton(projectCriteriaItemId), Collections.singleton(taskCriteriaItemId));
        givenCriteriaItemSignOffExists(branchPath, taskCriteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        //then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    void viewCriteriaItems_ShouldReturnExpectedBody_WhenBranchHasCriteria() throws Exception {
        //given
        String branchPath = "MAIN/projectA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(branchPath));
        String projectCriteriaItemId = UUID.randomUUID().toString();
        String taskCriteriaItemId = UUID.randomUUID().toString();

        givenBranchDoesExist(System.currentTimeMillis());
        givenCriteriaItemExists(projectCriteriaItemId, false, 1, projectCriteriaItemId);
        givenCriteriaItemExists(taskCriteriaItemId, true, 0, taskCriteriaItemId);
        givenAcceptanceCriteriaExists(branchPath, 1, Collections.singleton(projectCriteriaItemId), Collections.singleton(taskCriteriaItemId));
        givenCriteriaItemSignOffExists(branchPath, taskCriteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        String responseBody = getResponseBody(resultActions);
        ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = OBJECT_MAPPER.readValue(responseBody, ProjectAcceptanceCriteriaDTO.class);

        //then
        assertEquals(branchPath, projectAcceptanceCriteriaDTO.getBranchPath());
        assertEquals(2, projectAcceptanceCriteriaDTO.getCriteriaItems().size());
    }

    @Test
    void viewCriteriaItems_ShouldReturnCriteriaItemsInCorrectOrder() throws Exception {
        //given
        String branchPath = "MAIN/projectA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(branchPath));
        String firstProjectCriteriaItemId = "A";
        String secondProjectCriteriaItemId = "B";
        String firstTaskCriteriaItemId = "C";
        String secondTaskCriteriaItemId = "D";
        String thirdTaskCriteriaItemId = "E";
        String fourthTaskCriteriaItemId = "F";
        Set<String> projectCriteriaItemIdentifiers = new HashSet<>();
        Collections.addAll(
                projectCriteriaItemIdentifiers,
                firstProjectCriteriaItemId,
                secondProjectCriteriaItemId
        );
        Set<String> taskCriteriaItemIdentifiers = new HashSet<>();
        Collections.addAll(
                taskCriteriaItemIdentifiers,
                firstTaskCriteriaItemId,
                secondTaskCriteriaItemId,
                thirdTaskCriteriaItemId,
                fourthTaskCriteriaItemId
        );

        givenBranchDoesExist(System.currentTimeMillis());
        givenCriteriaItemExists(firstProjectCriteriaItemId, false, 4, firstProjectCriteriaItemId);
        givenCriteriaItemExists(secondProjectCriteriaItemId, false, 4, secondProjectCriteriaItemId);
        givenCriteriaItemExists(firstTaskCriteriaItemId, true, 0, firstTaskCriteriaItemId);
        givenCriteriaItemExists(secondTaskCriteriaItemId, true, 1, secondTaskCriteriaItemId);
        givenCriteriaItemExists(thirdTaskCriteriaItemId, true, 2, thirdTaskCriteriaItemId);
        givenCriteriaItemExists(fourthTaskCriteriaItemId, true, 3, fourthTaskCriteriaItemId);
        givenAcceptanceCriteriaExists(branchPath, 1, projectCriteriaItemIdentifiers, taskCriteriaItemIdentifiers);
        givenCriteriaItemSignOffExists(branchPath, firstTaskCriteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        String responseBody = getResponseBody(resultActions);
        ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = OBJECT_MAPPER.readValue(responseBody, ProjectAcceptanceCriteriaDTO.class);
        List<CriteriaItem> criteriaItems = new ArrayList<>(projectAcceptanceCriteriaDTO.getCriteriaItems());

        //then
        assertEquals(firstTaskCriteriaItemId, criteriaItems.get(0).getId());
        assertEquals(secondTaskCriteriaItemId, criteriaItems.get(1).getId());
        assertEquals(thirdTaskCriteriaItemId, criteriaItems.get(2).getId());
        assertEquals(fourthTaskCriteriaItemId, criteriaItems.get(3).getId());
        assertEquals(firstProjectCriteriaItemId, criteriaItems.get(4).getId());
        assertEquals(secondProjectCriteriaItemId, criteriaItems.get(5).getId());
    }

    @Test
    void viewCriteriaItems_ShouldIncludeGloballyRequiredCriteriaItems() throws Exception {
        //given
        String branchPath = "MAIN/projectA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(branchPath));
        String globalProjectLevelCriteriaItemId = UUID.randomUUID().toString();
        String globalTaskLevelCriteriaItemId = UUID.randomUUID().toString();
        String projectCriteriaItemId = UUID.randomUUID().toString();
        String taskCriteriaItemId = UUID.randomUUID().toString();

        givenBranchDoesExist(System.currentTimeMillis());
        givenGloballyRequiredProjectLevelCriteriaItemExists(globalProjectLevelCriteriaItemId, true, 0);
        givenGloballyRequiredTaskLevelCriteriaItemExists(globalTaskLevelCriteriaItemId, true, 1);
        givenCriteriaItemExists(projectCriteriaItemId, false, 2, projectCriteriaItemId);
        givenCriteriaItemExists(taskCriteriaItemId, true, 3, taskCriteriaItemId);
        givenAcceptanceCriteriaExists(branchPath, 1, Collections.singleton(projectCriteriaItemId), Collections.singleton(taskCriteriaItemId));
        givenCriteriaItemSignOffExists(branchPath, taskCriteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        String responseBody = getResponseBody(resultActions);
        ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = OBJECT_MAPPER.readValue(responseBody, ProjectAcceptanceCriteriaDTO.class);
        List<CriteriaItem> criteriaItems = new ArrayList<>(projectAcceptanceCriteriaDTO.getCriteriaItems());

        //then
        assertEquals(branchPath, projectAcceptanceCriteriaDTO.getBranchPath());
        assertEquals(4, criteriaItems.size());
        assertEquals(globalProjectLevelCriteriaItemId, criteriaItems.get(0).getId());
        assertEquals(globalTaskLevelCriteriaItemId, criteriaItems.get(1).getId());
        assertEquals(projectCriteriaItemId, criteriaItems.get(2).getId());
        assertEquals(taskCriteriaItemId, criteriaItems.get(3).getId());
    }

    @Test
    void viewCriteriaItems_ShouldReturnCompleteCriteriaItem_WhenCriteriaItemHasBeenSignedOff() throws Exception {
        //given
        String branchPath = "MAIN/projectA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(branchPath));
        String globalCriteriaItemId = UUID.randomUUID().toString();
        String projectCriteriaItemId = UUID.randomUUID().toString();
        String taskCriteriaItemId = UUID.randomUUID().toString();

        givenBranchDoesExist(System.currentTimeMillis());
        givenGloballyRequiredProjectLevelCriteriaItemExists(globalCriteriaItemId, true, 0);
        givenCriteriaItemExists(projectCriteriaItemId, false, 1, projectCriteriaItemId);
        givenCriteriaItemExists(taskCriteriaItemId, true, 2, taskCriteriaItemId);
        givenAcceptanceCriteriaExists(branchPath, 1, Collections.singleton(projectCriteriaItemId), Collections.singleton(taskCriteriaItemId));
        givenCriteriaItemSignOffExists(branchPath, taskCriteriaItemId);
        givenAcceptanceCriteriaExists("MAIN", 1, Collections.singleton(projectCriteriaItemId), Collections.singleton(taskCriteriaItemId));
        givenCriteriaItemSignOffExists("MAIN", taskCriteriaItemId); //shouldn't be return from ES query

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        String responseBody = getResponseBody(resultActions);
        ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = OBJECT_MAPPER.readValue(responseBody, ProjectAcceptanceCriteriaDTO.class);
        List<CriteriaItem> criteriaItems = new ArrayList<>(projectAcceptanceCriteriaDTO.getCriteriaItems());

        //then
        assertEquals(3, criteriaItems.size());
        assertTrue(criteriaItems.get(2).isComplete());
    }

    @Test
    void viewCriteriaItems_ShouldReturnExpectedCriteriaItems_WhenMultipleCriteriaItemsHaveSameLabel() throws Exception {
        //given
        String branchPath = "MAIN/projectA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(branchPath));
        String globalCriteriaItemId = "A";
        String projectCriteriaItemId = "B";
        String taskCriteriaItemId = "C";

        givenBranchDoesExist(System.currentTimeMillis());
        givenGloballyRequiredProjectLevelCriteriaItemExists(globalCriteriaItemId, true, 0);
        givenCriteriaItemExists(projectCriteriaItemId, false, 2, "duplicate-label");
        givenCriteriaItemExists(taskCriteriaItemId, true, 2, "duplicate-label");
        givenAcceptanceCriteriaExists(branchPath, 1, Collections.singleton(projectCriteriaItemId), Collections.singleton(taskCriteriaItemId));
        givenCriteriaItemSignOffExists(branchPath, taskCriteriaItemId);
        givenAcceptanceCriteriaExists("MAIN", 1, Collections.singleton(projectCriteriaItemId), Collections.singleton(taskCriteriaItemId));
        givenCriteriaItemSignOffExists("MAIN", taskCriteriaItemId); //shouldn't be return from ES query

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        String responseBody = getResponseBody(resultActions);
        ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = OBJECT_MAPPER.readValue(responseBody, ProjectAcceptanceCriteriaDTO.class);
        List<CriteriaItem> criteriaItems = new ArrayList<>(projectAcceptanceCriteriaDTO.getCriteriaItems());

        //then
        assertEquals(3, criteriaItems.size());
        assertEquals(globalCriteriaItemId, criteriaItems.get(0).getId());
        assertEquals(projectCriteriaItemId, criteriaItems.get(1).getId());
        assertEquals(taskCriteriaItemId, criteriaItems.get(2).getId());
    }

    @Test
    void viewCriteriaItems_ShouldReturnCriteriaItemsFromParentBranch_WhenGivenBranchHasNoAcceptanceCriteria() throws Exception {
        //given
        String projectBranch = "MAIN/projectA";
        String taskBranch = projectBranch + "/taskA";
        String globalCriteriaItemId = UUID.randomUUID().toString();
        String projectCriteriaItemId = UUID.randomUUID().toString();
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(taskBranch));

        givenBranchDoesExist(System.currentTimeMillis());
        givenGloballyRequiredProjectLevelCriteriaItemExists(globalCriteriaItemId, true, 0);
        givenCriteriaItemExists(projectCriteriaItemId, false, 1, projectCriteriaItemId);
        givenAcceptanceCriteriaExists(projectBranch, 1, Collections.singleton(projectCriteriaItemId), Collections.emptySet());

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        String responseBody = getResponseBody(resultActions);
        ProjectAcceptanceCriteriaDTO projectAcceptanceCriteriaDTO = OBJECT_MAPPER.readValue(responseBody, ProjectAcceptanceCriteriaDTO.class);
        List<CriteriaItem> criteriaItems = new ArrayList<>(projectAcceptanceCriteriaDTO.getCriteriaItems());

        //then
        assertEquals(2, criteriaItems.size());
    }

    @Test
    void viewCriteriaItems_ShouldReturnExpectedStatus_WhenGivenBranchHasNoAcceptanceCriteriaButParentDoes() throws Exception {
        //given
        String projectBranch = "MAIN/projectA";
        String taskBranch = projectBranch + "/taskA";
        String globalCriteriaItemId = UUID.randomUUID().toString();
        String projectCriteriaItemId = UUID.randomUUID().toString();
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(taskBranch));

        givenBranchDoesExist(System.currentTimeMillis());
        givenGloballyRequiredProjectLevelCriteriaItemExists(globalCriteriaItemId, true, 0);
        givenCriteriaItemExists(projectCriteriaItemId, false, 1, projectCriteriaItemId);
        givenAcceptanceCriteriaExists(projectBranch, 1, Collections.singleton(projectCriteriaItemId), Collections.emptySet());

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        //then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    void viewCriteriaItems_ShouldReturnItemMarkedComplete_WhenGivenBranchHasAcceptedItem() throws Exception {
        //given
        String projectBranch = "MAIN/projectA";
        String taskBranch = projectBranch + "/taskA";
        String globalCriteriaItemId = UUID.randomUUID().toString();
        String projectCriteriaItemId = UUID.randomUUID().toString();
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(taskBranch));

        givenBranchDoesExist(System.currentTimeMillis());
        givenGloballyRequiredProjectLevelCriteriaItemExists(globalCriteriaItemId, true, 0);
        givenCriteriaItemExists(projectCriteriaItemId, true, 1, projectCriteriaItemId);
        givenAcceptanceCriteriaExists(projectBranch, 1, Collections.singleton(projectCriteriaItemId), Collections.emptySet());
        givenCriteriaItemSignOffExists(taskBranch, projectCriteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        Set<CriteriaItem> criteriaItems = toProjectAcceptCriteria(getResponseBody(resultActions)).getCriteriaItems();

        //then
        boolean itemFound = false;
        for (CriteriaItem criteriaItem : criteriaItems) {
            if (criteriaItem.getId().equals(projectCriteriaItemId)) {
                assertTrue(criteriaItem.isComplete());
                itemFound = true;
                break;
            }
        }

        assertTrue(itemFound);
    }

    @Test
    void viewCriteriaItems_ShouldReturnExpectedItems_WhenBranchHasAuthorFlags() throws Exception {
        //given
        String projectBranch = "MAIN/projectA";
        String taskBranch = projectBranch + "/taskA";
        String globalCriteriaItemId = UUID.randomUUID().toString();
        String projectCriteriaItemId = UUID.randomUUID().toString();
        String taskCriteriaItemId1 = UUID.randomUUID().toString();
        String taskCriteriaItemId2 = UUID.randomUUID().toString();
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(taskBranch));

        givenBranchDoesExist(System.currentTimeMillis(), buildMetadataWithAuthorFlag("complex", true));
        givenGloballyRequiredProjectLevelCriteriaItemExists(globalCriteriaItemId, true, 0);
        givenCriteriaItemExists(projectCriteriaItemId, true, 1, projectCriteriaItemId);
        givenCriteriaItemExists(taskCriteriaItemId1, true, 1, taskCriteriaItemId1);
        givenCriteriaItemExists(taskCriteriaItemId2, true, 1, taskCriteriaItemId2, "complex");
        givenAcceptanceCriteriaExists(projectBranch, 1, Collections.singleton(projectCriteriaItemId), Set.of(taskCriteriaItemId1)); // Created without complex task

        //when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        Set<CriteriaItem> criteriaItems = toProjectAcceptCriteria(getResponseBody(resultActions)).getCriteriaItems();

        //then
        assertEquals(4, criteriaItems.size()); // Including items matching authoring flags
    }

    @Test
    void viewCriteriaItems_ShouldReturnPACWithExpectedCriteria_WhenGivenVariousConfigurations() throws Exception {
        /*
         * Scenario             Project has Criteria            Criteria is 'mandatory'         Criteria has 'enabledByFlag'   Branch Flags    Enabled
         * A                    Y                               -                               -                               -               Y
         * B                    -                               Y                               -                               -               Y
         * C                    Y                               -                               complex                         complex=true    Y
         * D                    -                               Y                               complex                         complex=true    Y
         * E                    -                               -                               complex                         complex=true    Y
         * F                    -                               Y                               complex                         -               N
         * G                    Y                               -                               thing                           complex=true    N
         * H                    -                               Y                               complex                         complex=false   N
         * I                    Y                               N                               complex                         complex=false   Y
         * */
        assertViewingPACRequirement("A", true, false, null, null, true);
        assertViewingPACRequirement("B", false, true, null, null, true);
        assertViewingPACRequirement("C", true, false, "complex", Pair.of("complex", true), true);
        assertViewingPACRequirement("D", false, true, "complex", Pair.of("complex", true), true);
        assertViewingPACRequirement("E", false, false, "complex", Pair.of("complex", true), true);
        assertViewingPACRequirement("F", false, true, "complex", null, false);
        assertViewingPACRequirement("G", true, false, "thing", Pair.of("complex", true), false);
        assertViewingPACRequirement("H", false, true, "complex", Pair.of("complex", false), false);
        assertViewingPACRequirement("I", true, false, "complex", Pair.of("complex", false), true);
    }

    @Test
    void viewCriteriaItems_ShouldReturnPACWithExpectedCriteria_WhenTaskBranchChangedViaBatch() throws Exception {
        // given
        String projectBranch = "MAIN/projectA";
        String taskBranch = projectBranch + "/taskA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(taskBranch));

        givenBranchDoesExist(System.currentTimeMillis(), buildMetadataWithAuthorFlag(Constants.AUTHOR_FLAG_BATCH_CHANGE, true));
        givenCriteriaItemExists(CriteriaItem.PROJECT_VALIDATION_CLEAN, true, 1, CriteriaItem.PROJECT_VALIDATION_CLEAN);
        givenCriteriaItemExists(CriteriaItem.TASK_VALIDATION_CLEAN, true, 1, CriteriaItem.TASK_VALIDATION_CLEAN, false);
        givenAcceptanceCriteriaExists(projectBranch, 1, Collections.singleton(CriteriaItem.PROJECT_VALIDATION_CLEAN), Collections.emptySet());

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        Set<CriteriaItem> criteriaItems = toProjectAcceptCriteria(getResponseBody(resultActions)).getCriteriaItems();
        CriteriaItem taskValidationClean = getCriteriaItem(CriteriaItem.TASK_VALIDATION_CLEAN, criteriaItems);

        // then
        assertEquals(2, criteriaItems.size()); // One configured; one inherited as task is batch.
        assertNotNull(taskValidationClean);
        assertTrue(taskValidationClean.isMandatory()); // Overwritten as configured false
    }

    @Test
    void viewCriteriaItems_ShouldReturnPACWithExpectedCriteria_WhenTaskBranchNotChangedViaBatch() throws Exception {
        // given
        String projectBranch = "MAIN/projectA";
        String taskBranch = projectBranch + "/taskA";
        String requestUrl = viewCriteriaItems(withPipeInsteadOfSlash(taskBranch));

        givenBranchDoesExist(System.currentTimeMillis(), buildMetadataWithAuthorFlag(Constants.AUTHOR_FLAG_BATCH_CHANGE, false));
        givenCriteriaItemExists(CriteriaItem.PROJECT_VALIDATION_CLEAN, true, 1, CriteriaItem.PROJECT_VALIDATION_CLEAN);
        givenCriteriaItemExists(CriteriaItem.TASK_VALIDATION_CLEAN, true, 1, CriteriaItem.TASK_VALIDATION_CLEAN, false);
        givenAcceptanceCriteriaExists(projectBranch, 1, Collections.singleton(CriteriaItem.PROJECT_VALIDATION_CLEAN), Collections.emptySet());

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        Set<CriteriaItem> criteriaItems = toProjectAcceptCriteria(getResponseBody(resultActions)).getCriteriaItems();
        CriteriaItem projectValidationClean = getCriteriaItem(CriteriaItem.PROJECT_VALIDATION_CLEAN, criteriaItems);

        // then
        assertEquals(1, criteriaItems.size()); // Only one configured
        assertNotNull(projectValidationClean);
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedStatus_WhenCriteriaItemCannotBeFoundFromId() throws Exception {
        //given
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND, "Criteria Item with id '" + criteriaItemId + "' not found."));
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenCriteriaItemCannotBeModified() throws Exception {
        //given
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, false, 0, criteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Criteria Item cannot be changed manually."));
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenBranchDoesNotExist() throws Exception {
        //given
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenBranchDoesNotExist();

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Branch does not exist."));
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenUserDoesNotHaveDesiredRole() throws Exception {
        //given
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        givenProjectAcceptanceCriteriaExists(branchPath, 1, criteriaItemId);
        givenCriteriaItemExists(criteriaItemId, true, 1, criteriaItemId);
        givenBranchDoesExist(1);
        givenUserDoesNotHavePermissionForBranch();

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "User does not have desired role."));
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenBranchHasNoAcceptanceCriteria() throws Exception {
        //given
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);
        String expectedErrorMessage = String.format("Cannot find Acceptance Criteria for %s.", branchPath);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(404, expectedErrorMessage));
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenBranchAndParentBranchHaveNoAcceptanceCriteria() throws Exception {
        //given
        String projectBranch = "MAIN/projectA";
        String taskBranch = projectBranch + "/" + "taskB";
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash(taskBranch), criteriaItemId);
        String expectedErrorMessage = String.format("Cannot find Acceptance Criteria for %s.", taskBranch);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(404, expectedErrorMessage));
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenThereIsNothingToDelete() throws Exception {
        //given
        givenBranchDoesExist();
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenProjectAcceptanceCriteriaExists(branchPath, 67, criteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(404, String.format("Cannot delete %s for branch %s and project iteration %d", criteriaItemId, branchPath, 67)));
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenSuccessfullyRejectingItem() throws Exception {
        //given
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenProjectAcceptanceCriteriaExists(branchPath, 67, criteriaItemId);
        givenCriteriaItemSignOffExists(branchPath, criteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 204);
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenSuccessfullyRejectingItemFromParent() throws Exception {
        //given
        String projectBranch = UUID.randomUUID().toString();
        String taskBranch = projectBranch + "/" + UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash(taskBranch), criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenProjectAcceptanceCriteriaExists(projectBranch, 67, criteriaItemId);
        givenCriteriaItemSignOffExists(taskBranch, criteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 204);
    }

    @Test
    void rejectCriteriaItem_ShouldRemoveEntryFromStore_WhenSuccessfullyRejectingItem() throws Exception {
        //given
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = givenProjectAcceptanceCriteriaExists(branchPath, 89, criteriaItemId);
        givenCriteriaItemSignOffExists(branchPath, criteriaItemId);

        mockMvc.perform(delete(requestUrl));

        //when
        Optional<CriteriaItemSignOff> result = criteriaItemSignOffService.findByCriteriaItemIdAndBranchPathAndProjectIteration(criteriaItemId, branchPath, 0, projectAcceptanceCriteria);

        //then
        assertFalse(result.isPresent());
    }

    private String signOffCriteriaItem(String branchPath, String criteriaItemId) {
        return "/acceptance/" + branchPath + "/item/" + criteriaItemId + "/accept";
    }

    private String viewCriteriaItems(String branchPath) {
        return "/acceptance/" + branchPath;
    }

    private String createProjectCriteria() {
        return "/criteria";
    }

    private void givenCriteriaItemExists(String criteriaItemId, boolean manual, int order, String label) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRoles(Collections.singleton("ROLE_ACCEPTANCE_CONTROLLER_TEST"));
        criteriaItem.setOrder(order);
        criteriaItem.setLabel(label);

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenCriteriaItemExists(String criteriaItemId, boolean manual, int order, String label, boolean mandatory) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRoles(Collections.singleton("ROLE_ACCEPTANCE_CONTROLLER_TEST"));
        criteriaItem.setOrder(order);
        criteriaItem.setLabel(label);
        criteriaItem.setMandatory(mandatory);

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenCriteriaItemExists(CriteriaItem criteriaItem) {
        criteriaItemRepository.save(criteriaItem);
    }

    private void givenCriteriaItemExists(String criteriaItemId, boolean manual, int order, String label, String flag) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRoles(Collections.singleton("ROLE_ACCEPTANCE_CONTROLLER_TEST"));
        criteriaItem.setOrder(order);
        criteriaItem.setLabel(label);
        criteriaItem.setEnabledByFlag(Collections.singleton(flag));

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenProjectAcceptanceCriteriaExists(String branchPath, Integer projectIteration) {
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, projectIteration);
        projectAcceptanceCriteriaRepository.save(projectAcceptanceCriteria);
    }

    private ProjectAcceptanceCriteria givenProjectAcceptanceCriteriaExists(String branchPath, Integer projectIteration, String projectCriteria) {
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, projectIteration);
        projectAcceptanceCriteria.setSelectedProjectCriteriaIds(Collections.singleton(projectCriteria));
        return projectAcceptanceCriteriaRepository.save(projectAcceptanceCriteria);
    }

    private void givenProjectAcceptanceCriteriaExists(ProjectAcceptanceCriteria projectAcceptanceCriteria) throws Exception {
        String requestUrl = createProjectCriteria();
        ResultActions resultActions = mockMvc.perform(
                post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
        );

        assertResponseStatus(resultActions, 201);
    }

    private void givenGloballyRequiredProjectLevelCriteriaItemExists(String criteriaItemId, boolean manual, int order) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRoles(Collections.singleton("ROLE_ACCEPTANCE_CONTROLLER_TEST"));
        criteriaItem.setOrder(order);
        criteriaItem.setMandatory(true);
        criteriaItem.setAuthoringLevel(AuthoringLevel.PROJECT);

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenGloballyRequiredTaskLevelCriteriaItemExists(String criteriaItemId, boolean manual, int order) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRoles(Collections.singleton("ROLE_ACCEPTANCE_CONTROLLER_TEST"));
        criteriaItem.setOrder(order);
        criteriaItem.setMandatory(true);
        criteriaItem.setAuthoringLevel(AuthoringLevel.TASK);

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenAcceptanceCriteriaExists(String branchPath, Integer projectIteration, Set<String> selectedProjectCriteriaIds,
                                               Set<String> selectedTaskCriteriaIds) throws Exception {
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, projectIteration);
        projectAcceptanceCriteria.setSelectedProjectCriteriaIds(selectedProjectCriteriaIds);
        projectAcceptanceCriteria.setSelectedTaskCriteriaIds(selectedTaskCriteriaIds);

        String requestUrl = createProjectCriteria();
        ResultActions resultActions = mockMvc.perform(
                post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
        );

        assertResponseStatus(resultActions, 201);
    }

    private void givenCriteriaItemSignOffExists(String branchPath, String criteriaItemId) throws Exception {
        String requestUrl = signOffCriteriaItem(withPipeInsteadOfSlash(branchPath), criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(System.currentTimeMillis());
        givenAuthenticatedUser("AcceptanceControllerTest");

        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        assertResponseStatus(resultActions, 200);
    }

    private ProjectAcceptanceCriteriaDTO toProjectAcceptCriteria(String response) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(response, new TypeReference<>() {
        });
    }

    private Map<String, Object> buildMetadataWithAuthorFlag(String key, Object value) {
        Map<String, Object> authorFlags = new LinkedHashMap<>();
        authorFlags.put(key, value);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(Constants.AUTHOR_FLAG, authorFlags);

        return metadata;
    }

    private void assertViewingPACRequirement(String scenario,
                                             boolean projectHasCriteria,
                                             boolean criteriaIsMandatory,
                                             String enabledByFlag,
                                             Pair<String, Boolean> branchFlagKeyValue,
                                             boolean shouldBeInProject) throws Exception {
        // Delete previous state
        criteriaItemRepository.deleteAll();
        projectAcceptanceCriteriaRepository.deleteAll();

        // Create CriteriaItem & ProjectAcceptanceCriteria
        CriteriaItem projectCriteriaItem = new CriteriaItem(scenario, AuthoringLevel.PROJECT, criteriaIsMandatory, true, true);
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria();

        // Test specific. Add CriteriaItem to ProjectAcceptanceCriteria for certain scenarios.
        if (projectHasCriteria) {
            projectAcceptanceCriteria.addToSelectedProjectCriteria(projectCriteriaItem);
        }

        // Test specific. Add enabledByFlag to CriteriaItem for certain scenarios.
        if (enabledByFlag != null) {
            projectCriteriaItem.setEnabledByFlag(Set.of(enabledByFlag));
        }

        // Set default properties on ProjectAcceptanceCriteria
        projectAcceptanceCriteria.setBranchPath(scenario);
        projectAcceptanceCriteria.setProjectIteration(1);

        // Write CriteriaItem & ProjectAcceptanceCriteria to store
        givenCriteriaItemExists(projectCriteriaItem);
        givenProjectAcceptanceCriteriaExists(projectAcceptanceCriteria);

        // Test specific. Add metadata to Branch for certain scenarios.
        if (branchFlagKeyValue != null) {
            givenBranchDoesExist(scenario, buildMetadataWithAuthorFlag(branchFlagKeyValue.getFirst(), branchFlagKeyValue.getSecond()));
        } else {
            givenBranchDoesExist(scenario);
        }

        // Request ProjectAcceptanceCriteria
        ResultActions resultActions = mockMvc.perform(get(viewCriteriaItems(withPipeInsteadOfSlash(scenario))));
        Set<CriteriaItem> criteriaItems = toProjectAcceptCriteria(getResponseBody(resultActions)).getCriteriaItems();

        // Test specific. CriteriaItem should only be present in ProjectAcceptanceCriteria for certain scenarios.
        if (shouldBeInProject) {
            assertEquals(1, criteriaItems.size(), String.format("Test case for scenario %s failed.", scenario));
        } else {
            assertEquals(0, criteriaItems.size(), String.format("Test case for scenario %s failed.", scenario));
        }
    }

    private CriteriaItem getCriteriaItem(String criteriaItemIdentifier, Set<CriteriaItem> criteriaItems) {
        for (CriteriaItem criteriaItem : criteriaItems) {
            if (criteriaItemIdentifier.equals(criteriaItem.getId())) {
                return criteriaItem;
            }
        }

        return null;
    }
}

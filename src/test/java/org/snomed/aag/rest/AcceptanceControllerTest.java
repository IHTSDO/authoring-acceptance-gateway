package org.snomed.aag.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.TestConfig;
import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.rest.pojo.ProjectAcceptanceCriteriaDTO;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
class AcceptanceControllerTest extends AbstractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AcceptanceController acceptanceController;
    private AcceptanceCriteriaController acceptanceCriteriaController;
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        this.acceptanceController = new AcceptanceController(
                criteriaItemService,
                branchService,
                criteriaItemSignOffService,
                securityService,
                projectAcceptanceCriteriaService
        );
        this.acceptanceCriteriaController = new AcceptanceCriteriaController(
                projectAcceptanceCriteriaService
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
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesNotHavePermissionForBranch();

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "User does not have desired role."));
    }

    @Test
    void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenTryingToSignOffCriteriaItemTwice() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem("MAIN", criteriaItemId);
        String expectedErrorMessage = String.format("Criteria Item %s has already been signed off for branch %s and project iteration %d", criteriaItemId, "MAIN", 1);

        givenProjectAcceptanceCriteriaExists("MAIN", 1);
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

        givenProjectAcceptanceCriteriaExists("MAIN", 1);
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
        givenProjectAcceptanceCriteriaExists("MAIN", 1);
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
        givenProjectAcceptanceCriteriaExists("MAIN/projectA", 1);
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
        givenProjectAcceptanceCriteriaExists("MAIN/projectA/taskB", 1);
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
        givenProjectAcceptanceCriteriaExists("MAIN", 1);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(timestamp);
        givenAuthenticatedUser(username);

        ResultActions resultActions = mockMvc.perform(post(requestUrl));
        CriteriaItemSignOff criteriaItemSignOff = OBJECT_MAPPER.readValue(getResponseBody(resultActions), CriteriaItemSignOff.class);

        //when
        CriteriaItem result = criteriaItemService.findOrThrow(criteriaItemSignOff.getCriteriaItemId());

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
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND, "No project acceptance criteria found for this branch path."));
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
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND, "No project acceptance criteria found for this branch path."));
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
        assertEquals(branchPath, projectAcceptanceCriteriaDTO.getProjectKey());
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
        assertEquals(branchPath, projectAcceptanceCriteriaDTO.getProjectKey());
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

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
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

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();

        //when
        ResultActions resultActions = mockMvc.perform(delete(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(404, String.format("Branch %s does not have any acceptance criteria.", branchPath)));
    }

    @Test
    void rejectCriteriaItem_ShouldReturnExpectedResponse_WhenThereIsNothingToDelete() throws Exception {
        //given
        String branchPath = UUID.randomUUID().toString();
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(branchPath, criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true, 0, criteriaItemId);
        givenUserDoesHavePermissionForBranch();
        givenProjectAcceptanceCriteriaExists(branchPath, 67);

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
        givenProjectAcceptanceCriteriaExists(branchPath, 67);
        givenCriteriaItemSignOffExists(branchPath, criteriaItemId);

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
        givenProjectAcceptanceCriteriaExists(branchPath, 89);
        givenCriteriaItemSignOffExists(branchPath, criteriaItemId);

        mockMvc.perform(delete(requestUrl));

        //when
        Optional<CriteriaItemSignOff> result = criteriaItemSignOffService.findBy(criteriaItemId, branchPath, 89);

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

    private String buildErrorResponse(HttpStatus error, String message) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);

        return OBJECT_MAPPER.writeValueAsString(response);
    }

    private String buildErrorResponse(int error, String message) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);

        return OBJECT_MAPPER.writeValueAsString(response);
    }

    private void assertResponseStatus(ResultActions result, int expectedResponseStatus) throws Exception {
        result.andExpect(status().is(expectedResponseStatus));
    }

    private void assertResponseBody(ResultActions result, String expectedResponseBody) throws Exception {
        result.andExpect(content().string(expectedResponseBody));
    }

    private void givenCriteriaItemExists(String criteriaItemId, boolean manual, int order, String label) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRole("ROLE_ACCEPTANCE_CONTROLLER_TEST");
        criteriaItem.setOrder(order);
        criteriaItem.setLabel(label);

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenProjectAcceptanceCriteriaExists(String branchPath, Integer projectIteration) {
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, projectIteration);

        projectAcceptanceCriteriaRepository.save(projectAcceptanceCriteria);
    }

    private void givenGloballyRequiredProjectLevelCriteriaItemExists(String criteriaItemId, boolean manual, int order) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRole("ROLE_ACCEPTANCE_CONTROLLER_TEST");
        criteriaItem.setOrder(order);
        criteriaItem.setMandatory(true);
        criteriaItem.setAuthoringLevel(AuthoringLevel.PROJECT);

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenGloballyRequiredTaskLevelCriteriaItemExists(String criteriaItemId, boolean manual, int order) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRole("ROLE_ACCEPTANCE_CONTROLLER_TEST");
        criteriaItem.setOrder(order);
        criteriaItem.setMandatory(true);
        criteriaItem.setAuthoringLevel(AuthoringLevel.TASK);

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenBranchDoesNotExist() throws RestClientException {
        when(securityService.currentUserHasRoleOnBranch(any(), any())).thenThrow(new AccessDeniedException("Branch does not exist."));
        when(securityService.getBranchOrThrow(any())).thenThrow(new AccessDeniedException("Branch does not exist."));
    }

    private void givenUserDoesNotHavePermissionForBranch() throws RestClientException {
        when(securityService.currentUserHasRoleOnBranch(any(), any())).thenReturn(false);
    }

    private void givenUserDoesHavePermissionForBranch() throws RestClientException {
        when(securityService.currentUserHasRoleOnBranch(any(), any())).thenReturn(true);
    }

    private void givenBranchDoesExist(long timestamp) throws RestClientException {
        Branch branch = new Branch();
        branch.setHeadTimestamp(timestamp);

        when(securityService.getBranchOrThrow(any())).thenReturn(branch);
    }

    private void givenAuthenticatedUser(String username) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(username);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
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

    private String getResponseBody(ResultActions resultActions) throws UnsupportedEncodingException {
        return resultActions.andReturn().getResponse().getContentAsString();
    }

    /*
     * MockMvc doesn't follow re-directs. Instead of sending a request
     * where a branch has a slash (which relies on BranchPathUriRewriteFilter),
     * send a request with the branch already decoded.
     * */
    private String withPipeInsteadOfSlash(String branch) {
        return branch.replaceAll("/", "|");
    }

    private String asJson(Object input) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(input);
    }
}

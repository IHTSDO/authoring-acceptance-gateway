package org.snomed.aag.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.TestConfig;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
class AcceptanceCriteriaControllerTest extends AbstractTest {
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
    void createProjectCriteria_ShouldReturnExpectedResponseStatus_WhenProjectAcceptanceCriteriaSuccessfullySaved() throws Exception {
        // given
        String requestUrl = createProjectCriteria();
        String branchPath = UUID.randomUUID().toString(); // random so unique across tests
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, 1);

        givenUserDoesHavePermissionForBranch();

        // when
        ResultActions resultActions = mockMvc
                .perform(post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertResponseStatus(resultActions, 201);
    }

    @Test
    void createProjectCriteria_ShouldWriteToStore_WhenProjectAcceptanceCriteriaSuccessfullySaved() throws Exception {
        // given
        String requestUrl = createProjectCriteria();
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, 23);

        givenUserDoesHavePermissionForBranch();

        mockMvc.perform(post(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(projectAcceptanceCriteria))
        );

        // when
        ProjectAcceptanceCriteria result = projectAcceptanceCriteriaService.findByBranchPathOrThrow(branchPath);

        // then
        assertNotNull(result);
        assertEquals(23, result.getProjectIteration());
    }

    @Test
    void createProjectCriteria_ShouldReturnExpectedResponseStatus_WhenMissingProjectIteration() throws Exception {
        // given
        String requestUrl = createProjectCriteria();
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath);

        givenUserDoesHavePermissionForBranch();

        // when
        ResultActions resultActions = mockMvc
                .perform(post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertResponseStatus(resultActions, 400);
    }

    @Test
    void createProjectCriteria_ShouldReturnExpectedResponseBody_WhenMissingProjectIteration() throws Exception {
        // given
        String requestUrl = createProjectCriteria();
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath);

        givenUserDoesHavePermissionForBranch();

        // when
        ResultActions resultActions = mockMvc
                .perform(post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertEquals(buildErrorResponse(HttpStatus.BAD_REQUEST, "Project iteration is required."), getResponseBody(resultActions));
    }

    @Test
    void createProjectCriteria_ShouldReturnExpectedResponseStatus_WhenNegativeProjectIteration() throws Exception {
        // given
        String requestUrl = createProjectCriteria();
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, -1);

        givenUserDoesHavePermissionForBranch();

        // when
        ResultActions resultActions = mockMvc
                .perform(post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertResponseStatus(resultActions, 400);
    }

    @Test
    void createProjectCriteria_ShouldReturnExpectedResponseBody_WhenNegativeProjectIteration() throws Exception {
        // given
        String requestUrl = createProjectCriteria();
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, -1);

        givenUserDoesHavePermissionForBranch();

        // when
        ResultActions resultActions = mockMvc
                .perform(post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertEquals(buildErrorResponse(HttpStatus.BAD_REQUEST, "Project iteration cannot be less than 0."), getResponseBody(resultActions));
    }

    @Test
    void createProjectCriteria_ShouldReturnExpectedResponseStatus_WhenEntryAlreadyExists() throws Exception {
        // given
        String requestUrl = createProjectCriteria();
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, 52);

        givenUserDoesHavePermissionForBranch();
        givenAcceptanceCriteriaExists(branchPath, 52);

        // when
        ResultActions resultActions = mockMvc
                .perform(post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertResponseStatus(resultActions, 409);
    }

    @Test
    void createProjectCriteria_ShouldReturnExpectedResponseBody_WhenEntryAlreadyExists() throws Exception {
        // given
        String requestUrl = createProjectCriteria();
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, 53);

        givenUserDoesHavePermissionForBranch();
        givenAcceptanceCriteriaExists(branchPath, 53);

        // when
        ResultActions resultActions = mockMvc
                .perform(post(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertEquals(buildErrorResponse(HttpStatus.CONFLICT.value(), String.format("Project Acceptance Criteria already exists for branch %s and iteration %d.", branchPath, 53)),
                getResponseBody(resultActions));
    }

    private String createProjectCriteria() {
        return "/criteria/";
    }

    private void givenUserDoesHavePermissionForBranch() throws RestClientException {
        when(securityService.currentUserHasRoleOnBranch(any(), any())).thenReturn(true);
    }

    private void givenAcceptanceCriteriaExists(String branchPath, int projectIteration) {
        projectAcceptanceCriteriaService.create(new ProjectAcceptanceCriteria(branchPath, projectIteration));
    }

    private String asJson(Object input) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(input);
    }

    private String getResponseBody(ResultActions resultActions) throws UnsupportedEncodingException {
        return resultActions.andReturn().getResponse().getContentAsString();
    }

    private void assertResponseStatus(ResultActions result, int expectedResponseStatus) throws Exception {
        result.andExpect(status().is(expectedResponseStatus));
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
}
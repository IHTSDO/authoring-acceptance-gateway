package org.snomed.aag.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.traceability.RestResponsePage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.TestConfig;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
class AcceptanceCriteriaControllerTest extends AbstractTest {
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
    void findAll_ShouldReturnExpectedResponseStatusCode() throws Exception {
        // given
        String requestUrl = findAll(0, 10);
        givenAcceptanceCriteriaExists(UUID.randomUUID().toString(), 1);
        givenAcceptanceCriteriaExists(UUID.randomUUID().toString(), 1);

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl).contentType(MediaType.APPLICATION_JSON));

        // then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    void findAll_ShouldReturnExpectedResponseBody() throws Exception {
        // given
        String requestUrl = findAll(0, 10);
        givenAcceptanceCriteriaExists(UUID.randomUUID().toString(), 1);
        givenAcceptanceCriteriaExists(UUID.randomUUID().toString(), 1);

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl).contentType(MediaType.APPLICATION_JSON));
        List<ProjectAcceptanceCriteria> projectAcceptanceCriteria = toProjectAcceptCriterias(getResponseBody(resultActions));

        // then
        assertEquals(2, projectAcceptanceCriteria.size());
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseStatusCode_WhenCannotFindForBranchAndProjectIteration() throws Exception {
        // given
        String requestUrl = findForBranch(UUID.randomUUID().toString(), 1);

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl).contentType(MediaType.APPLICATION_JSON));

        // then
        assertResponseStatus(resultActions, 404);
        assertEquals(buildErrorResponse(404, "Not found"), getResponseBody(resultActions));
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseStatusCode_WhenCannotFindForBranchAndLatestProjectIteration() throws Exception {
        // given
        String requestUrl = findForBranch(UUID.randomUUID().toString(), null);

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl).contentType(MediaType.APPLICATION_JSON));

        // then
        assertResponseStatus(resultActions, 404);
        assertEquals(buildErrorResponse(404, "Cannot find ProjectAcceptanceCriteria."), getResponseBody(resultActions));
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseStatusCode_WhenCanFindForBranch() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, null);

        givenAcceptanceCriteriaExists(branchPath, 1);
        givenAcceptanceCriteriaExists(branchPath, 2);
        givenAcceptanceCriteriaExists(branchPath, 3);

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl).contentType(MediaType.APPLICATION_JSON));

        // then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseBody_WhenCanFindForBranchAndLatestProjectIteration() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, null);

        givenAcceptanceCriteriaExists(branchPath, 1);
        givenAcceptanceCriteriaExists(branchPath, 3);
        givenAcceptanceCriteriaExists(branchPath, 2);

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl).contentType(MediaType.APPLICATION_JSON));
        ProjectAcceptanceCriteria projectAcceptanceCriteria = toProjectAcceptCriteria(getResponseBody(resultActions));

        // then
        assertEquals(3, projectAcceptanceCriteria.getProjectIteration()); //latest expected as null projectIteration
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseBody_WhenCanFindForBranchAndSpecificProjectIteration() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, 1);

        givenAcceptanceCriteriaExists(branchPath, 1);
        givenAcceptanceCriteriaExists(branchPath, 3);
        givenAcceptanceCriteriaExists(branchPath, 2);

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl).contentType(MediaType.APPLICATION_JSON));
        ProjectAcceptanceCriteria projectAcceptanceCriteria = toProjectAcceptCriteria(getResponseBody(resultActions));

        // then
        assertEquals(1, projectAcceptanceCriteria.getProjectIteration());
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseBody_WhenRequestingNegativeProjectIteration() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, -5);

        givenAcceptanceCriteriaExists(branchPath, 1);
        givenAcceptanceCriteriaExists(branchPath, 3);
        givenAcceptanceCriteriaExists(branchPath, 2);

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl).contentType(MediaType.APPLICATION_JSON));

        // then
        assertResponseStatus(resultActions, 400);
        assertEquals(buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid parameters."), getResponseBody(resultActions));
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
        ProjectAcceptanceCriteria result = projectAcceptanceCriteriaService.findByBranchPathAndProjectIteration(branchPath, 23);

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

    @Test
    void updateProjectCriteria_ShouldReturnExpectedResponse_WhenAmbiguousBranchAndUpdatingLatestProjectAcceptanceCriteria() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(UUID.randomUUID().toString(), 2);
        String requestUrl = updateProjectCriteria(branchPath, null);

        givenUserDoesHavePermissionForBranch();
        givenAcceptanceCriteriaExists(branchPath, 2);

        // when
        ResultActions resultActions = mockMvc
                .perform(put(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertResponseStatus(resultActions, 409);
        assertEquals(buildErrorResponse(409, "Branch in URL does not match branch in criteria."), getResponseBody(resultActions));
    }

    @Test
    void updateProjectCriteria_ShouldReturnExpectedResponse_WhenAmbiguousProjectIterationAndUpdatingLatestProjectAcceptanceCriteria() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, 3);
        String requestUrl = updateProjectCriteria(branchPath, null);

        givenUserDoesHavePermissionForBranch();
        givenAcceptanceCriteriaExists(branchPath, 2);

        // when
        ResultActions resultActions = mockMvc
                .perform(put(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertResponseStatus(resultActions, 409);
        assertEquals(buildErrorResponse(409, "Project Iteration in URL does not match Project Iteration in criteria."), getResponseBody(resultActions));
    }

    @Test
    void updateProjectCriteria_ShouldReturnExpectedResponse_WhenNewCriteriaItemDoesNotExist() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, 2);
        projectAcceptanceCriteria.setSelectedProjectCriteriaIds(Collections.singleton("test-criteria-item"));
        String requestUrl = updateProjectCriteria(branchPath, null);

        givenUserDoesHavePermissionForBranch();
        givenAcceptanceCriteriaExists(branchPath, 2);

        // when
        ResultActions resultActions = mockMvc
                .perform(put(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );

        // then
        assertResponseStatus(resultActions, 400);
        assertEquals(buildErrorResponse(HttpStatus.BAD_REQUEST, "The following criteria items were not found: [test-criteria-item]"), getResponseBody(resultActions));
    }

    @Test
    void updateProjectCriteria_ShouldReturnExpectedResponse_WhenSuccessfullyUpdating() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, 2);
        projectAcceptanceCriteria.setSelectedProjectCriteriaIds(Collections.singleton("test-criteria-item"));
        String requestUrl = updateProjectCriteria(branchPath, null);

        givenUserDoesHavePermissionForBranch();
        givenAcceptanceCriteriaExists(branchPath, 2);
        givenCriteriaItemExists("test-criteria-item", true, 1, "An example Criteria Item.");

        // when
        ResultActions resultActions = mockMvc
                .perform(put(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );
        ProjectAcceptanceCriteria updatedProjectAcceptanceCriteria = toProjectAcceptCriteria(getResponseBody(resultActions));

        // then
        assertResponseStatus(resultActions, 200);
        assertEquals(1, updatedProjectAcceptanceCriteria.getSelectedProjectCriteriaIds().size());
    }

    @Test
    void updateProjectCriteria_ShouldUpdateLatestProjectIteration_WhenNotSpecified() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath);
        projectAcceptanceCriteria.setSelectedProjectCriteriaIds(Collections.singleton("test-criteria-item"));
        String requestUrl = updateProjectCriteria(branchPath, null);

        givenUserDoesHavePermissionForBranch();
        givenAcceptanceCriteriaExists(branchPath, 1);
        givenCriteriaItemExists("test-criteria-item", true, 1, "An example Criteria Item.");

        // when
        ResultActions resultActions = mockMvc
                .perform(put(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(projectAcceptanceCriteria))
                );
        ProjectAcceptanceCriteria updatedProjectAcceptanceCriteria = toProjectAcceptCriteria(getResponseBody(resultActions));

        // then
        assertEquals(1, updatedProjectAcceptanceCriteria.getSelectedProjectCriteriaIds().size());
    }

    @Test
    void updateProjectCriteria_ShouldNotUpdateCreationDate() throws Exception {
        String branchPath = UUID.randomUUID().toString();
        String createProjectCriteria = createProjectCriteria();
        String findForBranch = findForBranch(branchPath, null);
        String updateProjectCriteria = updateProjectCriteria(branchPath, null);
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branchPath, 1);

        givenUserDoesHavePermissionForBranch();
        givenAcceptanceCriteriaExists(branchPath, 1);
        givenCriteriaItemExists("test-criteria-item", true, 1, "An example Criteria Item.");

        // Create PAC & get creationDate
        mockMvc.perform(post(createProjectCriteria).contentType(MediaType.APPLICATION_JSON).content(asJson(projectAcceptanceCriteria)));
        ResultActions findForBranchResponse = mockMvc.perform(get(findForBranch).contentType(MediaType.APPLICATION_JSON));
        Date creationDate = toProjectAcceptCriteria(getResponseBody(findForBranchResponse)).getCreationDate();

        // Update PAC
        projectAcceptanceCriteria.setSelectedProjectCriteriaIds(Collections.singleton("test-criteria-item"));
        ResultActions updateProjectCriteriaResponse = mockMvc.perform(put(updateProjectCriteria).contentType(MediaType.APPLICATION_JSON).content(asJson(projectAcceptanceCriteria)));
        Date newCreationDate = toProjectAcceptCriteria(getResponseBody(updateProjectCriteriaResponse)).getCreationDate();

        // creationDate should not have changed
        assertEquals(creationDate, newCreationDate);
    }

    @Test
    void deleteProjectCriteria_ShouldReturnExpectedResponse_WhenNoLatestProjectAcceptanceCriteriaCanBeFoundFromBranch() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = deleteProjectCriteria(branchPath, null);
        String expectedErrorMessage = String.format("Branch %s has no Acceptance Criteria.", branchPath);

        // when
        ResultActions resultActions = mockMvc
                .perform(delete(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                );

        // then
        assertResponseStatus(resultActions, 404);
        assertEquals(buildErrorResponse(404, expectedErrorMessage), getResponseBody(resultActions));
    }

    @Test
    void deleteProjectCriteria_ShouldReturnExpectedResponse_WhenNoProjectAcceptanceCriteriaCanBeFoundFromBranch() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = deleteProjectCriteria(branchPath, 1);

        // when
        ResultActions resultActions = mockMvc
                .perform(delete(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                );

        // then
        assertResponseStatus(resultActions, 404);
        assertEquals(buildErrorResponse(404, "Not found"), getResponseBody(resultActions));
    }

    @Test
    void deleteProjectCriteria_ShouldReturnExpectedResponse_WhenDeletingLatestProjectAcceptanceCriteria() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = deleteProjectCriteria(branchPath, 1);

        givenAcceptanceCriteriaExists(branchPath, 1);

        // when
        ResultActions resultActions = mockMvc
                .perform(delete(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                );

        // then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    void deleteProjectCriteria_ShouldReturnExpectedResponse_WhenDeletingSpecificProjectAcceptanceCriteria() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = deleteProjectCriteria(branchPath, 2);

        givenAcceptanceCriteriaExists(branchPath, 1);
        givenAcceptanceCriteriaExists(branchPath, 2);
        givenAcceptanceCriteriaExists(branchPath, 3);

        // when
        ResultActions resultActions = mockMvc
                .perform(delete(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                );

        // then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    void deleteProjectCriteria_ShouldRemoveEntryFromDatabase_WhenDeletingSpecificProjectAcceptanceCriteria() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = deleteProjectCriteria(branchPath, 2);

        givenAcceptanceCriteriaExists(branchPath, 1);
        givenAcceptanceCriteriaExists(branchPath, 2);
        givenAcceptanceCriteriaExists(branchPath, 3);

        // when
        mockMvc.perform(delete(requestUrl).contentType(MediaType.APPLICATION_JSON));

        // then
        assertNull(projectAcceptanceCriteriaService.findByBranchPathAndProjectIteration(branchPath, 2));
        assertNotNull(projectAcceptanceCriteriaService.findByBranchPathAndProjectIteration(branchPath, 1));
        assertNotNull(projectAcceptanceCriteriaService.findByBranchPathAndProjectIteration(branchPath, 3));
    }

    @Test
    void deleteProjectCriteria_ShouldRemoveEntryFromDatabase_WhenDeletingLatestProjectAcceptanceCriteria() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = deleteProjectCriteria(branchPath, null);

        givenAcceptanceCriteriaExists(branchPath, 1);
        givenAcceptanceCriteriaExists(branchPath, 2);
        givenAcceptanceCriteriaExists(branchPath, 3);

        // when
        mockMvc.perform(delete(requestUrl).contentType(MediaType.APPLICATION_JSON));

        // then
        assertNull(projectAcceptanceCriteriaService.findByBranchPathAndProjectIteration(branchPath, 3));
        assertNotNull(projectAcceptanceCriteriaService.findByBranchPathAndProjectIteration(branchPath, 1));
        assertNotNull(projectAcceptanceCriteriaService.findByBranchPathAndProjectIteration(branchPath, 2));
    }

    private String deleteProjectCriteria(String branchPath, Integer projectIteration) {
        if (projectIteration == null) {
            return "/criteria/" + branchPath;
        }

        return "/criteria/" + branchPath + "?projectIteration=" + projectIteration;
    }

    private String findAll(int page, int size) {
        return "/criteria?page=" + page + "&size=" + size;
    }

    private String createProjectCriteria() {
        return "/criteria/";
    }

    private String updateProjectCriteria(String branchPath, Integer projectIteration) {
        if (projectIteration == null) {
            return "/criteria/" + branchPath;
        }

        return "/criteria/" + branchPath + "?projectIteration=" + projectIteration;
    }

    private String findForBranch(String branchPath, Integer projectIteration) {
        if (projectIteration == null) {
            return "/criteria/" + branchPath;
        }

        return "/criteria/" + branchPath + "?projectIteration=" + projectIteration;
    }

    private void givenAcceptanceCriteriaExists(String branchPath, int projectIteration) {
        projectAcceptanceCriteriaService.create(new ProjectAcceptanceCriteria(branchPath, projectIteration));
    }

    private void givenCriteriaItemExists(String criteriaItemId, boolean manual, int order, String label) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRole("ROLE_ACCEPTANCE_CRITERIA_CONTROLLER_TEST");
        criteriaItem.setOrder(order);
        criteriaItem.setLabel(label);

        criteriaItemRepository.save(criteriaItem);
    }

    private List<ProjectAcceptanceCriteria> toProjectAcceptCriterias(String response) throws JsonProcessingException {
        RestResponsePage<ProjectAcceptanceCriteria> restResponsePage = OBJECT_MAPPER.readValue(response, new TypeReference<RestResponsePage<ProjectAcceptanceCriteria>>() {
        });
        return restResponsePage.getContent();
    }

    private ProjectAcceptanceCriteria toProjectAcceptCriteria(String response) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(response, new TypeReference<ProjectAcceptanceCriteria>() {
        });
    }
}

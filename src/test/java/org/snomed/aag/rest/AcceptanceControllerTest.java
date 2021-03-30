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
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
class AcceptanceControllerTest extends AbstractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BRANCH_PATH = "MAIN";

    private AcceptanceController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        this.controller = new AcceptanceController(
                criteriaItemService,
                branchService,
                criteriaItemSignOffService,
                securityService
        );
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new RestControllerAdvice())
                .build();
    }

    @AfterEach
    public void tearDown() {
        this.controller = null;
        this.mockMvc = null;
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenCriteriaItemCannotBeFoundFromId() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(criteriaItemId);

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 404);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.NOT_FOUND, "Criteria Item with id '" + criteriaItemId + "' not found."));
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenCriteriaItemCannotBeModified() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, false);

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Criteria Item cannot be changed manually."));
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenBranchDoesNotExist() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true);
        givenBranchDoesNotExist();

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Branch does not exist."));
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenUserDoesNotHaveDesiredRole() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true);
        givenUserDoesNotHavePermissionForBranch();

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "User does not have desired role."));
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenSuccessfullySigningOffCriteriaItem() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(criteriaItemId);

        givenCriteriaItemExists(criteriaItemId, true);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(System.currentTimeMillis());

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));

        //then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedBody_WhenSuccessfullySigningOffCriteriaItem() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(criteriaItemId);
        long timestamp = System.currentTimeMillis();
        String username = "AcceptanceControllerTest";

        givenCriteriaItemExists(criteriaItemId, true);
        givenUserDoesHavePermissionForBranch();
        givenBranchDoesExist(timestamp);
        givenAuthenticatedUser(username);

        //when
        ResultActions resultActions = mockMvc.perform(post(requestUrl));
        CriteriaItemSignOff criteriaItemSignOff = OBJECT_MAPPER.readValue(getResponseBody(resultActions), CriteriaItemSignOff.class);

        //then
        assertEquals(criteriaItemId, criteriaItemSignOff.getCriteriaItemId());
        assertEquals(BRANCH_PATH, criteriaItemSignOff.getBranch());
        assertEquals(timestamp, criteriaItemSignOff.getBranchHeadTimestamp());
        assertEquals(username, criteriaItemSignOff.getUserId());
    }

    @Test
    public void signOffCriteriaItem_ShouldAddRecordToStore_WhenSuccessfullySigningOffCriteriaItem() throws Exception {
        //given
        String criteriaItemId = UUID.randomUUID().toString();
        String requestUrl = signOffCriteriaItem(criteriaItemId);
        long timestamp = System.currentTimeMillis();
        String username = "AcceptanceControllerTest";

        givenCriteriaItemExists(criteriaItemId, true);
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

    private String signOffCriteriaItem(String criteriaItemId) {
        return "/acceptance/" + BRANCH_PATH + "/item/" + criteriaItemId + "/accept";
    }

    private String buildErrorResponse(HttpStatus error, String message) throws JsonProcessingException {
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

    private void givenCriteriaItemExists(String criteriaItemId, boolean manual) {
        CriteriaItem criteriaItem = new CriteriaItem(criteriaItemId);
        criteriaItem.setManual(manual);
        criteriaItem.setRequiredRole("ROLE_ACCEPTANCE_CONTROLLER_TEST");

        criteriaItemRepository.save(criteriaItem);
    }

    private void givenBranchDoesNotExist() throws RestClientException {
        when(securityService.currentUserHasRoleOnBranch(any(), any())).thenThrow(new AccessDeniedException("Branch does not exist."));
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

    private String getResponseBody(ResultActions resultActions) throws UnsupportedEncodingException {
        return resultActions.andReturn().getResponse().getContentAsString();
    }
}

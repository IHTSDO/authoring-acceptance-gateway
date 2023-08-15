package org.snomed.aag.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.TestConfig;
import org.snomed.aag.data.domain.WhitelistItem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
class WhitelistControllerTest extends AbstractTest {
    private WhitelistController whitelistController;
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        this.whitelistController = new WhitelistController(whitelistService, securityService);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(whitelistController)
                .setControllerAdvice(new RestControllerAdvice())
                .build();
    }

    @AfterEach
    public void tearDown() {
        this.whitelistController = null;
        this.mockMvc = null;
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponse_WhenBranchNotFound() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, 1L);

        givenBranchDoesNotExist();

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        // then
        assertResponseStatus(resultActions, 403);
        assertResponseBody(resultActions, buildErrorResponse(HttpStatus.FORBIDDEN, "Branch does not exist."));
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponse_WhenNoWhitelistItems() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        long creationDate = System.currentTimeMillis();
        String requestUrl = findForBranch(branchPath, creationDate);

        givenBranchDoesExist(System.currentTimeMillis());

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        // then
        assertResponseStatus(resultActions, 204);
        assertResponseBody(resultActions, "[]");
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponse_WhenNoWhitelistItemsMatchesRequestParam() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        givenWhitelistItemExists(branchPath);
        givenBranchDoesExist(System.currentTimeMillis());

        long requestedCreationDate = System.currentTimeMillis();
        String requestUrl = findForBranch(branchPath, requestedCreationDate); //Requesting after creation of whitelist item

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        // then
        assertResponseStatus(resultActions, 204);
        assertResponseBody(resultActions, "[]");
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseStatusCode_WhenWhitelistItemsMatchesRequestParam() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, System.currentTimeMillis()); //Requesting before creation of whitelist item

        givenWhitelistItemExists(branchPath);
        givenBranchDoesExist(System.currentTimeMillis());

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));

        // then
        assertResponseStatus(resultActions, 200);
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseBody_WhenWhitelistItemsMatchesRequestParam() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, System.currentTimeMillis()); //Requesting before creation of whitelist item

        givenWhitelistItemExists(branchPath);
        givenWhitelistItemExists(branchPath + "/projectA");
        givenBranchDoesExist(System.currentTimeMillis());

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        List<WhitelistItem> whitelistItems = toWhitelistItems(getResponseBody(resultActions));

        // then
        assertEquals(2, whitelistItems.size()); //found child branches
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseBody_WhenWhitelistItemMatchesRequestParam() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, System.currentTimeMillis()); //Requesting before creation of whitelist item

        givenWhitelistItemExists(branchPath);
        givenBranchDoesExist(System.currentTimeMillis());

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        List<WhitelistItem> whitelistItems = toWhitelistItems(getResponseBody(resultActions));

        // then
        assertEquals(1, whitelistItems.size());
    }

    @Test
    void findForBranch_ShouldReturnExpectedResponseBody_WhenDefaultCreationDateIsApplied() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath);

        givenWhitelistItemExists(branchPath);
        givenWhitelistItemExists(branchPath + "/projectA");
        givenWhitelistItemExists(branchPath + "/projectB");
        givenWhitelistItemExists(branchPath + "/projectC");
        givenBranchDoesExist(System.currentTimeMillis());

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        List<WhitelistItem> whitelistItems = toWhitelistItems(getResponseBody(resultActions));

        // then
        assertEquals(4, whitelistItems.size()); //Found everything
    }

    @Test
    void findForBranch_ShouldReturnTransientProperty() throws Exception {
        // given
        String branchPath = UUID.randomUUID().toString();
        String requestUrl = findForBranch(branchPath, System.currentTimeMillis());

        givenWhitelistItemExists(branchPath);
        givenBranchDoesExist(System.currentTimeMillis());

        // when
        ResultActions resultActions = mockMvc.perform(get(requestUrl));
        String responseBody = getResponseBody(resultActions);

        // then
        assertTrue(responseBody.contains("\"creationDateLong\""));
    }

    private void givenWhitelistItemExists(String branch) throws Exception {
        // given
        String requestUrl = addWhitelistItem();
        WhitelistItem whitelistItem = new WhitelistItem();
        whitelistItem.setUserId("WhitelistControllerTest");
        whitelistItem.setValidationRuleId("test-rule-id");
        whitelistItem.setComponentId("100");
        whitelistItem.setConceptId("101");
        whitelistItem.setBranch(branch);

        // when
        ResultActions resultActions = mockMvc.perform(post(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(whitelistItem)));

        // then
        assertResponseStatus(resultActions, 201);
    }

    private String findForBranch(String branchPath, long creationDate) {
        return "/whitelist-items/" + branchPath + "?creationDate=" + creationDate;
    }

    private String findForBranch(String branchPath) {
        return "/whitelist-items/" + branchPath;
    }

    private String addWhitelistItem() {
        return "/whitelist-items/";
    }

    private List<WhitelistItem> toWhitelistItems(String response) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(response, new TypeReference<>() {
        });
    }
}

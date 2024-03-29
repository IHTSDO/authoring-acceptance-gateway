package org.snomed.aag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.snomed.aag.data.repositories.CriteriaItemRepository;
import org.snomed.aag.data.repositories.CriteriaItemSignOffRepository;
import org.snomed.aag.data.repositories.ProjectAcceptanceCriteriaRepository;
import org.snomed.aag.data.repositories.WhitelistItemRepository;
import org.snomed.aag.data.services.*;
import org.snomed.aag.data.validators.CommitInformationValidator;
import org.snomed.aag.data.validators.ProjectAcceptanceCriteriaUpdateValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@Testcontainers
@ContextConfiguration(classes = TestConfig.class)
public abstract class AbstractTest {
	protected static final ObjectMapper OBJECT_MAPPER;

	static {
		OBJECT_MAPPER = new ObjectMapper();
		OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Autowired
	protected CriteriaItemRepository criteriaItemRepository;

	@Autowired
	protected CriteriaItemSignOffRepository criteriaItemSignOffRepository;

	@Autowired
	protected WhitelistItemRepository whitelistItemRepository;

	@Autowired
	protected ProjectAcceptanceCriteriaRepository projectAcceptanceCriteriaRepository;

	@Autowired
	protected CriteriaItemService criteriaItemService;

	@Autowired
	protected CriteriaItemSignOffService criteriaItemSignOffService;

	@Autowired
	protected WhitelistService whitelistService;

	@Autowired
	protected ProjectAcceptanceCriteriaService projectAcceptanceCriteriaService;

	@Autowired
	protected ProjectAcceptanceCriteriaUpdateValidator projectAcceptanceCriteriaUpdateValidator;

	@Autowired
	protected AcceptanceService acceptanceService;

	@Autowired
	protected CommitInformationValidator commitInformationValidator;

	@MockBean
	protected BranchSecurityService securityService;

	@MockBean
	protected ValidationService validationService;

	@AfterEach
	void defaultTearDown() {
		criteriaItemRepository.deleteAll();
		criteriaItemSignOffRepository.deleteAll();
		projectAcceptanceCriteriaRepository.deleteAll();
		whitelistItemRepository.deleteAll();
	}

	protected void givenBranchDoesNotExist() throws RestClientException {
		doThrow(new AccessDeniedException("Branch does not exist.")).when(securityService).verifyBranchRole(any(), any());
		when(securityService.currentUserHasRoleOnBranch(any(), any())).thenThrow(new AccessDeniedException("Branch does not exist."));
		when(securityService.getBranchOrThrow(any())).thenThrow(new AccessDeniedException("Branch does not exist."));
	}

	protected void givenBranchDoesExist() throws RestClientException {
		givenBranchDoesExist(System.currentTimeMillis());
	}

	protected void givenBranchDoesExist(String branchPath) throws RestClientException {
		Branch branch = new Branch();
		branch.setPath(branchPath);

		when(securityService.getBranchOrThrow(any())).thenReturn(branch);
	}

	protected void givenBranchDoesExist(long timestamp) throws RestClientException {
		Branch branch = new Branch();
		branch.setHeadTimestamp(timestamp);

		when(securityService.getBranchOrThrow(any())).thenReturn(branch);
	}

	protected void givenBranchDoesExist(long timestamp, Map<String, Object> metadata) throws RestClientException {
		Branch branch = new Branch();
		branch.setHeadTimestamp(timestamp);
		branch.setMetadata(metadata);

		when(securityService.getBranchOrThrow(anyString())).thenReturn(branch);
	}

	protected void givenBranchDoesExist(String branchPath, Map<String, Object> metadata) throws RestClientException {
		Branch branch = new Branch();
		branch.setPath(branchPath);
		branch.setMetadata(metadata);

		when(securityService.getBranchOrThrow(anyString())).thenReturn(branch);
	}

	protected void givenUserDoesNotHavePermissionForBranch() {
		doThrow(new AccessDeniedException("User does not have desired role.")).when(securityService).verifyBranchRole(any(), any());
	}

	protected void givenUserDoesHavePermissionForBranch() throws RestClientException {
		when(securityService.currentUserHasRoleOnBranch(any(), any())).thenReturn(true);
	}

	protected void givenValidationReportIsClean() {
		when(validationService.isReportClean(any(), anyLong(), any())).thenReturn(true);
	}

	protected void givenValidationReportIsDirty() {
		when(validationService.isReportClean(any(), anyLong(), any())).thenReturn(false);
	}

	protected void givenAuthenticatedUser(String username) {
		Authentication authentication = mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(username);

		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);

		SecurityContextHolder.setContext(securityContext);
	}

	protected String buildErrorResponse(HttpStatus error, String message) throws JsonProcessingException {
		Map<String, Object> response = new HashMap<>();
		response.put("error", error);
		response.put("message", message);

		return OBJECT_MAPPER.writeValueAsString(response);
	}

	protected String buildErrorResponse(int error, String message) throws JsonProcessingException {
		Map<String, Object> response = new HashMap<>();
		response.put("error", error);
		response.put("message", message);

		return OBJECT_MAPPER.writeValueAsString(response);
	}

	protected void assertResponseStatus(ResultActions result, int expectedResponseStatus) throws Exception {
		result.andExpect(status().is(expectedResponseStatus));
	}

	protected void assertResponseBody(ResultActions result, String expectedResponseBody) throws Exception {
		result.andExpect(content().string(expectedResponseBody));
	}

	protected void assertResponseBodyIsEmpty(ResultActions result) throws Exception {
		result.andExpect(content().string(""));
	}

	/*
	 * MockMvc doesn't follow re-directs. Instead of sending a request
	 * where a branch has a slash (which relies on BranchPathUriRewriteFilter),
	 * send a request with the branch already decoded.
	 * */
	protected String withPipeInsteadOfSlash(String branch) {
		return branch.replaceAll("/", "|");
	}

	protected String asJson(Object input) throws JsonProcessingException {
		return OBJECT_MAPPER.writeValueAsString(input);
	}

	protected String getResponseBody(ResultActions resultActions) throws UnsupportedEncodingException {
		return resultActions.andReturn().getResponse().getContentAsString();
	}
}

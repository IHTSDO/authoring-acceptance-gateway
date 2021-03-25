package org.snomed.aag.rest;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.rest.pojo.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class AcceptanceControllerTest extends AbstractTest {
    private AcceptanceController target;

    @BeforeEach
    public void setUp() {
        this.target = new AcceptanceController(
                criteriaItemService,
                securityServiceMock,
                criteriaItemSignOffService
        );
    }

    @AfterEach
    public void tearDown() {
        this.target = null;
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenCriteriaItemCannotBeFoundFromId() throws RestClientException {
        //given
        String branchPath = "MAIN";
        String criteriaItemId = UUID.randomUUID().toString();

        //when
        ResponseEntity<?> result = target.signOffCriteriaItem(branchPath, criteriaItemId);

        //then
        assertEquals(result.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenCriteriaItemCannotBeModified() throws RestClientException {
        //given
        String branchPath = "MAIN";
        String criteriaItemId = UUID.randomUUID().toString();
        givenCriteriaItemExists(criteriaItemId, false);

        //when
        ResponseEntity<?> result = target.signOffCriteriaItem(branchPath, criteriaItemId);

        //then
        assertEquals(result.getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(result.getBody().toString(), new ErrorMessage(HttpStatus.FORBIDDEN.toString(), "Criteria Item cannot be changed manually.").toString());
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenBranchDoesNotExist() throws RestClientException {
        //given
        String branchPath = "MAIN";
        String criteriaItemId = UUID.randomUUID().toString();
        givenCriteriaItemExists(criteriaItemId, true);
        givenBranchDoesNotExist();

        //when
        ResponseEntity<?> result = target.signOffCriteriaItem(branchPath, criteriaItemId);

        //then
        assertEquals(result.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenUserDoesNotHaveDesiredRole() throws RestClientException {
        //given
        String branchPath = "MAIN";
        String criteriaItemId = UUID.randomUUID().toString();
        givenCriteriaItemExists(criteriaItemId, true);
        givenUserDoesNotHaveDesiredRole();

        //when
        ResponseEntity<?> result = target.signOffCriteriaItem(branchPath, criteriaItemId);

        //then
        assertEquals(result.getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(result.getBody().toString(), new ErrorMessage(HttpStatus.FORBIDDEN.toString(), "User does not have desired role.").toString());
    }

    @Test
    public void signOffCriteriaItem_ShouldReturnExpectedResponse_WhenSuccessfullySigningOffCriteriaItem() throws RestClientException {
        //given
        String branchPath = "MAIN";
        String criteriaItemId = UUID.randomUUID().toString();
        String username = "AcceptanceControllerTest";
        long branchHeadTimestamp = System.currentTimeMillis();

        givenCriteriaItemExists(criteriaItemId, true);
        givenUserDoesHaveDesiredRole();
        givenBranchExists(branchHeadTimestamp);
        givenAuthenticatedUser(username);

        //when
        ResponseEntity<?> result = target.signOffCriteriaItem(branchPath, criteriaItemId);
        CriteriaItemSignOff body = (CriteriaItemSignOff) result.getBody();

        //then
        assertEquals(result.getStatusCode(), HttpStatus.OK);
        assertEquals(body.getCriteriaItemId(), criteriaItemId);
        assertEquals(body.getBranch(), branchPath);
        assertEquals(body.getBranchHeadTimestamp(), branchHeadTimestamp);
        assertEquals(body.getUserId(), username);
    }

    @Test
    public void signOffCriteriaItem_ShouldAddRecordToStore_WhenSuccessfullySigningOffCriteriaItem() throws RestClientException {
        //given
        String branchPath = "MAIN";
        String criteriaItemId = UUID.randomUUID().toString();
        long branchHeadTimestamp = System.currentTimeMillis();

        givenCriteriaItemExists(criteriaItemId, true);
        givenUserDoesHaveDesiredRole();
        givenBranchExists(branchHeadTimestamp);

        ResponseEntity<?> responseEntity = target.signOffCriteriaItem(branchPath, criteriaItemId);
        CriteriaItemSignOff responseEntityBody = (CriteriaItemSignOff) responseEntity.getBody();

        //when
        CriteriaItem result = criteriaItemService.findOrThrow(responseEntityBody.getCriteriaItemId());

        //then
        assertNotNull(result);
    }

    private void givenAuthenticatedUser(String username) {
        //given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(username);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void givenBranchExists(long timestamp) throws RestClientException {
        //given
        Branch branch = new Branch();
        branch.setHeadTimestamp(timestamp);

        when(securityServiceMock.getBranchOrThrow(any())).thenReturn(branch);
    }

    private void givenBranchDoesNotExist() throws RestClientException {
        //given
        when(securityServiceMock.currentUserHasRoleOnBranch(any(), any())).thenThrow(new AccessDeniedException("Branch does not exist."));
    }

    private void givenUserDoesHaveDesiredRole() throws RestClientException {
        //given
        when(securityServiceMock.currentUserHasRoleOnBranch(any(), any())).thenReturn(true);
    }

    private void givenUserDoesNotHaveDesiredRole() throws RestClientException {
        //given
        when(securityServiceMock.currentUserHasRoleOnBranch(any(), any())).thenReturn(false);
    }

    private void givenCriteriaItemExists(String id, boolean manual) {
        //given
        CriteriaItem criteriaItem = new CriteriaItem(id);
        criteriaItem.setManual(manual);

        criteriaItemService.create(criteriaItem);
    }
}

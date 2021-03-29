package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class BranchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchService.class);

    private final SecurityService securityService;

    public BranchService(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Verify whether the branch has the expected permissions.
     *
     * @param branchPath   Branch to check.
     * @param requiredRole Role the branch should have.
     * @throws IllegalArgumentException When given illegal arguments.
     * @throws AccessDeniedException    When branch does not exist or when user does not have desired role.
     */
    public void verifyBranchPermission(String branchPath, String requiredRole) {
        if (branchPath == null || requiredRole == null) {
            LOGGER.error("Cannot verify branch permissions as given illegal arguments.");
            LOGGER.debug("branchPath: {}, requiredRole: {}", branchPath, requiredRole);
            throw new IllegalArgumentException("Cannot verify branch permission.");
        }

        try {
            if (!securityService.currentUserHasRoleOnBranch(requiredRole, branchPath)) {
                LOGGER.info("User does not have desired role of {}.", requiredRole);
                throw new AccessDeniedException("User does not have desired role.");
            }
        } catch (RestClientException e) {
            LOGGER.error("Cannot verify request for branch {} with requiredRole {}.", branchPath, requiredRole);
            LOGGER.debug(e.getMessage());
            throw new AccessDeniedException("Could not ascertain user roles: Failed to communication with Snowstorm.", e);
        }
    }
}

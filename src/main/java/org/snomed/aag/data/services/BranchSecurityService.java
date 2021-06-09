package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClientFactory;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class BranchSecurityService {

	// This factory produces clients authenticated as the current user, it has its own client cache.
	private final SnowstormRestClientFactory snowstormRestClientFactory;

	private static final Logger LOGGER = LoggerFactory.getLogger(BranchSecurityService.class);

	public BranchSecurityService(@Value("${snowstorm.url}") String snowstormUrl) {
		snowstormRestClientFactory = new SnowstormRestClientFactory(snowstormUrl, null);
	}

	/**
	 * Verify whether the branch has the expected permissions.
	 *
	 * @param branchPath   Branch to check.
	 * @param requiredRole Role the branch should have.
	 * @throws IllegalArgumentException When given illegal arguments.
	 * @throws AccessDeniedException    When branch does not exist or when user does not have desired role.
	 */
	public void verifyBranchRole(String branchPath, String requiredRole) {
		if (branchPath == null || requiredRole == null) {
			LOGGER.error("Cannot verify branch permissions as given illegal arguments.");
			LOGGER.debug("branchPath: {}, requiredRole: {}", branchPath, requiredRole);
			throw new IllegalArgumentException("Cannot verify branch permission.");
		}

		try {
			if (!currentUserHasRoleOnBranch(requiredRole, branchPath)) {
				LOGGER.info("User does not have desired role of {}.", requiredRole);
				throw new AccessDeniedException("User does not have desired role.");
			}
		} catch (RestClientException e) {
			LOGGER.error("Cannot verify request for branch {} with requiredRole {}.", branchPath, requiredRole);
			LOGGER.debug(e.getMessage());
			throw new AccessDeniedException("Could not ascertain user roles: Failed to communication with Snowstorm.", e);
		}
	}

	public boolean currentUserHasRoleOnBranch(String role, String branchPath) throws RestClientException {
		if (branchPath.equals("global")) {
			return getBranchOrThrow("MAIN").getGlobalUserRoles().contains(role);
		} else {
			return getBranchOrThrow(branchPath).getUserRoles().contains(role);
		}
	}

	public Branch getBranchOrThrow(String branchPath) throws RestClientException {
		final Branch branch = snowstormRestClientFactory.getClient().getBranch(branchPath);
		if (branch == null) {
			throw new AccessDeniedException("Branch does not exist.");
		}
		return branch;
	}

	public Set<String> getBranchRoles(String branchPath) {
		try {
			final Branch branch = getBranchOrThrow(branchPath);
			if (branch != null) {
				branch.getUserRoles();// This already includes any global roles
			}
		} catch (RestClientException e) {
			LOGGER.debug("Failed to fetch branch {}", branchPath, e);
		}
		return Collections.emptySet();
	}
}

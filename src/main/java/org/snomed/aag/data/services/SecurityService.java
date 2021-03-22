package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClientFactory;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
public class SecurityService {

	// This factory produces clients authenticated as the current user, it has its own client cache.
	private final SnowstormRestClientFactory snowstormRestClientFactory;

	public SecurityService(@Value("${snowstorm.url}") String snowstormUrl) {
		snowstormRestClientFactory = new SnowstormRestClientFactory(snowstormUrl, null);
	}

	public boolean isGlobalAdmin() throws RestClientException {
		return getBranchOrThrow("MAIN").getGlobalUserRoles().contains("ADMIN");
	}

	public boolean currentUserHasRoleOnBranch(String role, String branchPath) throws RestClientException {
		if (branchPath.equals("global")) {
			return getBranchOrThrow("MAIN").getGlobalUserRoles().contains(role);
		} else {
			return getBranchOrThrow(branchPath).getUserRoles().contains(role);
		}
	}

	private Branch getBranchOrThrow(String branchPath) throws RestClientException {
		final Branch branch = snowstormRestClientFactory.getClient().getBranch(branchPath);
		if (branch == null) {
			throw new AccessDeniedException("Branch does not exist.");
		}
		return branch;
	}
}

package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClientFactory;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

	// This factory produces clients authenticated as the current user, it has its own client cache.
	private final SnowstormRestClientFactory snowstormRestClientFactory;

	public SecurityService(@Value("${snowstorm.url}") String snowstormUrl) {
		snowstormRestClientFactory = new SnowstormRestClientFactory(snowstormUrl, null);
	}

	public boolean isGlobalAdmin() throws RestClientException {
		return getBranch("MAIN").getGlobalUserRoles().contains("ADMIN");
	}

	public boolean currentUserHasRoleOnBranch(String role, String branchPath) throws RestClientException {
		if (branchPath.equals("global")) {
			return getBranch("MAIN").getGlobalUserRoles().contains(role);
		} else {
			return getBranch(branchPath).getUserRoles().contains(role);
		}
	}

	private Branch getBranch(String branchPath) throws RestClientException {
		return snowstormRestClientFactory.getClient().getBranch(branchPath);
	}
}

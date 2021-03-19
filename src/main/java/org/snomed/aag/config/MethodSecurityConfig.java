package org.snomed.aag.config;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.snomed.aag.data.services.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

import static java.lang.String.format;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

	@Autowired
	private PermissionEvaluator permissionEvaluator;

	@Override
	protected MethodSecurityExpressionHandler createExpressionHandler() {
		DefaultMethodSecurityExpressionHandler expressionHandler =
				new DefaultMethodSecurityExpressionHandler();
		expressionHandler.setPermissionEvaluator(permissionEvaluator);
		return expressionHandler;
	}

	@Bean
	public PermissionEvaluator permissionEvaluator(@Lazy SecurityService securityService) {
		return new PermissionEvaluator() {
			@Override
			public boolean hasPermission(Authentication authentication, Object role, Object branchPath) {
				if (branchPath == null) {
					throw new SecurityException("Branch path is null, can not ascertain roles.");
				}
				try {
					return securityService.currentUserHasRoleOnBranch((String) role, BranchPathUriUtil.decodePath((String) branchPath));
				} catch (RestClientException e) {
					throw new AccessDeniedException("Could not ascertain user roles: Failed to communication with Snowstorm.", e);
				}
			}

			@Override
			public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) {
				return false;
			}
		};
	}
}


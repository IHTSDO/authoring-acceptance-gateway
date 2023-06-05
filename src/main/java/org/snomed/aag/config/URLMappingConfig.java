package org.snomed.aag.config;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriRewriteFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class URLMappingConfig {
	/**
	 * Instantiate a bean which allows for endpoints within this service to contain a branch path.
	 *
	 * @return A bean which allows for endpoints within this service to contain a branch path.
	 */
	@Bean
	public FilterRegistrationBean<BranchPathUriRewriteFilter> getUrlRewriteFilter() {
		return new FilterRegistrationBean<>(new BranchPathUriRewriteFilter(getPatternStrings()));
	}

	/**
	 * Return endpoints which have the branch path present in their path.
	 *
	 * @return Endpoints which have the branch path present in their path.
	 */
	public String[] getPatternStrings() {
		return new String[]{
				"/criteria/(.*)",
				"/acceptance/(.*)/item/.*/accept",
				"/acceptance/(.*)",
				// negation here, otherwise 'validation-rules' or 'item' gets interpreted as a branch name
				"/whitelist-items/(((?!validation-rules).)((?!item).)(.*))",
				"/criteria-items/(.*)",
				"/admin/criteria/(.*)/accept"
		};
	}
}

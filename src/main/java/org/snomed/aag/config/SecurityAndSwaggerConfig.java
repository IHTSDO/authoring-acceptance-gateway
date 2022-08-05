package org.snomed.aag.config;

import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.snomed.aag.rest.security.AccessDeniedExceptionHandler;
import org.snomed.aag.rest.security.RequiredRoleFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;

import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@EnableWebSecurity
public class SecurityAndSwaggerConfig extends WebSecurityConfigurerAdapter {

	@Autowired(required = false)
	private BuildProperties buildProperties;

	@Value("${ims-security.required-role}")
	private String requiredRole;

	private final String[] excludedUrlPatterns = {
			"/version",
			"/swagger-ui.html",
			"/swagger-resources/**",
			"/v2/api-docs",
			"/webjars/springfox-swagger-ui/**"
	};

	@Bean
	public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
		DefaultHttpFirewall firewall = new DefaultHttpFirewall();
		firewall.setAllowUrlEncodedSlash(true);
		return firewall;
	}

	@Override
	public void configure(WebSecurity web) {
		web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();// lgtm [java/spring-disabled-csrf-protection]

		http.addFilterBefore(new RequestHeaderAuthenticationDecorator(), FilterSecurityInterceptor.class);
		http.addFilterAt(new RequiredRoleFilter(requiredRole, excludedUrlPatterns), FilterSecurityInterceptor.class);

		http.authorizeRequests()
				.antMatchers(excludedUrlPatterns).permitAll()
				.anyRequest().authenticated()
				// Handles AccessDeniedException thrown within Spring Security filter chain, i.e. before the request reaches any controller
				.and().exceptionHandling().accessDeniedHandler(new AccessDeniedExceptionHandler())
				.and().httpBasic();
	}

	@Bean
	// Swagger config
	public Docket api() {
		Docket docket = new Docket(DocumentationType.SWAGGER_2);
		final String version = buildProperties != null ? buildProperties.getVersion() : "DEV";
		docket.apiInfo(new ApiInfo(
				"Authoring Acceptance Gateway",
				"Microservice to ensure service acceptance criteria are met before content promotion within the SNOMED CT Authoring Platform.",
				version,
				null,
				new Contact("SNOMED International", "https://github.com/IHTSDO/authoring-acceptance-gateway", null),
				"Apache 2.0", "http://www.apache.org/licenses/LICENSE-2.0"));
		ApiSelectorBuilder apiSelectorBuilder = docket.select();

		apiSelectorBuilder
				.apis(RequestHandlerSelectors.any());

		// Don't show the error or root endpoints in swagger
		apiSelectorBuilder
				.paths(not(regex("/error")))
				.paths(not(regex("/")));

		return apiSelectorBuilder.build();
	}

}

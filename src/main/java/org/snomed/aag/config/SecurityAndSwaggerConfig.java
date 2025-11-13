package org.snomed.aag.config;

import com.google.common.base.Strings;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.snomed.aag.rest.security.AccessDeniedExceptionHandler;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

@Configuration
@EnableWebSecurity
public class SecurityAndSwaggerConfig {

	@Autowired(required = false)
	private BuildProperties buildProperties;

	@Value("${ims-security.required-role}")
	private String requiredRole;

	private final String[] excludedUrlPatterns = {
			"/version",
			"/swagger-ui/**",
			"/v3/api-docs/**"
	};

	@Bean
	public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
		DefaultHttpFirewall firewall = new DefaultHttpFirewall();
		firewall.setAllowUrlEncodedSlash(true);
		return firewall;
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);// lgtm [java/spring-disabled-csrf-protection]

		http.addFilterBefore(new RequestHeaderAuthenticationDecorator(), AuthorizationFilter.class);

		if (!Strings.isNullOrEmpty(requiredRole)) {
			http.authorizeHttpRequests(c -> c
					.requestMatchers(excludedUrlPatterns).permitAll()
					.anyRequest().hasAuthority(requiredRole));
		} else {
			http.authorizeHttpRequests(c -> c
					.requestMatchers(excludedUrlPatterns).permitAll()
					.anyRequest().authenticated());
		}

		http.exceptionHandling(c -> c.accessDeniedHandler(new AccessDeniedExceptionHandler()));

		return http.build();
	}

	@Bean
	public GroupedOpenApi apiDocs() {
		return GroupedOpenApi.builder()
				.group("authoring-acceptance-gateway")
				.packagesToScan("org.snomed.aag.rest")
				// Don't show the error or root endpoints in Swagger
				.pathsToExclude("/error", "/")
				.build();
	}

	@Bean
	public GroupedOpenApi springActuatorApi() {
		return GroupedOpenApi.builder()
				.group("actuator")
				.packagesToScan("org.springframework.boot.actuate")
				.pathsToMatch("/actuator/**")
				.build();
	}

	@Bean
	public OpenAPI apiInfo() {
		final String version = buildProperties != null ? buildProperties.getVersion() : "DEV";
		return new OpenAPI()
				.info(new Info()
						.title("Authoring Acceptance Gateway")
						.description("Microservice to ensure service acceptance criteria are met before content promotion within the SNOMED CT Authoring Platform.")
						.version(version)
						.contact(new Contact().name("SNOMED International").url("https://www.snomed.org"))
						.license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0")))
				.externalDocs(new ExternalDocumentation()
						.description("See more about Authoring Acceptance Gateway in GitHub")
						.url("https://github.com/IHTSDO/authoring-acceptance-gateway"));
	}
}

package org.snomed.aag.config;

import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.snomed.aag.rest.security.RequestHeaderAuthenticationDecoratorWithRequiredRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
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

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();// lgtm [java/spring-disabled-csrf-protection]

		http
				.authorizeRequests()
				.anyRequest().permitAll();
	}

	@Bean
	// Swagger config
	public Docket api() {
		Docket docket = new Docket(DocumentationType.SWAGGER_2);
		final String version = buildProperties != null ? buildProperties.getVersion() : "DEV";
		docket.apiInfo(new ApiInfo("Authoring Acceptance Gateway", "Microservice to ensure service acceptance criteria are met before content promotion within the SNOMED CT Authoring Platform.", version, null,
				new Contact("SNOMED International", "https://github.com/IHTSDO/authoring-acceptance-gateway", null), "Apache 2.0", "http://www.apache.org/licenses/LICENSE-2.0"));
		ApiSelectorBuilder apiSelectorBuilder = docket.select();

		apiSelectorBuilder
				.apis(RequestHandlerSelectors.any());

		// Don't show the error or root endpoints in swagger
		apiSelectorBuilder
				.paths(not(regex("/error")))
				.paths(not(regex("/")));

		return apiSelectorBuilder.build();
	}

	@Bean
	public FilterRegistrationBean<RequestHeaderAuthenticationDecorator> getSingleSignOnFilter() {
		FilterRegistrationBean<RequestHeaderAuthenticationDecorator> filterRegistrationBean = new FilterRegistrationBean<>(
				new RequestHeaderAuthenticationDecorator());
		filterRegistrationBean.setOrder(1);
		return filterRegistrationBean;
	}

	@Bean
	public FilterRegistrationBean<RequestHeaderAuthenticationDecoratorWithRequiredRole> getRequiredRoleFilter(@Value("${ims-security.required-role}") String requiredRole) {
		FilterRegistrationBean<RequestHeaderAuthenticationDecoratorWithRequiredRole> filterRegistrationBean = new FilterRegistrationBean<>(
				new RequestHeaderAuthenticationDecoratorWithRequiredRole(requiredRole)
						.addExcludedPath("/webjars/springfox-swagger-ui")
		);
		filterRegistrationBean.setOrder(2);
		return filterRegistrationBean;
	}

}

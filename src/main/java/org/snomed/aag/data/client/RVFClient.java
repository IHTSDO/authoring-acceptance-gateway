package org.snomed.aag.data.client;

import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

public class RVFClient {
    private final RestTemplate restTemplate;

    public RVFClient(String authToken) {
        this.restTemplate = getNewRestTemplate();
        this.addInterceptorToRestTemplate(restTemplate, getHeaders(authToken));
    }

    public String getValidationReport(String reportAbsoluteUrl) {
        return restTemplate.getForObject(reportAbsoluteUrl, String.class);
    }

    private RestTemplate getNewRestTemplate() {
        return new RestTemplateBuilder()
                .errorHandler(new ExpressiveErrorHandler())
                .build();
    }

    private HttpHeaders getHeaders(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", authToken);
        return headers;
    }

    private void addInterceptorToRestTemplate(RestTemplate restTemplate, HttpHeaders headers) {
        //Add a ClientHttpRequestInterceptor to the RestTemplate to add cookies as required
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().addAll(headers);
            return execution.execute(request, body);
        });
    }
}


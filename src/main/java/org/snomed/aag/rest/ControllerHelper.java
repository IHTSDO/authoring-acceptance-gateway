package org.snomed.aag.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

public class ControllerHelper {

	static ResponseEntity<Void> getCreatedResponse(String id) {
		return new ResponseEntity<>(getCreatedLocationHeaders(id), HttpStatus.CREATED);
	}

	static HttpHeaders getCreatedLocationHeaders(String id) {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
		HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();

		String requestUrl = request.getRequestURL().toString();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(ServletUriComponentsBuilder.fromHttpUrl(requestUrl).path("/{id}").buildAndExpand(id).toUri());
		return httpHeaders;
	}
}

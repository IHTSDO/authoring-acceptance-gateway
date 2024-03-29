package org.snomed.aag.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.services.NotFoundException;
import org.snomed.aag.data.services.ServiceRuntimeException;
import org.springframework.data.elasticsearch.RestStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestControllerAdvice {

	private static final Logger logger = LoggerFactory.getLogger(RestControllerAdvice.class);

	@ExceptionHandler({
			IllegalArgumentException.class,
			IllegalStateException.class,
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class,
			MethodArgumentNotValidException.class,
			MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class
	})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public Map<String, Object> handleIllegalArgumentException(Exception exception) {
		HashMap<String, Object> result = new HashMap<>();
		result.put("error", HttpStatus.BAD_REQUEST);
		result.put("message", exception.getMessage());
		if (exception.getCause() != null) {
			result.put("causeMessage", exception.getCause().getMessage());
		}
		logger.info("bad request {}", exception.getMessage());
		logger.debug("bad request {}", exception.getMessage(), exception);
		return result;
	}

	@ExceptionHandler({NotFoundException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ResponseBody
	public Map<String,Object> handleNotFoundException(Exception exception) {
		HashMap<String, Object> result = new HashMap<>();
		result.put("error", HttpStatus.NOT_FOUND);
		result.put("message", exception.getMessage());
		logger.debug("Not Found {}", exception.getMessage(), exception);
		return result;
	}

	@ExceptionHandler(AccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	@ResponseBody
	public Map<String,Object> handleAccessDeniedException(AccessDeniedException exception) {
		HashMap<String, Object> result = new HashMap<>();
		result.put("error", HttpStatus.FORBIDDEN);
		result.put("message", exception.getMessage());
		return result;
	}

	@ExceptionHandler(RestStatusException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public Map<String, Object> handleElasticsearchStatusException(RestStatusException exception) {
		logger.error("Failed to run Elasticsearch query; returning {} to client.", HttpStatus.INTERNAL_SERVER_ERROR.value());
		logger.debug("ElasticsearchStatusException: " + exception.toString());
		HashMap<String, Object> result = new HashMap<>();
		result.put("error", HttpStatus.INTERNAL_SERVER_ERROR);
		result.put("message", exception.getMessage());
		return result;
	}

	@ExceptionHandler(ServiceRuntimeException.class)
	public ResponseEntity<Map<String, Object>> handleServiceRuntimeException(ServiceRuntimeException exception) {
		logger.info("ServiceRuntimeException caught; returning {} to client.", exception.getHttpStatus());
		logger.debug("ServiceRuntimeException", exception);
		Map<String, Object> result = new HashMap<>();
		result.put("error", exception.getHttpStatusCode());
		result.put("message", exception.getMessage());

		return ResponseEntity
				.status(exception.getHttpStatus())
				.body(result);
	}
}

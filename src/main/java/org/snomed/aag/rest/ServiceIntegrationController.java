package org.snomed.aag.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.pojo.CommitInformation;
import org.snomed.aag.data.pojo.ValidationInformation;
import org.snomed.aag.data.services.AcceptanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@Api(tags = "Service Integration")
@RequestMapping(value = "/integration", produces = "application/json")
public class ServiceIntegrationController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
    private AcceptanceService acceptanceService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

	@ApiOperation(value = "Receive commit information from Snowstorm.",
			notes = "This function is called by the Snowstorm Terminology server when a commit is made. " +
					"This information is used to perform automatic actions within this service like accepting or expiring acceptance items. "
	)
	@PostMapping("/snowstorm/commit")
	public ResponseEntity<Void> receiveCommitInformation(@RequestBody CommitInformation commitInformation) {
		final String username = SecurityUtil.getUsername();
		logger.info("Received commit information {} from user {}", commitInformation, username);

		final CommitInformation.CommitType commitType = commitInformation.getCommitType();
		if (commitType != CommitInformation.CommitType.PROMOTION) {
			// Prevent the processing of this call slowing down the snowstorm commit
			// All business logic within the service method
			final SecurityContext context = SecurityContextHolder.getContext();
			executorService.submit(() -> {
				SecurityContextHolder.setContext(context);// Security context brought across into new thread
				acceptanceService.processCommit(commitInformation);
			});
//		} else {
			// In FRI-54 we will block promotion if the branch criteria is not complete. This must be synchronous.
//			criteriaService.validatePromotion(commitInformation);
		}

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@ApiOperation(value = "Receive validation report information from Authoring Services.",
			notes = "This function is called by Authoring Services when an RVF validation completes. " +
					"This information may automatically accept a validation acceptance item. "
	)
	@PostMapping("/validation-complete")
	public ResponseEntity<Void> receiveValidation(@RequestBody ValidationInformation validationInformation) {
		final String username = SecurityUtil.getUsername();
		logger.info("Received validation information {} from user {}", validationInformation, username);

		// Prevent the processing of this call slowing down the snowstorm commit
		acceptanceService.processValidationAsync(validationInformation);

		return ResponseEntity.status(HttpStatus.OK).build();
	}

}

package org.snomed.aag.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.pojo.CommitInformation;
import org.snomed.aag.data.pojo.ValidationInformation;
import org.snomed.aag.data.services.AcceptanceService;
import org.snomed.aag.data.services.BranchSecurityService;
import org.snomed.aag.data.services.ProjectAcceptanceCriteriaService;
import org.snomed.aag.data.validators.CommitInformationValidator;
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
	private final CommitInformationValidator commitInformationValidator;
	private final AcceptanceService acceptanceService;
	private final ProjectAcceptanceCriteriaService projectAcceptanceCriteriaService;

	public ServiceIntegrationController(CommitInformationValidator commitInformationValidator, AcceptanceService acceptanceService,
										ProjectAcceptanceCriteriaService projectAcceptanceCriteriaService) {
		this.commitInformationValidator = commitInformationValidator;
		this.acceptanceService = acceptanceService;
		this.projectAcceptanceCriteriaService = projectAcceptanceCriteriaService;
	}

    private final ExecutorService executorService = Executors.newCachedThreadPool();

	@ApiOperation(value = "Receive commit information from Snowstorm.",
			notes = "This function is called by the Snowstorm Terminology server when a commit is made. " +
					"This information is used to perform automatic actions within this service like accepting or expiring acceptance items. "
	)
	@PostMapping("/snowstorm/commit")
	public ResponseEntity<?> receiveCommitInformation(@RequestBody CommitInformation commitInformation) {
		final String username = SecurityUtil.getUsername();
		logger.info("Received commit information {} from user {}", commitInformation, username);

		commitInformationValidator.validate(commitInformation);
		final CommitInformation.CommitType commitType = commitInformation.getCommitType();
		if (commitType != CommitInformation.CommitType.PROMOTION) {
			// Prevent the processing of this call slowing down the snowstorm commit
			// All business logic within the service method
			final SecurityContext context = SecurityContextHolder.getContext();
			executorService.submit(() -> {
				SecurityContextHolder.setContext(context);// Security context brought across into new thread
				acceptanceService.processCommit(commitInformation);
			});

			return ResponseEntity.status(HttpStatus.OK).build();
		} else {
			String sourceBranchPath = commitInformation.getSourceBranchPath();
			ProjectAcceptanceCriteria projectAcceptanceCriteria = projectAcceptanceCriteriaService.findEffectiveCriteriaWithMandatoryItems(sourceBranchPath);
			if (projectAcceptanceCriteria == null) {
				String message = String.format("No Project Acceptance Criteria found for branch %s. Returning %s.", sourceBranchPath, HttpStatus.NO_CONTENT);
				logger.info(message);
				return ResponseEntity
						.status(HttpStatus.NO_CONTENT)
						.body(message);
			}

			boolean pacComplete = projectAcceptanceCriteriaService.incrementIfComplete(projectAcceptanceCriteria, sourceBranchPath);
			if (pacComplete) {
				logger.info("Project Acceptance Criteria for {} is complete. Promotion is recommended.", sourceBranchPath);
				return ResponseEntity.status(HttpStatus.OK).build();
			} else {
				logger.info("Project Acceptance Criteria for {} is incomplete. Promotion is not recommended.", sourceBranchPath);
				return ResponseEntity.status(HttpStatus.CONFLICT).build();
			}
		}
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

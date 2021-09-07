package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.CriteriaItemSignOffFactory;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.pojo.CommitInformation;
import org.snomed.aag.data.pojo.ValidationInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AcceptanceService {

	@Autowired
	private CriteriaItemService criteriaItemService;

	@Autowired
	private CriteriaItemSignOffService criteriaItemSignOffService;

	@Autowired
	private BranchSecurityService securityService;

	@Autowired
	private ProjectAcceptanceCriteriaService criteriaService;

	@Autowired
	private ValidationService validationService;

	@Autowired
	private CriteriaItemSignOffFactory criteriaItemSignOffFactory;

	private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceService.class);

	/**
	 * Return created CriteriaItemSignOff for given branchPath. The latest ProjectAcceptanceCriteria for the given branchPath, if present,
	 * will have all Criteria, regardless of whether any Criteria is manual, marked as complete.
	 *
	 * @param branchPath Branch path for Criteria to mark complete.
	 * @return Created CriteriaItemSignOff for given branchPath.
	 * @throws RestClientException When Branch does not exist.
	 */
	public Set<CriteriaItemSignOff> acceptAllItemsForBranch(String branchPath) throws RestClientException {
		// Verify branch
		Branch branch = securityService.getBranchOrThrow(branchPath);

		// Verify ProjectAcceptanceCriteria & CriteriaItems
		ProjectAcceptanceCriteria projectAcceptanceCriteria = getPACOrThrow(branchPath);
		Set<String> criteriaIdentifiers = getCriteriaIdentifiersOrThrow(projectAcceptanceCriteria, branchPath);

		// Only create CriteriaItems if they exist
		Set<CriteriaItem> criteriaItemsToAccept = new HashSet<>();
		for (String criteriaIdentifier : criteriaIdentifiers) {
			CriteriaItem criteriaItem = criteriaItemService.findByIdOrThrow(criteriaIdentifier);
			securityService.verifyBranchRole(branchPath, criteriaItem.getRequiredRole());
			criteriaItemsToAccept.add(criteriaItem);
		}

		// Verification complete; add record(s)
		return criteriaItemSignOffService.createAll(criteriaItemsToAccept, branchPath, projectAcceptanceCriteria.getProjectIteration(), branch.getHeadTimestamp(), projectAcceptanceCriteria);
	}

	/**
	 * The latest ProjectAcceptanceCriteria for the given branchPath, if present, will have all Criteria, regardless of whether any Criteria is manual,
	 * marked as incomplete.
	 *
	 * @param branchPath Branch path for Criteria to mark incomplete.
	 * @throws RestClientException When Branch does not exist.
	 */
	public void rejectAllItemsForBranch(String branchPath) throws RestClientException {
		// Verify branch
		securityService.getBranchOrThrow(branchPath);

		// Verify ProjectAcceptanceCriteria & CriteriaItems
		ProjectAcceptanceCriteria projectAcceptanceCriteria = getPACOrThrow(branchPath);
		Set<String> criteriaIdentifiers = getCriteriaIdentifiersOrThrow(projectAcceptanceCriteria, branchPath);

		// Verification complete; remove record(s)
		criteriaItemSignOffService.deleteFrom(criteriaIdentifiers, branchPath, projectAcceptanceCriteria.getProjectIteration(), projectAcceptanceCriteria);
	}

	/**
	 * Return created CriteriaItemSignOff for given branchPath and CriteriaItem. The latest ProjectAcceptanceCriteria for the given branchPath, if present,
	 * will have its matching CriteriaItem marked as complete.
	 *
	 * @param branchPath Branch path for Criteria to mark complete.
	 * @param itemId     Identifier for Criteria to mark complete.
	 * @return Created CriteriaItemSignOff for given branchPath and CriteriaItem.
	 * @throws RestClientException When Branch does not exist.
	 */
	public CriteriaItemSignOff acceptItem(String branchPath, String itemId) throws RestClientException {
		//Verify request.
		ProjectAcceptanceCriteria projectAcceptanceCriteria = getProjectAcceptanceCriteriaForAcceptRejectRequestOrThrow(branchPath, itemId);

		//Verification complete; add record.
		CriteriaItemSignOff criteriaItemSignOff = criteriaItemSignOffFactory.create(
				itemId,
				branchPath,
				securityService.getBranchOrThrow(branchPath).getHeadTimestamp(),
				projectAcceptanceCriteria.getProjectIteration(),
				SecurityUtil.getUsername(),
				projectAcceptanceCriteria);
		CriteriaItemSignOff savedCriteriaItemSignOff = criteriaItemSignOffService.create(projectAcceptanceCriteria, criteriaItemSignOff);
		LOGGER.info("Created CriteriaItemSignOff {} for {}.", savedCriteriaItemSignOff.getId(), itemId);
		return savedCriteriaItemSignOff;
	}

	/**
	 * The latest ProjectAcceptanceCriteria for the given branchPath, if present, will have its matching CriteriaItem marked as incomplete.
	 *
	 * @param branchPath Branch path for Criteria to mark incomplete.
	 * @param itemId     Identifier for Criteria to mark incomplete.
	 */
	public void rejectOrThrow(String branchPath, String itemId) {
		//Verify request.
		ProjectAcceptanceCriteria projectAcceptanceCriteria = getProjectAcceptanceCriteriaForAcceptRejectRequestOrThrow(branchPath, itemId);

		//Verification complete; remove record.
		Integer projectIteration = projectAcceptanceCriteria.getProjectIteration();
		boolean deleted = criteriaItemSignOffService.deleteByCriteriaItemIdAndBranchPathAndProjectIteration(itemId, branchPath, projectAcceptanceCriteria.getProjectIteration(), projectAcceptanceCriteria);
		if (!deleted) {
			String message = String.format("Cannot delete %s for branch %s and project iteration %d", itemId, branchPath, projectIteration);
			throw new ServiceRuntimeException(message, HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Use a Snowstorm commit to automatically accept classification items and reject any automatically expiring items
	 * @param commitInformation Commit information including branch path and metadata.
	 */
	public void processCommit(CommitInformation commitInformation) {
		final String sourceBranchPath = commitInformation.getSourceBranchPath();
		final ProjectAcceptanceCriteria criteria = criteriaService.findByBranchPathWithRelevantCriteriaItems(sourceBranchPath, true);
		if (criteria == null) {
			LOGGER.info("ProjectAcceptanceCriteria not found for branch; nothing to process.");
			return;
		}

		Set<CriteriaItem> criteriaItems = getCriteriaItemsAndMarkSignOff(branchPathReceivingChanges, criteria);
		Set<String> itemsToReject = getCriteriaItemsToUnaccept(criteriaItems);
		Integer projectIteration = criteria.getProjectIteration();
		if (commitInformation.isContent()) {
			Set<String> itemsAlreadyAccepted = getCriteriaItemsAlreadyAccepted(criteriaItems, itemsToReject);
			Set<String> itemsShouldBeAccepted = getCriteriaItemsThatShouldBeAccepted(commitInformation, criteria, branchPathReceivingChanges, criteriaItems);
			persistItemsShouldBeAccepted(itemsShouldBeAccepted, itemsAlreadyAccepted, branchPathReceivingChanges, commitInformation.getHeadTime(), projectIteration, criteria);
		}

		if (!itemsToReject.isEmpty()) {
			LOGGER.info("Rejecting items {} for branch {}, iteration {}", itemsToReject, branchPathReceivingChanges, projectIteration);
			criteriaItemSignOffService.deleteFrom(itemsToReject, branchPathReceivingChanges, projectIteration, criteria);
		} else {
			LOGGER.info("No Criteria Items to reject.");
		}

		persistItemsShouldBeAccepted(itemsShouldBeAccepted, acceptedItems, branchPath, commitInformation.getHeadTime(), criteria.getProjectIteration());
	}

	/**
	 * Mark relevant CriteriaItem as complete if the corresponding validation report has no errors.
	 *
	 * @param validationInformation Validation information including branchPath and URL to report.
	 */
	@Async
	public void processValidationAsync(ValidationInformation validationInformation) {
		try {
			final String branchPath = validationInformation.getBranchPath();
			final ProjectAcceptanceCriteria criteria = criteriaService.findByBranchPathWithRelevantCriteriaItems(branchPath, true);
			if (criteria == null) {
				return;
			}

			final Branch branch = securityService.getBranchOrThrow(branchPath);
			if (validationService.isReportClean(validationInformation.getReportUrl(), branch.getHeadTimestamp(), branchPath)) {

				final Set<CriteriaItem> items = criteriaService.findItemsAndMarkSignOff(criteria, branchPath);

				Set<String> itemsShouldBeAccepted = items.stream()
						.filter(item ->
								(item.getId().equals(CriteriaItem.PROJECT_VALIDATION_CLEAN) && criteria.isBranchProjectLevel(branchPath)) ||
										(item.getId().equals(CriteriaItem.TASK_VALIDATION_CLEAN) && criteria.isBranchTaskLevel(branchPath))
						)
						.map(CriteriaItem::getId)
						.collect(Collectors.toSet());

				persistItemsShouldBeAccepted(itemsShouldBeAccepted, getAcceptedItemIds(items), branchPath, branch.getHeadTimestamp(), criteria.getProjectIteration(), criteria);
			}

		} catch (RestClientException e) {
			LOGGER.error("Failed to handle validation complete notification.", e);
		}
	}

	private Set<String> getAcceptedItemIds(Set<CriteriaItem> items) {
		return items.stream().filter(CriteriaItem::isComplete).map(CriteriaItem::getId).collect(Collectors.toSet());
	}

	private void persistItemsShouldBeAccepted(Set<String> itemsShouldBeAccepted, Set<String> itemsAlreadyAccepted, String branchPath, long branchHeadTime,
											  Integer projectIteration, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		// Only accept items which are not already accepted
		Set<String> toPersist = new HashSet<>(itemsShouldBeAccepted);
		toPersist.removeAll(itemsAlreadyAccepted);

		if (!toPersist.isEmpty()) {
			LOGGER.info("Signing off items {} for branch {}, iteration {}", toPersist, branchPath, projectIteration);
			criteriaItemSignOffService.createFrom(toPersist, branchPath, projectIteration, branchHeadTime, projectAcceptanceCriteria);
		} else {
			LOGGER.info("No Criteria Items to accept.");
		}
	}

	private boolean userHasRole(CriteriaItem item, Set<String> branchRoles) {
		final boolean contains = item.getRequiredRole() == null || branchRoles.contains(item.getRequiredRole());
		if (!contains) {
			LOGGER.debug("User does not have sufficient roles to change item {}", item.getId());
		}
		return contains;
	}

	private ProjectAcceptanceCriteria getProjectAcceptanceCriteriaForAcceptRejectRequestOrThrow(String branchPath, String criteriaItemId) {
		//Verify CriteriaItems
		CriteriaItem criteriaItem = criteriaItemService.findByIdOrThrow(criteriaItemId);
		criteriaItemService.verifyManual(criteriaItem, true);

		// Check that the user making the request has the required role on the branch
		securityService.verifyBranchRole(branchPath, criteriaItem.getRequiredRole());

		//Verify ProjectAcceptanceCriteria
		ProjectAcceptanceCriteria projectAcceptanceCriteria = criteriaService.findByBranchPathWithRelevantCriteriaItems(branchPath, true);
		if (projectAcceptanceCriteria == null) {
			String message = String.format("Cannot find Acceptance Criteria for %s.", branchPath);
			throw new ServiceRuntimeException(message, HttpStatus.NOT_FOUND);
		}

		boolean itemNotConfigured = !projectAcceptanceCriteria.getAllCriteriaIdentifiers().contains(criteriaItemId);
		if (itemNotConfigured) {
			String message = String.format("Branch %s does not have %s included in its Acceptance Criteria, and can, therefore, not be accepted/rejected.", branchPath,
					criteriaItemId);
			throw new ServiceRuntimeException(message, HttpStatus.BAD_REQUEST);
		}

		return projectAcceptanceCriteria;
	}

	private ProjectAcceptanceCriteria getPACOrThrow(String branchPath) {
		ProjectAcceptanceCriteria projectAcceptanceCriteria = criteriaService.findByBranchPathWithRelevantCriteriaItems(branchPath, true);
		if (projectAcceptanceCriteria == null) {
			String message = String.format("Cannot find Acceptance Criteria for %s.", branchPath);
			throw new ServiceRuntimeException(message, HttpStatus.NOT_FOUND);
		}

		return projectAcceptanceCriteria;
	}

	private Set<String> getCriteriaIdentifiersOrThrow(ProjectAcceptanceCriteria projectAcceptanceCriteria, String branchPath) {
		Set<String> criteriaIdentifiers = projectAcceptanceCriteria.getAllCriteriaIdentifiers();
		if (criteriaIdentifiers.isEmpty()) {
			String message = String.format("Acceptance Criteria for %s has no Criteria Items configured.", branchPath);
			throw new ServiceRuntimeException(message, HttpStatus.CONFLICT);
		}

		return criteriaIdentifiers;
	}
}

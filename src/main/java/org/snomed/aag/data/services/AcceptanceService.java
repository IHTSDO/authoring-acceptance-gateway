package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.pojo.CommitInformation;
import org.snomed.aag.rest.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AcceptanceService {

	@Autowired
	private CriteriaItemService criteriaItemService;

	@Autowired
	private CriteriaItemSignOffService criteriaItemSignOffService;

	@Autowired
	private SecurityService securityService;

	@Autowired
	private ProjectAcceptanceCriteriaService criteriaService;

	private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceService.class);

	public CriteriaItemSignOff acceptItem(String branchPath, String itemId) throws RestClientException {
		//Verify request.
		ProjectAcceptanceCriteria projectAcceptanceCriteria = getProjectAcceptanceCriteriaForAcceptRejectRequestOrThrow(branchPath, itemId);

		//Verification complete; add record.
		CriteriaItemSignOff savedCriteriaItemSignOff = criteriaItemSignOffService.create(new CriteriaItemSignOff(
				itemId,
				branchPath,
				securityService.getBranchOrThrow(branchPath).getHeadTimestamp(), projectAcceptanceCriteria.getProjectIteration(),
				SecurityUtil.getUsername()
		));
		LOGGER.info("Created CriteriaItemSignOff {} for {}.", savedCriteriaItemSignOff.getId(), itemId);
		return savedCriteriaItemSignOff;
	}

	public void rejectOrThrow(String branchPath, String itemId) {
		//Verify request.
		ProjectAcceptanceCriteria projectAcceptanceCriteria = getProjectAcceptanceCriteriaForAcceptRejectRequestOrThrow(branchPath, itemId);

		//Verification complete; remove record.
		Integer projectIteration = projectAcceptanceCriteria.getProjectIteration();
		boolean deleted = criteriaItemSignOffService.deleteByCriteriaItemIdAndBranchPathAndProjectIteration(itemId, branchPath, projectIteration);
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
		final String branchPath = commitInformation.getPath();
		final ProjectAcceptanceCriteria criteria = criteriaService.findEffectiveCriteriaWithMandatoryItems(branchPath);
		if (criteria == null) {
			// No criteria for branch, nothing to do.
			return;
		}

		boolean classified = commitInformation.isClassified();
		boolean projectLevel = criteria.getBranchPath().equals(branchPath);
		boolean taskLevel = criteria.getBranchPath().equals(PathUtil.getParentPath(branchPath));

		final Set<CriteriaItem> items = criteriaService.findItemsAndMarkSignOff(criteria, branchPath);
		final Set<String> branchRoles = securityService.getBranchRoles(branchPath);

		// Includes role check
		Set<String> itemsShouldBeAccepted = items.stream()
				.filter(item ->
						(item.getId().equals(CriteriaItem.PROJECT_CLEAN_CLASSIFICATION) && projectLevel && classified && userHasRole(item, branchRoles)) ||
								(item.getId().equals(CriteriaItem.TASK_CLEAN_CLASSIFICATION) && taskLevel && classified && userHasRole(item, branchRoles))
				)
				.map(CriteriaItem::getId)
				.collect(Collectors.toSet());

		// Intentionally does not include a user role check
		Set<String> itemsToUnaccept = items.stream()
				.filter(item -> item.isExpiresOnCommit() && item.isComplete())
				.map(CriteriaItem::getId)
				.collect(Collectors.toSet());

		itemsToUnaccept.removeAll(itemsShouldBeAccepted);

		Set<String> itemsToAccept = items.stream()
				.filter(item -> !item.isComplete() && itemsShouldBeAccepted.contains(item.getId()))
				.map(CriteriaItem::getId)
				.collect(Collectors.toSet());

		if (!itemsToUnaccept.isEmpty()) {
			criteriaItemSignOffService.deleteItems(itemsToUnaccept, branchPath, criteria.getProjectIteration());
		}
		if (!itemsToAccept.isEmpty()) {
			criteriaItemSignOffService.doCreateItems(itemsToAccept, branchPath, commitInformation.getHeadTime(), criteria.getProjectIteration());
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
		ProjectAcceptanceCriteria projectAcceptanceCriteria = criteriaService.findEffectiveCriteriaWithMandatoryItems(branchPath);
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
}

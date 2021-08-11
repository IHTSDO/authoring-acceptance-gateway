package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.repositories.CriteriaItemRepository;
import org.snomed.aag.data.repositories.ProjectAcceptanceCriteriaRepository;
import org.snomed.aag.rest.util.MetadataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.lang.String.format;

@Service
public class CriteriaItemService {
	private static final String INVALID_PARAMETERS = "Invalid parameters.";

	@Autowired
	private CriteriaItemRepository repository;

	@Autowired
	private ProjectAcceptanceCriteriaRepository acceptanceCriteriaRepository;

	private static void verifyParams(CriteriaItem criteriaItem) {
		if (criteriaItem == null) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	private static void verifyParams(PageRequest pageRequest) {
		if (pageRequest == null) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	private static void verifyParams(String id) {
		if (id == null) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	private static void verifyParams(Collection<String> criteriaItemIdentifiers) {
		if (criteriaItemIdentifiers == null || criteriaItemIdentifiers.isEmpty()) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	private static void verifyParams(AuthoringLevel authoringLevel) {
		if (authoringLevel == null) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	private void verifyParams(Set<CriteriaItem> criteriaItems, Branch branch) {
		if (criteriaItems == null || branch == null) {
			throw new IllegalArgumentException(INVALID_PARAMETERS);
		}
	}

	/**
	 * Save entry in database.
	 *
	 * @param criteriaItem Entry to save in database.
	 * @throws IllegalArgumentException If argument is invalid.
	 */
	public void create(CriteriaItem criteriaItem) {
		verifyParams(criteriaItem);
		repository.save(criteriaItem);
	}

	/**
	 * Find entries in database matching page request, and return as new page.
	 *
	 * @param pageRequest Page configuration for database query.
	 * @return Entries in database matching page request.
	 * @throws IllegalArgumentException If argument is invalid.
	 */
	public Page<CriteriaItem> findAll(PageRequest pageRequest) {
		verifyParams(pageRequest);
		return repository.findAll(pageRequest);
	}

	/**
	 * Find entry in database with matching id field.
	 *
	 * @param id Field to match in query.
	 * @return Entry in database with matching id field.
	 * @throws IllegalArgumentException If argument is invalid.
	 */
	public CriteriaItem findByIdOrThrow(String id) {
		verifyParams(id);
		final Optional<CriteriaItem> itemOptional = repository.findById(id);
		if (!itemOptional.isPresent()) {
			throw new NotFoundException(format("Criteria Item with id '%s' not found.", id));
		}
		return itemOptional.get();
	}

	/**
	 * Find entries in database with matching identifiers.
	 *
	 * @param criteriaItemIdentifiers Potential identifiers to match in query.
	 * @return Entries in database with matching identifiers.
	 * @throws IllegalArgumentException If argument is invalid.
	 */
	public Set<CriteriaItem> findAllByIdentifiers(Collection<String> criteriaItemIdentifiers) {
		verifyParams(criteriaItemIdentifiers);
		return repository.findAllByIdIn(criteriaItemIdentifiers);
	}

	/**
	 * Find entries in database with matching mandatory and authoringLevel fields.
	 *
	 * @param mandatory      Field to match in query.
	 * @param authoringLevel Field to match in query.
	 * @return Entries in database with matching mandatory and authoringLevel fields.
	 * @throws IllegalArgumentException If arguments are invalid.
	 */
	public List<CriteriaItem> findAllByMandatoryAndAuthoringLevel(boolean mandatory, AuthoringLevel authoringLevel) {
		verifyParams(authoringLevel);
		return repository.findAllByMandatoryAndAuthoringLevel(mandatory, authoringLevel);
	}

	/**
	 * Return entries from store where the enabledByFlag field has a value present in
	 * given collection.
	 *
	 * @param enabledByFlag Collection of potential matches.
	 * @return Entries from store where the enabledByFlag field has a value present in given collection.
	 */
	public Set<CriteriaItem> findAllByEnabledByFlag(Set<String> enabledByFlag) {
		if (enabledByFlag == null || enabledByFlag.isEmpty()) {
			return Collections.emptySet();
		}

		return repository.findAllByEnabledByFlagIn(enabledByFlag);
	}

	/**
	 * Replace entry in database with given CriteriaItem.
	 *
	 * @param criteriaItem Entry to replace the existing one with.
	 * @return Updated entry from database.
	 * @throws IllegalArgumentException If argument is invalid.
	 */
	public CriteriaItem update(CriteriaItem criteriaItem) {
		verifyParams(criteriaItem);
		return repository.save(criteriaItem);
	}

	/**
	 * Delete given CriteriaItem from database.
	 *
	 * @param criteriaItem Entry to delete from database.
	 * @throws IllegalArgumentException If argument is invalid.
	 * @throws ServiceRuntimeException  If CriteriaItem is used in ProjectAcceptanceCriteria.
	 */
	public void delete(CriteriaItem criteriaItem) {
		verifyParams(criteriaItem);
		final Page<ProjectAcceptanceCriteria> found = acceptanceCriteriaRepository.findAllBySelectedProjectCriteriaIdsOrSelectedTaskCriteriaIds(criteriaItem.getId(), criteriaItem.getId(), Constants.PAGE_OF_ONE);
		if (!found.isEmpty()) {
			throw new ServiceRuntimeException(format("Criteria can not be deleted, it is used in %s project criteria.", found.getTotalElements()), HttpStatus.CONFLICT);
		}

		repository.delete(criteriaItem);
	}

	/**
	 * Verify given CriteriaItem has the expected manual state.
	 *
	 * @param criteriaItem   CriteriaItem to verify.
	 * @param expectedManual Expected state of given CriteriaItem's manual state.
	 * @throws AccessDeniedException If CriteriaItem does not have expected state.
	 */
	public void verifyManual(CriteriaItem criteriaItem, boolean expectedManual) {
		if (criteriaItem.isManual() != expectedManual) {
			throw new AccessDeniedException("Criteria Item cannot be changed manually.");
		}
	}

	/**
	 * Remove entry from collection. If entry does not have a corresponding flag in Branch metadata
	 * or the corresponding flag in Branch metadata is false, then remove entry from collection.
	 *
	 * @param criteriaItems Collection to process
	 * @param branch        Branch to cross reference
	 */
	public void removeNonEnabled(Set<CriteriaItem> criteriaItems, Branch branch) {
		verifyParams(criteriaItems, branch);
		Set<String> authorFlags = MetadataUtil.getEnabledAuthorFlags(branch);
		if (!authorFlags.isEmpty()) {
			criteriaItems.removeIf(item -> item.getEnabledByFlag().stream().noneMatch(authorFlags::contains));
		}
	}
}

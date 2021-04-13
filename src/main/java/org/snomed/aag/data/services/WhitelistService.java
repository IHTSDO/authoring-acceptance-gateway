package org.snomed.aag.data.services;

import org.ihtsdo.sso.integration.SecurityUtil;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.repositories.WhitelistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
public class WhitelistService {

	private static final Comparator<WhitelistItem> WHITELIST_ITEM_COMPARATOR = Comparator.comparing(WhitelistItem::getValidationRuleId)
																		.thenComparing(WhitelistItem::getConceptId)
																		.thenComparing(WhitelistItem::getComponentId)
																		.thenComparing(WhitelistItem::getAdditionalFields);
	@Autowired
	private WhitelistItemRepository repository;

	public Page<WhitelistItem> findAll(PageRequest pageRequest) {
		return repository.findAll(pageRequest);
	}

	public List<WhitelistItem> findAllByValidationRuleId(String validationRuleId) {
		return repository.findAllByValidationRuleIdIn(Collections.singleton(validationRuleId));
	}

	public List<WhitelistItem> findAllByValidationRuleIds(Set<String> validationRuleIds) {
		return repository.findAllByValidationRuleIdIn(validationRuleIds);
	}

	public List<WhitelistItem> validateWhitelistComponents(Set<WhitelistItem> whitelistItems) {
		if (CollectionUtils.isEmpty(whitelistItems)) {
			return Collections.EMPTY_LIST;
		}

		List<WhitelistItem> validWhitelistItems = new ArrayList <>();
		Set<String> validationRuleIds = whitelistItems.stream().map(WhitelistItem::getValidationRuleId).collect(Collectors.toSet());
		List<WhitelistItem> persistedWhitelistItems = repository.findAllByValidationRuleIdIn(validationRuleIds);
		for (WhitelistItem whitelistItemTobeCompared : whitelistItems) {
			for (WhitelistItem persistedWhitelistItem : persistedWhitelistItems) {
				if (WHITELIST_ITEM_COMPARATOR.compare(whitelistItemTobeCompared, persistedWhitelistItem) == 0) {
					validWhitelistItems.add(whitelistItemTobeCompared);
					break;
				}
			}
		}

		return validWhitelistItems;
	}

	public WhitelistItem findOrThrow(String id) {
		final Optional<WhitelistItem> itemOptional = repository.findById(id);
		if (!itemOptional.isPresent()) {
			throw new NotFoundException(format("Whitelist Item with id '%s' not found.", id));
		}
		return itemOptional.get();
	}

	public WhitelistItem create(WhitelistItem whitelistItem) {
		whitelistItem.setId(UUID.randomUUID().toString());
		whitelistItem.setUserId(SecurityUtil.getUsername());
		whitelistItem.setCreationDate(new Date());
		return repository.save(whitelistItem);
	}

	public WhitelistItem update(WhitelistItem whitelistItem) {
		return repository.save(whitelistItem);
	}

	public void delete(WhitelistItem item) {
		repository.delete(item);
	}


}

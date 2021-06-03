package org.snomed.aag.data.services;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.repositories.WhitelistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class WhitelistService {

	private static final Comparator<WhitelistItem> WHITELIST_ITEM_COMPARATOR = Comparator.comparing(WhitelistItem::getValidationRuleId)
																		.thenComparing(WhitelistItem::getConceptId)
																		.thenComparing(WhitelistItem::getComponentId)
																		.thenComparing(WhitelistItem::getAdditionalFields);
	@Autowired
	private WhitelistItemRepository repository;

	@Autowired
	private ElasticsearchRestTemplate elasticsearchRestTemplate;

	private static Date getDefaultDateIfNull(Date date) {
		if (date == null) {
			try {
				date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2021-01-01 00:00:00");
			} catch (ParseException e) {
				// Should never happen as source is hard-coded.
			}
		}

		return date;
	}

	public Page<WhitelistItem> findAll(PageRequest pageRequest) {
		return repository.findAll(pageRequest);
	}

	public List<WhitelistItem> findAllByValidationRuleId(String validationRuleId) {
		return repository.findAllByValidationRuleIdIn(Collections.singleton(validationRuleId));
	}

	public List<WhitelistItem> findAllByValidationRuleIds(Set<String> validationRuleIds) {
		return repository.findAllByValidationRuleIdIn(validationRuleIds);
	}

	public List<WhitelistItem> findAllByBranchAndMinimumCreationDate(String branchPath, Date date, boolean includeDescendants, PageRequest pageRequest) {
		date = getDefaultDateIfNull(date);
		BoolQueryBuilder branchQuery;
		if (includeDescendants) {
			branchQuery = boolQuery().should(termQuery(WhitelistItem.Fields.BRANCH, branchPath)).should(wildcardQuery(WhitelistItem.Fields.BRANCH, branchPath + "/*"));
		} else {
			branchQuery = boolQuery().must(termQuery(WhitelistItem.Fields.BRANCH, branchPath));
		}

		BoolQueryBuilder creationDateQuery = boolQuery().must(rangeQuery(WhitelistItem.Fields.CREATION_DATE).gte(date.getTime()));
		NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
				.withQuery(boolQuery()
						.must(creationDateQuery)
						.must(branchQuery)
				)
				.withPageable(pageRequest)
				.build();

		return elasticsearchRestTemplate.search(nativeSearchQuery, WhitelistItem.class)
				.stream()
				.map(SearchHit::getContent)
				.collect(Collectors.toList());
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

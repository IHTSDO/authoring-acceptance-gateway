package org.snomed.aag.data.services;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.snomed.aag.data.domain.WhitelistItem;
import org.snomed.aag.data.repositories.WhitelistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool;
import static java.lang.String.format;
import static org.snomed.aag.data.helper.QueryHelper.*;

@Service
public class WhitelistService {

	private static final Comparator<WhitelistItem> WHITELIST_ITEM_COMPARATOR = Comparator.comparing(WhitelistItem::getValidationRuleId)
			.thenComparing(WhitelistItem::getConceptId)
			.thenComparing(WhitelistItem::getComponentId)
			.thenComparing(WhitelistItem::getAdditionalFields);
	@Autowired
	private WhitelistItemRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

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
		return findAllByValidationRuleIds(Collections.singleton(validationRuleId));
	}

	public List<WhitelistItem> findAllByValidationRuleIds(Set<String> validationRuleIds) {
		NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
				.withQuery(bool(b -> b.must(termsQuery(WhitelistItem.Fields.VALIDATION_RULE_ID, validationRuleIds))))
				.withPageable(PageRequest.of(0, 10_000));

		return elasticsearchTemplate.searchForStream(queryBuilder.build(), WhitelistItem.class)
				.stream()
				.map(SearchHit::getContent)
				.collect(Collectors.toList());
	}

	public List<WhitelistItem> findAllByBranchAndMinimumCreationDate(String branchPath, Date date, WhitelistItem.WhitelistItemType type, boolean includeDescendants, PageRequest pageRequest) {
		final Date creationDate = getDefaultDateIfNull(date);
		Query query;
		if (includeDescendants) {
			Query branchQuery = bool(b -> b
					.should(termQuery(WhitelistItem.Fields.BRANCH, branchPath))
					.should(wildcardQuery(WhitelistItem.Fields.BRANCH, branchPath + "/*")));
			query = bool(b -> b
					.must(branchQuery)
					// Must not close into another code system
					.mustNot(wildcardQuery(WhitelistItem.Fields.BRANCH, branchPath + "/*" + "SNOMEDCT*")));
		} else {
			query = termQuery(WhitelistItem.Fields.BRANCH, branchPath);
		}

		Query creationDateQuery = bool(b -> b
				.must(rangeQuery(WhitelistItem.Fields.CREATION_DATE, creationDate.getTime(), RangeQuery.Builder::gte)));

		NativeQueryBuilder nativeQueryBuilder = new NativeQueryBuilder()
				.withQuery(bool(b -> b
						.must(creationDateQuery)
						.must(query)))
				.withPageable(pageRequest);

		if (type != null && !WhitelistItem.WhitelistItemType.ALL.equals(type)) {
			if (WhitelistItem.WhitelistItemType.TEMPORARY.equals(type)) {
				nativeQueryBuilder.withFilter(termQuery(WhitelistItem.Fields.TEMPORARY, true));
			} else {
				Query mustNotExistQuery = bool(b -> b.mustNot(existsQuery(WhitelistItem.Fields.TEMPORARY)));
				nativeQueryBuilder.withFilter(bool(b -> b
						.should(mustNotExistQuery)
						.should(termQuery(WhitelistItem.Fields.TEMPORARY, false))));
			}
		}

		return elasticsearchTemplate.search(nativeQueryBuilder.build(), WhitelistItem.class)
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
		List<WhitelistItem> persistedWhitelistItems = findAllByValidationRuleIds(validationRuleIds);
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
		whitelistItem.setAdditionalFields(whitelistItem.getAdditionalFields() == null ? "" : whitelistItem.getAdditionalFields());
		whitelistItem.setComponentId(whitelistItem.getComponentId() == null ? "" : whitelistItem.getComponentId());
		whitelistItem.setCreationDate(new Date());
		return repository.save(whitelistItem);
	}

	public WhitelistItem update(WhitelistItem whitelistItem) {
		return repository.save(whitelistItem);
	}

	public void delete(WhitelistItem item) {
		repository.delete(item);
	}

	public void deleteAll(List<WhitelistItem> items) {
		repository.deleteAll(items);
	}
}

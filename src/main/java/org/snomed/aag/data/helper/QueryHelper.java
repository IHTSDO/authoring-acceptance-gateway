package org.snomed.aag.data.helper;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;

import java.util.Collection;

public class QueryHelper {
	public static Query termsQuery(String field, Collection<?> values) {
		return new TermsQuery.Builder().field(field).terms(tq -> tq.value(values.stream().map(JsonData::of).map(FieldValue::of).toList())).build()._toQuery();
	}

	public static Query existsQuery(String field) {
		return new ExistsQuery.Builder().field(field).build()._toQuery();
	}

	public static Query termQuery(String field, Object value) {
		return new TermQuery.Builder().field(field).value(FieldValue.of(JsonData.of(value))).build()._toQuery();
	}

	public static Query wildcardQuery(String field, String value) {
		return new WildcardQuery.Builder().field(field).value(value).build()._toQuery();
	}

	public static Query rangeQueryGte(String field, Object value) {
		JsonData jsonValue = JsonData.of(value);
		return new RangeQuery.Builder()
				.untyped(u -> {
					u.field(field);
					u.gte(jsonValue);
					return u;
				})
				.build()._toQuery();
	}
}

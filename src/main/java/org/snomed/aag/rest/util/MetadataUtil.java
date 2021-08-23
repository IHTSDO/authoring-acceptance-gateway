package org.snomed.aag.rest.util;

import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.snomed.aag.data.Constants;

import java.util.*;
import java.util.stream.Collectors;

public class MetadataUtil {
	private MetadataUtil() {

	}

	private static void verifyParams(Branch branch) {
		if (branch == null) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Return all author flags from metadata where the value is true.
	 *
	 * @param branch Branch to read metadata from.
	 * @return Author flags that are enabled.
	 */
	@SuppressWarnings("unchecked")
	public static Set<String> getTrueAuthorFlags(Branch branch) {
		verifyParams(branch);

		if (branch.getMetadata() == null) {
			return Collections.emptySet();
		}

		final Map<String, Object> flags = (LinkedHashMap<String, Object>) branch.getMetadata().getOrDefault(Constants.AUTHOR_FLAG, new HashMap<>());
		return flags.entrySet().stream()
				.filter(entry -> Boolean.parseBoolean(entry.getValue().toString()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}
}

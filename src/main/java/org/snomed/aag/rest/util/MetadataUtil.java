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

		final Map<String, Object> flags = (Map<String, Object>) branch.getMetadata().getOrDefault(Constants.AUTHOR_FLAG, new HashMap<>());
		return flags.entrySet().stream()
				.filter(entry -> Boolean.parseBoolean(entry.getValue().toString()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}

	/**
	 * Return author flags where the value is true.
	 *
	 * @param flags Collection to read.
	 * @return Author flags where the value is true.
	 */
	public static Set<String> getTrueAuthorFlags(Map<String, Object> flags) {
		if (flags == null || flags.isEmpty()) {
			return Collections.emptySet();
		}

		return flags.entrySet().stream()
				.filter(entry -> Boolean.parseBoolean(entry.getValue().toString()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}

	/**
	 * Return all author flags from metadata.
	 *
	 * @param branch Branch to read metadata from.
	 * @return Author flags that are enabled.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getAuthorFlags(Branch branch) {
		verifyParams(branch);
		Map<String, Object> metadata = branch.getMetadata();
		if (metadata == null) {
			return new LinkedHashMap<>();
		}

		Object authorFlags = metadata.get(Constants.AUTHOR_FLAG);
		if (authorFlags == null) {
			return new LinkedHashMap<>();
		}

		return (LinkedHashMap<String, Object>) authorFlags;
	}
}

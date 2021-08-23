package org.snomed.aag.rest.util;

import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.snomed.aag.data.Constants;

import java.util.LinkedHashMap;
import java.util.Map;

public class MetadataUtil {
	private MetadataUtil() {

	}

	private static void verifyParams(Branch branch) {
		if (branch == null) {
			throw new IllegalArgumentException();
		}
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

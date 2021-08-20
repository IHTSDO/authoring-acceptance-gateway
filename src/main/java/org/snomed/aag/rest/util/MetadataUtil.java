package org.snomed.aag.rest.util;

import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.snomed.aag.data.Constants;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

	/**
	 * Return all author flags from metadata that are enabled.
	 *
	 * @param branch Branch to read metadata from.
	 * @return Author flags that are enabled.
	 */
	public static Set<String> getEnabledAuthorFlags(Branch branch) {
		verifyParams(branch);
		Map<String, Object> authorFlags = getAuthorFlags(branch);
		authorFlags.entrySet().removeIf(eS -> {
			Object value = eS.getValue();
			if (value == null) {
				return true;
			}

			// Don't remove if active
			return !Boolean.parseBoolean(value.toString());
		});

		return authorFlags.keySet();
	}
}

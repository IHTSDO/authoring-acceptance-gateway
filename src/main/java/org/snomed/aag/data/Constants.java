package org.snomed.aag.data;

import org.springframework.data.domain.PageRequest;

public class Constants {
	public static final PageRequest PAGE_OF_ONE = PageRequest.of(0, 1);
	public static final PageRequest SMALL_PAGE = PageRequest.of(0, 100);
	public static final PageRequest LARGE_PAGE = PageRequest.of(0, 10_000);

	/**
	 * Key in Branch metadata storing collection of author flags.
	 */
	public static final String AUTHOR_FLAG = "authorFlags";


	/**
	 * Key in Branch metadata storing whether Branch received content via a batch process.
	 */
	public static final String AUTHOR_FLAG_BATCH_CHANGE = "batch-change";
}

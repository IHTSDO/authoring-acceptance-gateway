package org.snomed.aag.rest.util;

public class BranchPathUtil {

    private BranchPathUtil() {
    }

    public static String extractCodeSystem(String branch) {
        if (branch.contains("SNOMEDCT-")) {
            return branch.substring(branch.lastIndexOf("SNOMEDCT-")).replaceAll("/.*", "");
        } else {
            return "SNOMEDCT";
        }
    }
}

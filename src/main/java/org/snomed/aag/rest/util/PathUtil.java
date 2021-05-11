package org.snomed.aag.rest.util;

public class PathUtil {
    private static final String SEPARATOR = "/";

    private PathUtil() {

    }

    public static String getParentPath(String path) {
        if (path == null) {
            return null;
        }

        int indexOf = path.lastIndexOf(SEPARATOR);
        return indexOf != -1 ? path.substring(0, indexOf) : null;
    }
}

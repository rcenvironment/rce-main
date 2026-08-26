/*
 * Copyright 2006-2026 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;


import java.nio.file.Path;

/**
 * Utility class for file path related operations.
 * 
 * @author Jan Flink
 */
public final class PathUtils {

    private static final String SINGLE_QUOTE = "'";

    private static final String DOUBLE_QUOTE = "\"";

    private PathUtils() {

    }

    /**
     * Calculates the relative path from a base path to a target path.
     *
     * @param basePath The absolute path from which the relative path should be calculated.
     * @param targetPath The absolute path to which the relative path should be calculated.
     * @return A string representing the relative path from basePath to targetPath.
     * @throws IllegalArgumentException if either path is null, not absolute, not supported on all platforms or located on different roots.
     */
    public static String getRelativePath(String basePath, String targetPath) {
        return getRelativePath(basePath, targetPath, PathUtilsTargetOS.DEFAULT);
    }

    /**
     * Calculates the OS specific relative path from a base path to a target path.
     * 
     * @param basePath The absolute path from which the relative path should be calculated.
     * @param targetPath The absolute path to which the relative path should be calculated.
     * @param targetOS The target OS for which the relative path should be specifically calculated.
     * @return A string representing the relative path from basePath to targetPath.
     * @throws IllegalArgumentException if either path is null, not absolute, not supported on all platforms or located on different roots.
     */
    public static String getRelativePath(String basePath, String targetPath, PathUtilsTargetOS targetOS) {
        if (basePath == null || targetPath == null) {
            throw new IllegalArgumentException("Base path and target path must not be null or empty.");
        }

        Path base = Path.of(stripSurroundingQuotes(basePath)).normalize();
        Path target = Path.of(stripSurroundingQuotes(targetPath)).normalize();

        validatePaths(base, target);

        // Paths on different Windows drives cannot be computed to a relative path
        if (!base.getRoot().equals(target.getRoot())) {
            throw new IllegalArgumentException("Paths must be on the same drive.");
        }
        
        String relativePath = base.relativize(target).toString();
        
        switch (targetOS){
        case WINDOWS:
            return relativePath.replace('/', '\\');
        case LINUX:
            return relativePath.replace('\\', '/');
        default:
            return relativePath;
        }
    }

    /**
     * Validates that the given path is equal to or located below the base path.
     *
     * @param path The path that must be within or equal to the base path
     * @param basePath The expected base path
     * @return true if the given path is inside the base path
     * @throws IllegalArgumentException if either path is null, empty, not absolute or not supported on all platforms
     */
    public static boolean ensureWithinBasePath(String path, String basePath) {
        if (path == null || path.trim().isEmpty() || basePath == null || basePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Path and base path must not be null or empty");
        }

        Path p = Path.of(stripSurroundingQuotes(path)).normalize();
        Path base = Path.of(stripSurroundingQuotes(basePath)).normalize();

        validatePaths(p, base);

        p = p.toAbsolutePath();
        base = base.toAbsolutePath();

        return p.startsWith(base);
    }

    /**
     * Validates paths to be supported on all platforms and to be absolute.
     * 
     * @param paths The paths to be validated
     * @throws IllegalArgumentException if at least one path is not absolute or not supported on all platforms
     */
    private static void validatePaths(Path... paths) {
        for (Path path : paths) {
            if (!CrossPlatformFilenameUtils.isPathValid(path.toString())) {
                throw new IllegalArgumentException(
                    StringUtils.format("File path is not valid on all supported platforms: %s", path.toString()));
            }

            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(StringUtils.format("Paths is not absolute: %s", path.toString()));
            }
        }
    }

    private static String stripSurroundingQuotes(String path) {

        if (path == null || path.isBlank()) {
            return path;
        }

        String stripped = path.strip();

        if (stripped.length() >= 2) {
            if ((stripped.startsWith(DOUBLE_QUOTE) && stripped.endsWith(DOUBLE_QUOTE))
                || (stripped.startsWith(SINGLE_QUOTE) && stripped.endsWith(SINGLE_QUOTE))) {
                return stripped.substring(1, stripped.length() - 1);
            }
        }

        return stripped;

    }

    /** Enum for selecting a target OS for OS specific path generation. */
    public enum PathUtilsTargetOS implements Comparable<PathUtilsTargetOS> {

        /** Target OS is current OS. */
        DEFAULT("default", "default"),

        /** Target OS is Linux. */
        LINUX("Linux", "linux"),

        /** Target OS is Windows. */
        WINDOWS("Windows", "win");

        /** Display name of the target OS. */
        public String displayName;

        /** Short name of the target OS. */
        public String shortName;

        PathUtilsTargetOS(String displayName, String shortName) {
            this.displayName = displayName;
            this.shortName = shortName;
        }

    }
}

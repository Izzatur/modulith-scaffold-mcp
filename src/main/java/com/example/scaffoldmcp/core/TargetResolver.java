package com.example.scaffoldmcp.core;

import java.nio.file.Path;

/**
 * Turns the user's folder choice into a safe absolute path.
 * The choice is either the word "current" (the folder this server was started in)
 * or an absolute path. Roots and the home folder itself are refused.
 */
public final class TargetResolver {

    private final Path cwd;
    private final Path home;
    private final Path allowedRoot; // may be null = no extra restriction

    public TargetResolver(Path cwd, Path home, Path allowedRoot) {
        this.cwd = cwd.toAbsolutePath().normalize();
        this.home = home.toAbsolutePath().normalize();
        this.allowedRoot = allowedRoot == null ? null : allowedRoot.toAbsolutePath().normalize();
    }

    /** Uses the process working directory, the user's home, and optional SCAFFOLD_ALLOWED_ROOT. */
    public static TargetResolver fromEnvironment() {
        String allowed = System.getenv("SCAFFOLD_ALLOWED_ROOT");
        return new TargetResolver(
                Path.of(""),
                Path.of(System.getProperty("user.home")),
                (allowed == null || allowed.isBlank()) ? null : Path.of(allowed));
    }

    public Path resolve(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "targetDir is required. Use 'current' for the folder this server was started in, "
                            + "or give an absolute path. Ask the user which one they want.");
        }
        String value = input.trim();
        Path path;
        if (value.equalsIgnoreCase("current")) {
            path = cwd;
        } else if (value.equals("~")) {
            path = home;
        } else if (value.startsWith("~/") || value.startsWith("~\\")) {
            path = home.resolve(value.substring(2));
        } else {
            path = Path.of(value);
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(
                        "targetDir must be an absolute path or the word 'current': " + value);
            }
        }
        path = path.toAbsolutePath().normalize();

        if (path.getParent() == null) {
            throw new IllegalArgumentException("Refusing to use a filesystem root as the target: " + path);
        }
        if (path.equals(home)) {
            throw new IllegalArgumentException(
                    "Refusing to use the home folder itself as the target. Choose a subfolder: " + path);
        }
        if (allowedRoot != null && !path.startsWith(allowedRoot)) {
            throw new IllegalArgumentException(
                    "Target " + path + " is outside the allowed root " + allowedRoot + ".");
        }
        return path;
    }
}

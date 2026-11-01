package com.example.scaffoldmcp.core;

import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;

/** Validates and converts names that end up in file paths and Java identifiers. */
public final class Names {

    private static final Pattern PROJECT = Pattern.compile("^[a-z][a-z0-9-]{1,40}$");
    private static final Pattern MODULE = Pattern.compile("^[a-z][a-z0-9]{1,30}$");
    private static final Pattern PACKAGE = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");

    private Names() {
    }

    public static String projectName(String value) {
        return require(PROJECT, value, "projectName",
                "lowercase letters, digits and hyphens, starting with a letter (2 to 41 characters), e.g. my-shop");
    }

    public static String moduleName(String value) {
        String module = require(MODULE, value, "module name",
                "lowercase letters and digits only, starting with a letter (2 to 31 characters), e.g. orders");
        if (SourceVersion.isKeyword(module)) {
            throw new IllegalArgumentException("Module name '" + module + "' is a Java keyword.");
        }
        return module;
    }

    public static String packageName(String value) {
        String pkg = require(PACKAGE, value, "package or groupId",
                "lowercase dot-separated name such as com.example");
        for (String part : pkg.split("\\.")) {
            if (SourceVersion.isKeyword(part)) {
                throw new IllegalArgumentException("'" + part + "' is a Java keyword and cannot be part of a package name.");
            }
        }
        return pkg;
    }

    /** my-shop becomes MyShop. */
    public static String className(String hyphenated) {
        StringBuilder out = new StringBuilder();
        for (String part : hyphenated.split("-")) {
            if (!part.isEmpty()) {
                out.append(capitalize(part));
            }
        }
        return out.toString();
    }

    public static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /** groupId plus the project name without hyphens, e.g. com.example + my-shop -> com.example.myshop. */
    public static String defaultPackage(String groupId, String projectName) {
        return packageName(groupId + "." + projectName.replace("-", ""));
    }

    private static String require(Pattern pattern, String value, String label, String rule) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String trimmed = value.trim();
        if (!pattern.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid " + label + " '" + trimmed + "'. Use " + rule + ".");
        }
        return trimmed;
    }
}

package com.example.scaffoldmcp.core;

/** The design patterns this server can add to a module. */
public enum DesignPattern {

    STRATEGY(
            "Swap interchangeable algorithms behind one interface.",
            "When behaviour depends on a rule that varies, such as discounts, pricing or routing.",
            "Inject List<Strategy> and pick by name instead of growing an if/else chain."),

    FACTORY(
            "Create objects without the caller knowing which concrete class is built.",
            "When the class to create depends on runtime input.",
            "Often unnecessary in Spring because the container already creates beans. "
                    + "Use a factory when the choice depends on a runtime value."),

    OBSERVER(
            "Notify interested parties when something happens.",
            "When one change must trigger several independent reactions.",
            "Use Spring application events with @EventListener instead of hand-written listener lists. "
                    + "Events also work across Spring Modulith modules."),

    DECORATOR(
            "Add behaviour by wrapping an object that has the same interface.",
            "When you want combinable extras such as formatting, caching or logging without subclass explosion.",
            "Wire the chain in a @Configuration class. Spring AOP offers similar wrapping for cross-cutting concerns.");

    private final String summary;
    private final String whenToUse;
    private final String springNote;

    DesignPattern(String summary, String whenToUse, String springNote) {
        this.summary = summary;
        this.whenToUse = whenToUse;
        this.springNote = springNote;
    }

    public String summary() {
        return summary;
    }

    public String whenToUse() {
        return whenToUse;
    }

    public String springNote() {
        return springNote;
    }

    /** Lower-case folder and package name, e.g. "strategy". */
    public String folder() {
        return name().toLowerCase();
    }

    /** Accepts "strategy", "Factory Method", "factory-method" and similar. */
    public static DesignPattern parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("pattern is required. Available: " + available());
        }
        String key = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (key.equals("FACTORY_METHOD")) {
            key = "FACTORY";
        }
        for (DesignPattern pattern : values()) {
            if (pattern.name().equals(key)) {
                return pattern;
            }
        }
        throw new IllegalArgumentException("Unknown pattern '" + value + "'. Available: " + available());
    }

    private static String available() {
        StringBuilder out = new StringBuilder();
        for (DesignPattern pattern : values()) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(pattern.folder());
        }
        return out.toString();
    }
}

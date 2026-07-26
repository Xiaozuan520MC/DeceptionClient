package com.easycaikuai.deceptionclient.module;

public enum Category {
    COMBAT("Combat", "com.easycaikuai.deceptionclient.module.modules.combat"),
    MOVEMENT("Movement", "com.easycaikuai.deceptionclient.module.modules.movement"),
    RENDER("Render", "com.easycaikuai.deceptionclient.module.modules.render"),
    PLAYER("Player", "com.easycaikuai.deceptionclient.module.modules.player"),
    MISC("Misc", "com.easycaikuai.deceptionclient.module.modules.misc"),
    CONFIG("Config", null),
    SETTINGS("Settings", null);
    private final String displayName;
    private final String packageName;

    Category(String displayName, String packageName) {
        this.displayName = displayName;
        this.packageName = packageName;
    }

    public static Category fromClass(Class<?> clazz) {
        String className = clazz.getName();
        for (Category category : values()) {
            if (className.startsWith(category.packageName)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category for class: " + className);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPackageName() {
        return packageName;
    }
}

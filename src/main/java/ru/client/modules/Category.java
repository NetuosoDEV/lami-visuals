package ru.client.modules;

public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    HUD("HUD"),
    UTILITY("Utility"),
    COLORS("Colors"),
    CONFIGS("Configs"),
    ACCOUNTS("Accounts");

    private final String displayName;

    Category(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    public boolean isModuleTab() { return this != CONFIGS && this != ACCOUNTS; }
}

package com.wwwjaripraj777.clashhelper;

public class SafeMacroDefinition {
    public static final String ACTION_TAP = "tap";
    public static final String ACTION_DELAY = "delay";

    public String actionType;
    public int x;
    public int y;
    public long delayMs;

    public SafeMacroDefinition(String actionType, int x, int y, long delayMs) {
        this.actionType = actionType;
        this.x = x;
        this.y = y;
        this.delayMs = delayMs;
    }
}

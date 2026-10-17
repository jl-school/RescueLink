package com.rescuelink.ui;

/** Shared formatting for the RescueLink console screens. */
final class TerminalUi {

    private static final String RULE = "════════════════════════════════════════════════════════════";

    private TerminalUi() { }

    static void banner(String title, String subtitle) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("  RESCUELINK  /  " + title);
        if (subtitle != null && !subtitle.isBlank()) {
            System.out.println("  " + subtitle);
        }
        System.out.println(RULE);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("── " + title + " ──────────────────────────────────────────────");
    }

    static String status(String status) {
        return switch (status) {
            case "ACTIVE" -> "● ACTIVE";
            case "PENDING" -> "◷ PENDING";
            case "DEACTIVATED" -> "○ DEACTIVATED";
            default -> status;
        };
    }
}

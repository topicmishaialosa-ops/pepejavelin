package org.blade.feature.impl.misc;

import java.util.ArrayList;
import java.util.List;

public final class VibeSession {
    private static int nextId = 1;

    public final int id;
    public final String name;
    public final long createdAt;
    public final List<VibeSession.Msg> history = new ArrayList<>();
    public String lastPrompt = "";
    public int retries = 0;

    public VibeSession(String name) {
        this.id = VibeSession.nextId++;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }

    public record Msg(boolean user, String text) {
    }

    public void addMessage(boolean user, String text) {
        this.history.add(new Msg(user, text));
    }

    public String getShortLabel() {
        if (this.history.size() > 1) {
            for (VibeSession.Msg m : this.history) {
                if (m.user()) {
                    String t = m.text();
                    return t.length() > 22 ? t.substring(0, 22) + "..." : t;
                }
            }
        }
        return this.name;
    }
}
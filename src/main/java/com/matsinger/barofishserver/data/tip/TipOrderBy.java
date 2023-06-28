package com.matsinger.barofishserver.data.tip;

public enum TipOrderBy {
    id("id"), type("type"), title("title"), description("description"), createdAt("createdAt");

    public final String label;

    private TipOrderBy(String label) {
        this.label = label;
    }
}

package com.wouterbreukink.onedrive.client.resources.facets;

public class QuotaFacet {

    private long total;
    private long used;
    private long remaining;
    private long deleted;
    private String state;

    public long getTotal() {
        return total;
    }

    public long getUsed() {
        return used;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getDeleted() {
        return deleted;
    }

    public String getState() {
        return state;
    }
}

package com.wouterbreukink.onedrive.client.facets;

import com.google.api.client.util.Key;

public class QuotaFacet {

    @Key
    private long total;
    @Key
    private long used;
    @Key
    private long remaining;
    @Key
    private long deleted;
    @Key
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

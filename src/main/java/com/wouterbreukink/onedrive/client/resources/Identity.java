package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;

public class Identity {

    @Key
    private String id;
    @Key
    private String displayName;

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}

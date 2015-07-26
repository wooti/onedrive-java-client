package com.wouterbreukink.onedrive.client.facets;

import com.google.api.client.util.Key;

public class FolderFacet {

    @Key
    private long childCount;

    public long getChildCount() {
        return childCount;
    }
}

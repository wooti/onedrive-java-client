package com.wouterbreukink.onedrive.client.resources;

import com.wouterbreukink.onedrive.client.resources.facets.FolderFacet;

public class WriteFolder {
    private String name;
    private FolderFacet folder;

    public WriteFolder(String name) {
        this.name = name;
        this.folder = new FolderFacet();
    }

    public String getName() {
        return name;
    }

    public FolderFacet getFolder() {
        return folder;
    }
}

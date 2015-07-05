package com.wouterbreukink.onedrive.resources;

import com.wouterbreukink.onedrive.resources.facets.FolderFacet;

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

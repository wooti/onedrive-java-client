package com.wouterbreukink.onedrive.client.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wouterbreukink.onedrive.client.resources.facets.FileFacet;
import com.wouterbreukink.onedrive.client.resources.facets.FileSystemInfoFacet;

public class WriteItem {
    private final FileSystemInfoFacet fileSystemInfo;
    private String name;
    private FileFacet file = new FileFacet();
    private boolean multipart;

    public WriteItem(String name, FileSystemInfoFacet fileSystemInfo, boolean multipart) {
        this.name = name;
        this.fileSystemInfo = fileSystemInfo;
        this.multipart = multipart;
    }

    public FileSystemInfoFacet getFileSystemInfo() {
        return fileSystemInfo;
    }

    public String getName() {
        return name;
    }

    public FileFacet getFile() {
        return file;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("@content.sourceUrl")
    public String getSourceUrl() {
        return multipart ? "cid:content" : null;
    }

    //@JsonInclude(JsonInclude.Include.NON_NULL)
    //@JsonProperty("@name.conflictBehavior")
    //public String getConflictBehaviour() {
    //    return "replace";
    //}
}

package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;
import com.wouterbreukink.onedrive.client.facets.DeletedFacet;
import com.wouterbreukink.onedrive.client.facets.FileFacet;
import com.wouterbreukink.onedrive.client.facets.FileSystemInfoFacet;
import com.wouterbreukink.onedrive.client.facets.FolderFacet;

public class Item {

    @Key
    private String id;
    @Key
    private String name;
    @Key
    private String eTag;
    @Key
    private String cTag;
    @Key
    private IdentitySet createdBy;
    @Key
    private IdentitySet lastModifiedBy;
    @Key
    private String createdDateTime;
    @Key
    private String lastModifiedDateTime;
    @Key
    private long size;
    @Key
    private ItemReference parentReference;
    @Key
    private Item[] children;
    @Key
    private String webUrl;
    @Key
    private FolderFacet folder;
    @Key
    private FileFacet file;
    @Key
    private FileSystemInfoFacet fileSystemInfo;
    @Key
    private DeletedFacet deleted;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String geteTag() {
        return eTag;
    }

    public String getcTag() {
        return cTag;
    }

    public IdentitySet getCreatedBy() {
        return createdBy;
    }

    public IdentitySet getLastModifiedBy() {
        return lastModifiedBy;
    }

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public String getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public long getSize() {
        return size;
    }

    public ItemReference getParentReference() {
        return parentReference;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public FolderFacet getFolder() {
        return folder;
    }

    public FileFacet getFile() {
        return file;
    }

    public FileSystemInfoFacet getFileSystemInfo() {
        return fileSystemInfo;
    }

    public DeletedFacet getDeleted() {
        return deleted;
    }

    public Item[] getChildren() {
        return children;
    }
}

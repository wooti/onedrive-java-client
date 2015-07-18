package com.wouterbreukink.onedrive.client.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.client.resources.facets.DeletedFacet;
import com.wouterbreukink.onedrive.client.resources.facets.FileFacet;
import com.wouterbreukink.onedrive.client.resources.facets.FileSystemInfoFacet;
import com.wouterbreukink.onedrive.client.resources.facets.FolderFacet;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item implements OneDriveItem {

    private String id;
    private String name;
    private String eTag;
    private String cTag;
    private IdentitySet createdBy;
    private IdentitySet lastModifiedBy;
    private Date createdDateTime;
    private Date lastModifiedDateTime;
    private long size;
    private ItemReference parentReference;
    private Item[] children;
    private String webUrl;
    private FolderFacet folder;
    private FileFacet file;
    private FileSystemInfoFacet fileSystemInfo;
    //private	ImageFacet	image
    //private	PhotoFacet	photo
    //private	AudioFacet	audio
    //private	VideoFacet	video
    //private	LocationFacet	location
    private DeletedFacet deleted;

    public boolean isDeleted() {
        return deleted != null;
    }

    public boolean isFolder() {
        return folder != null;
    }

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

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public Date getLastModifiedDateTime() {
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

    public String getFullName() {
        return parentReference.getFullName() + "/" + name + (isFolder() ? "/" : "");
    }

    public Item[] getChildren() {
        return children;
    }
}

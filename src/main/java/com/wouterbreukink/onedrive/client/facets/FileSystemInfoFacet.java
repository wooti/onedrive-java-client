package com.wouterbreukink.onedrive.client.facets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import com.google.api.client.util.Key;
import com.wouterbreukink.onedrive.client.serialization.JsonDateSerializer;

public class FileSystemInfoFacet {

	public FileSystemInfoFacet()
	{
		
	}
	
	public FileSystemInfoFacet(Date createdDate, Date modifiedDate)
	{
		createdDateTime = JsonDateSerializer.INSTANCE.serialize(createdDate);
        lastModifiedDateTime = JsonDateSerializer.INSTANCE.serialize(modifiedDate);		
	}
	
	public FileSystemInfoFacet(File file) throws IOException
	{
		BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        
		createdDateTime = JsonDateSerializer.INSTANCE.serialize(new Date(attr.creationTime().toMillis()));
        lastModifiedDateTime = JsonDateSerializer.INSTANCE.serialize(new Date(attr.lastModifiedTime().toMillis()));        
	}
	
    @Key
    private String createdDateTime;
    @Key
    private String lastModifiedDateTime;
    
    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(String lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }	
}

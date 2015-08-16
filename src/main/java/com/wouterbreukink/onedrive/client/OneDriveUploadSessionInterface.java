package com.wouterbreukink.onedrive.client;

import java.io.File;
import java.io.IOException;

public interface OneDriveUploadSessionInterface {

	void setRanges(String[] stringRanges);

	byte[] getChunk() throws IOException;

	long getTotalUploaded();

	long getLastUploaded();

	OneDriveItem getParent();

	String getUploadUrl();

	File getFile();

	String getRemoteFilename();

	boolean isComplete();

	void setComplete(OneDriveItem item);

	OneDriveItem getItem();

	long getRemoteFileLength();

}
package com.wouterbreukink.onedrive.client.resources.facets;

public class FileFacet {

    private String mimeType;
    private HashesFacet hashes;

    public String getMimeType() {
        return mimeType;
    }

    public HashesFacet getHashes() {
        return hashes;
    }
}

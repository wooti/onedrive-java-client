package com.wouterbreukink.onedrive.client;

import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.api.client.util.Lists;
import com.wouterbreukink.onedrive.client.authoriser.AuthorisationProvider;
import com.wouterbreukink.onedrive.client.facets.FileSystemInfoFacet;
import com.wouterbreukink.onedrive.client.resources.Drive;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.ItemSet;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

class ROOneDriveProvider implements OneDriveProvider {

    static final HttpTransport HTTP_TRANSPORT = new ApacheHttpTransport();
    static final JsonFactory JSON_FACTORY = new GsonFactory();

    final HttpRequestFactory requestFactory;

    public ROOneDriveProvider(final AuthorisationProvider authoriser) {
        requestFactory =
                HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(JSON_FACTORY));
                        try {
                            request.getHeaders().setAuthorization("bearer " + authoriser.getAccessToken());
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }

                        request.setUnsuccessfulResponseHandler(new OneDriveResponseHandler(authoriser));
                    }
                });
    }

    public Drive getDefaultDrive() throws IOException {
        HttpRequest request = requestFactory.buildGetRequest(OneDriveUrl.defaultDrive());
        return request.execute().parseAs(Drive.class);
    }

    public OneDriveItem getRoot() throws IOException {
        HttpRequest request = requestFactory.buildGetRequest(OneDriveUrl.driveRoot());
        Item response = request.execute().parseAs(Item.class);
        return OneDriveItem.FACTORY.create(response);
    }

    public OneDriveItem[] getChildren(OneDriveItem parent) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

        List<OneDriveItem> itemsToReturn = Lists.newArrayList();

        String token = null;

        do {

            OneDriveUrl url = OneDriveUrl.children(parent.getId());

            if (token != null) {
                url.setToken(token);
            }

            HttpRequest request = requestFactory.buildGetRequest(url);
            ItemSet items = request.execute().parseAs(ItemSet.class);

            for (Item i : items.getValue()) {
                itemsToReturn.add(OneDriveItem.FACTORY.create(i));
            }

            token = items.getNextToken();

        } while (token != null); // If we have a token for the next page we need to keep going

        return itemsToReturn.toArray(new OneDriveItem[itemsToReturn.size()]);
    }

    public OneDriveItem getPath(String path) throws IOException {
        HttpRequest request = requestFactory.buildGetRequest(OneDriveUrl.getPath(path));
        Item response = request.execute().parseAs(Item.class);
        return OneDriveItem.FACTORY.create(response);
    }

    public OneDriveItem replaceFile(OneDriveItem parent, File file, String remoteFilename) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, remoteFilename, file.isDirectory());
    }
    
    @Override
	public OneDriveItem replaceEncryptedFile(OneDriveItem parent, HttpContent httpContent, FileSystemInfoFacet fsi,
			String remoteFilename) throws IOException {
    	
    	if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, remoteFilename, false);
	}

    public OneDriveItem uploadFile(OneDriveItem parent, File file, String remoteFilename) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, remoteFilename, file.isDirectory());
    }
    
	@Override
	public OneDriveItem uploadEncryptedFile(OneDriveItem parent, HttpContent httpContent, FileSystemInfoFacet fsi,
			String remoteFilename) throws IOException {

		if (!parent.isDirectory()) {
			throw new IllegalArgumentException("Parent is not a folder");
		}

		return OneDriveItem.FACTORY.create(parent, remoteFilename, false);
	}

    @Override
    public OneDriveUploadSessionInterface startUploadSession(OneDriveItem parent, File file, String remoteFilename) throws IOException {
        return new OneDriveUploadSession(parent, file, remoteFilename, null, new String[0]);
    }
    
    @Override
    public OneDriveUploadSessionInterface startEncryptedUploadSession(OneDriveItem parent, File file, String remoteFilename) throws IOException {
        return new OneDriveEncryptedUploadSession(parent, file, remoteFilename, null, new String[0]);
    }

    @Override
    public void uploadChunk(OneDriveUploadSessionInterface session) throws IOException {
        session.setComplete(OneDriveItem.FACTORY.create(session.getParent(), session.getRemoteFilename(), session.getFile().isDirectory()));
    }

    public OneDriveItem updateFile(OneDriveItem item, Date createdDate, Date modifiedDate) throws IOException {
        // Do nothing, just return the unmodified item
        return item;
    }
	
    @Override
	public OneDriveItem updateFile(OneDriveItem item, FileSystemInfoFacet fsi) throws IOException {
        // Do nothing, just return the unmodified item
        return item;
	}

    public OneDriveItem createFolder(OneDriveItem parent, String name) throws IOException {
        // Return a dummy folder
        return OneDriveItem.FACTORY.create(parent, name, true);
    }

    public void download(OneDriveItem item, File target) throws IOException {
        // Do nothing
    }

    public void delete(OneDriveItem remoteFile) throws IOException {
        // Do nothing
    }
}

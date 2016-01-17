package com.wouterbreukink.onedrive.client;

import com.google.api.client.http.*;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.api.client.util.Lists;
import com.wouterbreukink.onedrive.client.authoriser.AuthorisationProvider;
import com.wouterbreukink.onedrive.client.downloader.ResumableDownloaderProgressListener;
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
                        request.setReadTimeout(60000);
                        request.setConnectTimeout(60000);
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
        try {
            HttpRequest request = requestFactory.buildGetRequest(OneDriveUrl.getPath(path));
            Item response = request.execute().parseAs(Item.class);
            return OneDriveItem.FACTORY.create(response);
        } catch (HttpResponseException e) {
            throw new OneDriveAPIException(e.getStatusCode(), "Unable to get path", e);
        } catch (IOException e) {
            throw new OneDriveAPIException(0, "Unable to get path", e);
        }
    }

    public OneDriveItem replaceFile(OneDriveItem parent, File file) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, file.getName(), file.isDirectory());
    }

    public OneDriveItem uploadFile(OneDriveItem parent, File file) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, file.getName(), file.isDirectory());
    }

    @Override
    public OneDriveUploadSession startUploadSession(OneDriveItem parent, File file) throws IOException {
        return new OneDriveUploadSession(parent, file, null, new String[0]);
    }

    @Override
    public void uploadChunk(OneDriveUploadSession session) throws IOException {
        session.setComplete(OneDriveItem.FACTORY.create(session.getParent(), session.getFile().getName(), session.getFile().isDirectory()));
    }

    public OneDriveItem updateFile(OneDriveItem item, Date createdDate, Date modifiedDate) throws IOException {
        // Do nothing, just return the unmodified item
        return item;
    }

    public OneDriveItem createFolder(OneDriveItem parent, File target) throws IOException {
        // Return a dummy folder
        return OneDriveItem.FACTORY.create(parent, target.getName(), true);
    }

    public void download(OneDriveItem item, File target, ResumableDownloaderProgressListener progressListener) throws IOException {
        // Do nothing
    }

    public void delete(OneDriveItem remoteFile) throws IOException {
        // Do nothing
    }
}

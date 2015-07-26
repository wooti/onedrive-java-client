package com.wouterbreukink.onedrive.client;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.BackOffUtils;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.Sleeper;
import com.wouterbreukink.onedrive.client.authoriser.AuthorisationProvider;

import java.io.IOException;

class OneDriveResponseHandler implements HttpUnsuccessfulResponseHandler {

    private final Sleeper sleeper = Sleeper.DEFAULT;
    private final BackOff backOff = new ExponentialBackOff();
    private final AuthorisationProvider authoriser;

    public OneDriveResponseHandler(AuthorisationProvider authoriser) {
        this.authoriser = authoriser;
    }

    @Override
    public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry) throws IOException {

        if (!supportsRetry) {
            return false;
        }

        if (response.getStatusCode() == HttpStatusCodes.STATUS_CODE_UNAUTHORIZED) {
            authoriser.refresh();
            return true;
        }

        // check if back-off is required for this response
        if (isRequired(response)) {
            try {
                return BackOffUtils.next(sleeper, backOff);
            } catch (InterruptedException exception) {
                // ignore
            }
        }

        return false;
    }

    public boolean isRequired(HttpResponse httpResponse) {
        return httpResponse.getStatusCode() / 100 == 5 || httpResponse.getStatusCode() == 429;
    }

}

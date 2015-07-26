package com.wouterbreukink.onedrive.client.authoriser;

import com.wouterbreukink.onedrive.client.resources.Authorisation;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OneDriveAuth {

    private static final Logger log = LogManager.getLogger(OneDriveAuth.class.getName());
    private static final String clientSecret = "to8fZAGMvD7Jr-NSdY1eVm4V7eaAtV5B";
    private static final String clientId = "000000004015B68A";
    private final Client client;
    private Path keyFile;
    private Authorisation authorisation;

    public OneDriveAuth(Client client) {
        this.client = client;
    }

    public boolean initialise(Path keyFile) {
        this.keyFile = Preconditions.checkNotNull(keyFile);

        if (!Files.exists(keyFile) || !Files.isRegularFile(keyFile)) {
            log.error(String.format("Specified key file '%s' cannot be found.", keyFile));
            return false;
        }

        String[] keyFileContents = readToken();

        switch (keyFileContents.length) {
            case 0:
                log.error("Key file is empty");
                return false;
            case 1:
                String authCode = keyFileContents[0];

                // If the user has pasted the entire URL then parse it
                Pattern url = Pattern.compile("https://login.live.com/oauth20_desktop.srf.*code=(.*)&.*");
                Matcher m = url.matcher(authCode);

                if (m.matches()) {
                    authCode = m.group(1);
                }

                return getTokenFromCode(authCode);
            case 2:
                if (keyFileContents[0].equals(clientId)) {
                    return getTokenFromRefreshToken(keyFileContents[1]);
                } else {
                    log.error("Key file does not match this application version. Please re-create.");
                    return false;
                }
            default:
                log.error("Expected key file with code and/or refresh token");
                return false;
        }
    }

    public String getAccessToken() {
        if (authorisation != null) {

            // Refresh if we know it is needed
            if (authorisation.getTokenExpiryDate().before(new Date())) {
                log.info("Authorisation token has expired - refreshing");
                if (!getTokenFromRefreshToken(authorisation.getRefreshToken())) {
                    log.warn("Unable to refresh authorisation token");
                } else {
                    saveToken();
                }
            }

            return authorisation.getAccessToken();
        } else {
            throw new IllegalStateException("Authoriser has not been initialised");
        }
    }

    public void refresh() {
        if (!getTokenFromRefreshToken(authorisation.getRefreshToken())) {
            log.warn("Unable to refresh authorisation token");
        } else {
            saveToken();
        }
    }

    public void printAuthInstructions(boolean expected) {

        WebTarget target = ClientBuilder
                .newClient()
                .target("https://login.live.com/oauth20_authorize.srf")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("scope", "wl.signin wl.offline_access onedrive.readwrite")
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        if (expected) {
            log.info("You must generate an authorisation token to use this application");
            log.info("Authorisation URL: " + target.getUri());
        } else {
            log.error("Unable to authenticate. Please re-create your key file.");
            log.error("Open the following in a browser, sign on, wait until you are redirected to a blank page and then store the code returned in the address bar in your key file.");
            log.error("Authorisation URL: " + target.getUri());
        }
    }

    private boolean getTokenFromCode(String code) {

        log.debug("Fetching authorisation token using authorisation code");

        WebTarget tokenTarget =
                client.target("https://login.live.com/oauth20_token.srf")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("code", code)
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        Invocation.Builder invocationBuilder =
                tokenTarget.request(MediaType.TEXT_PLAIN_TYPE);

        return processResponse(invocationBuilder.get());
    }

    private boolean getTokenFromRefreshToken(String refreshToken) {

        log.debug("Fetching authorisation token using refresh token");

        WebTarget tokenTarget =
                client.target("https://login.live.com/oauth20_token.srf")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("refresh_token", refreshToken)
                        .queryParam("grant_type", "refresh_token")
                        .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        Invocation.Builder invocationBuilder =
                tokenTarget.request(MediaType.TEXT_PLAIN_TYPE);

        return processResponse(invocationBuilder.get());
    }

    private boolean processResponse(Response response) {
        try {
            authorisation = response.readEntity(Authorisation.class);

            // Check for failures
            if (response.getStatus() != 200 || authorisation.getError() != null) {
                log.error(String.format("Error code %d - %s (%s)",
                        response.getStatus(),
                        authorisation.getError(),
                        authorisation.getErrorDescription()));
                return false;
            }

            log.info("Fetched new authorisation token and refresh token for user " + authorisation.getUserId());
            saveToken();
            return true;

        } catch (Exception ex) {
            log.error("Unable to retrieve authorisation token", ex);
            return false;
        }
    }

    private String[] readToken() {
        try {
            return Files.readAllLines(keyFile, Charset.defaultCharset()).toArray(new String[1]);
        } catch (IOException e) {
            log.error("Unable to read key file", e);
        }

        return new String[0];
    }

    private void saveToken() {
        try {
            String[] content = new String[]{clientId, authorisation.getRefreshToken()};
            Files.write(keyFile, Arrays.asList(content), Charset.defaultCharset());
        } catch (IOException e) {
            log.error("Unable to write to key file ", e);
        }
    }
}

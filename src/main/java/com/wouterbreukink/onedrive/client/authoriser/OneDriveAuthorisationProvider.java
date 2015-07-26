package com.wouterbreukink.onedrive.client.authoriser;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
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

public class OneDriveAuthorisationProvider implements AuthorisationProvider {

    private static final Logger log = LogManager.getLogger(OneDriveAuthorisationProvider.class.getName());
    private static final String clientSecret = "to8fZAGMvD7Jr-NSdY1eVm4V7eaAtV5B";
    private static final String clientId = "000000004015B68A";
    private final Client client;
    private Path keyFile;
    private Authorisation authorisation;

    public OneDriveAuthorisationProvider(Client client, Path keyFile) throws OneDriveAPIException {
        this.client = client;

        this.keyFile = Preconditions.checkNotNull(keyFile);

        if (!Files.exists(keyFile) || !Files.isRegularFile(keyFile)) {
            throw new OneDriveAPIException(401, String.format("Specified key file '%s' cannot be found.", keyFile));
        }

        String[] keyFileContents = readToken();

        switch (keyFileContents.length) {
            case 0:
                throw new OneDriveAPIException(401, String.format("Key file '%s' is empty.", keyFile));
            case 1:
                String authCode = keyFileContents[0];

                // If the user has pasted the entire URL then parse it
                Pattern url = Pattern.compile("https://login.live.com/oauth20_desktop.srf.*code=(.*)&.*");
                Matcher m = url.matcher(authCode);

                if (m.matches()) {
                    authCode = m.group(1);
                }

                getTokenFromCode(authCode);
                break;
            case 2:
                if (keyFileContents[0].equals(clientId)) {
                    getTokenFromRefreshToken(keyFileContents[1]);
                } else {
                    throw new OneDriveAPIException(401, "Key file does not match this application version.");
                }
                break;
            default:
                throw new OneDriveAPIException(401, "Expected key file with code and/or refresh token");
        }
    }

    public static void printAuthInstructions() {

        WebTarget target = ClientBuilder.newClient()
                .target("https://login.live.com/oauth20_authorize.srf")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("scope", "wl.signin wl.offline_access onedrive.readwrite")
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        log.info("To authorise this application ou must generate an authorisation token");
        log.info("Open the following in a browser, sign on, wait until you are redirected to a blank page and then store the url in the address bar in your key file.");
        log.info("Authorisation URL: " + target.getUri());
    }

    @Override
    public String getAccessToken() throws OneDriveAPIException {
        if (authorisation != null) {

            // Refresh if we know it is needed
            if (authorisation.getTokenExpiryDate().before(new Date())) {
                log.info("Authorisation token has expired - refreshing");
                getTokenFromRefreshToken(authorisation.getRefreshToken());
                saveToken();
            }

            return authorisation.getAccessToken();
        } else {
            throw new IllegalStateException("Authoriser has not been initialised");
        }
    }

    public void refresh() throws OneDriveAPIException {
        getTokenFromRefreshToken(authorisation.getRefreshToken());
        saveToken();
    }

    private void getTokenFromCode(String code) throws OneDriveAPIException {

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

        processResponse(invocationBuilder.get());
    }

    private void getTokenFromRefreshToken(String refreshToken) throws OneDriveAPIException {

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

        processResponse(invocationBuilder.get());
    }

    private void processResponse(Response response) throws OneDriveAPIException {
        authorisation = response.readEntity(Authorisation.class);

        // Check for failures
        if (response.getStatus() != 200 || authorisation.getError() != null) {
            throw new OneDriveAPIException(response.getStatus(),
                    String.format("Error code %d - %s (%s)",
                            response.getStatus(),
                            authorisation.getError(),
                    authorisation.getErrorDescription()));
        }

        log.info("Fetched new authorisation token and refresh token for user " + authorisation.getUserId());
        saveToken();
    }

    private String[] readToken() {
        try {
            return Files.readAllLines(keyFile, Charset.defaultCharset()).toArray(new String[1]);
        } catch (IOException e) {
            log.error("Unable to read key file", e);
        }

        return new String[0];
    }

    private void saveToken() throws OneDriveAPIException {
        try {
            String[] content = new String[]{clientId, authorisation.getRefreshToken()};
            Files.write(keyFile, Arrays.asList(content), Charset.defaultCharset());
        } catch (IOException e) {
            log.error("Unable to write to key file ", e);
        }
    }
}

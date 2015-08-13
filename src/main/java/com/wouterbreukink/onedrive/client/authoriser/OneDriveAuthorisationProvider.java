package com.wouterbreukink.onedrive.client.authoriser;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Key;
import com.google.api.client.util.Preconditions;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Authorisation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class OneDriveAuthorisationProvider implements AuthorisationProvider {

    static final HttpTransport HTTP_TRANSPORT = new ApacheHttpTransport();
    static final JsonFactory JSON_FACTORY = new GsonFactory();
    private static final Logger log = LogManager.getLogger(OneDriveAuthorisationProvider.class.getName());
    private static final String clientSecret = "to8fZAGMvD7Jr-NSdY1eVm4V7eaAtV5B";
    private static final String clientId = "000000004015B68A";
    private Path keyFile;
    private Authorisation authorisation;
    private Date lastFetched;

    public OneDriveAuthorisationProvider(Path keyFile) throws IOException {

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

        String authString =
                String.format("%s?client_id=%s&response_type=code&scope=wl.signin%%20wl.offline_access%%20onedrive.readwrite&client_secret=%s&redirect_uri=%s",
                        "https://login.live.com/oauth20_authorize.srf",
                        clientId,
                        clientSecret,
                        "https://login.live.com/oauth20_desktop.srf");

        log.info("To authorise this application ou must generate an authorisation token");
        log.info("Open the following in a browser, sign on, wait until you are redirected to a blank page and then store the url in the address bar in your key file.");
        log.info("Authorisation URL: " + authString);
    }

    @Override
    public String getAccessToken() throws IOException {
        if (authorisation != null) {

            // Refresh if we know it is needed
            if (lastFetched.after(new Date(lastFetched.getTime() + authorisation.getExpiresIn() * 1000))) {
                log.info("Authorisation token has expired - refreshing");
                getTokenFromRefreshToken(authorisation.getRefreshToken());
                saveToken();
            }

            return authorisation.getAccessToken();
        } else {
            throw new IllegalStateException("Authoriser has not been initialised");
        }
    }

    public void refresh() throws IOException {
        getTokenFromRefreshToken(authorisation.getRefreshToken());
        saveToken();
    }

    private void getTokenFromCode(final String code) throws IOException {

        log.debug("Fetching authorisation token using authorisation code");
        HttpRequest request =
                HTTP_TRANSPORT.createRequestFactory().buildGetRequest(new GenericUrl("https://login.live.com/oauth20_token.srf") {
                    @Key("client_id")
                    private String id = clientId;
                    @Key("client_secret")
                    private String secret = clientSecret;
                    @Key("code")
                    private String authCode = code;
                    @Key("grant_type")
                    private String grantType = "authorization_code";
                    @Key("redirect_uri")
                    private String redirect = "https://login.live.com/oauth20_desktop.srf";
                });

        request.setParser(new JsonObjectParser(JSON_FACTORY));

        processResponse(request.execute());
    }

    private void getTokenFromRefreshToken(final String refreshToken) throws IOException {

        log.debug("Fetching authorisation token using refresh token");

        HttpRequest request =
                HTTP_TRANSPORT.createRequestFactory().buildGetRequest(new GenericUrl("https://login.live.com/oauth20_token.srf") {
                    @Key("client_id")
                    private String id = clientId;
                    @Key("client_secret")
                    private String secret = clientSecret;
                    @Key("refresh_token")
                    private String token = refreshToken;
                    @Key("grant_type")
                    private String grantType = "refresh_token";
                    @Key("redirect_uri")
                    private String redirect = "https://login.live.com/oauth20_desktop.srf";
                });

        request.setParser(new JsonObjectParser(JSON_FACTORY));

        processResponse(request.execute());
    }

    private void processResponse(HttpResponse response) throws IOException {
        authorisation = response.parseAs(Authorisation.class);

        // Check for failures
        if (response.getStatusCode() != 200 || authorisation.getError() != null) {
            throw new OneDriveAPIException(response.getStatusCode(),
                    String.format("Error code %d - %s (%s)",
                            response.getStatusCode(),
                            authorisation.getError(),
                    authorisation.getErrorDescription()));
        }

        log.info("Fetched new authorisation token and refresh token for user " + authorisation.getUserId());
        saveToken();
        lastFetched = new Date();
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

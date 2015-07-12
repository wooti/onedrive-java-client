package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.Authorisation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class OneDriveAuth {

    private static final Logger log = LogManager.getLogger(OneDriveAuth.class.getName());
    private final Client client;
    private final Properties props;
    private Authorisation authorisation;

    public OneDriveAuth(Client client, Properties props) {

        this.client = client;
        this.props = props;

        if (props.containsKey("code") && !props.getProperty("code").equals("[PUT AUTHORISATION CODE HERE]")) {
            getTokenFromCode(props.getProperty("code"));
            props.remove("code");
        } else if (props.containsKey("refresh_token")) {
            getTokenFromRefreshToken(props.getProperty("refresh_token"));
        } else {
            printAuthInstructions();
            return;
        }

        props.setProperty("refresh_token", authorisation.getRefreshToken());
        saveToken();
    }

    public Authorisation getAuthorisation() {
        if (authorisation != null) {

            // Refresh if needed
            if (authorisation.getTokenExpiryDate().before(new Date())) {
                log.info("Authorisation token has expired - refreshing");
                getTokenFromRefreshToken(authorisation.getRefreshToken());
                saveToken();
            }
        }

        return authorisation;
    }

    public void getTokenFromCode(String code) {

        log.debug("Fetching authorisation token using authorisation code");

        WebTarget tokenTarget =
                client.target("https://login.live.com/oauth20_token.srf")
                        .queryParam("client_id", props.getProperty("client_id"))
                        .queryParam("client_secret", props.getProperty("client_secret"))
                        .queryParam("code", code)
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        Invocation.Builder invocationBuilder =
                tokenTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Response response = invocationBuilder.get();

        authorisation = response.readEntity(Authorisation.class);
        log.info("Fetched new authorisation token and refresh token for user " + authorisation.getUserId());
        saveToken();
    }

    public void getTokenFromRefreshToken(String refreshToken) {

        log.debug("Fetching authorisation token using refresh token");

        WebTarget tokenTarget =
                client.target("https://login.live.com/oauth20_token.srf")
                        .queryParam("client_id", props.getProperty("client_id"))
                        .queryParam("client_secret", props.getProperty("client_secret"))
                        .queryParam("refresh_token", refreshToken)
                        .queryParam("grant_type", "refresh_token")
                        .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        Invocation.Builder invocationBuilder =
                tokenTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Response response = invocationBuilder.get();

        authorisation = response.readEntity(Authorisation.class);
        log.debug("Fetched authorisation token for user " + authorisation.getUserId());
        saveToken();
    }

    private void printAuthInstructions() {

        WebTarget target = ClientBuilder
                .newClient()
                .target("https://login.live.com/oauth20_authorize.srf")
                .queryParam("client_id", props.getProperty("client_id"))
                .queryParam("response_type", "code")
                .queryParam("scope", "wl.signin wl.offline_access onedrive.readwrite")
                .queryParam("client_secret", props.getProperty("client_secret"))
                .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        log.error("No authentication tokens found!");
        log.warn("Open the following in a browser and store the returned code in onedrive.xml");
        log.warn("Authorisation URL: " + target.getUri());
    }

    private void saveToken() {
        try {
            props.storeToXML(new FileOutputStream("onedrive.xml"), null);
        } catch (IOException e) {
            log.error("Unable to save token ", e);
        }
    }
}

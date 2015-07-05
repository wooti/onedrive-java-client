package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.resources.Authorisation;

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
import java.util.logging.Logger;

public class OneDriveAuth {

    private static final Logger log = Logger.getLogger(OneDriveAuth.class.getName());

    private final Client client;
    private final String clientId;
    private final String clientSecret;
    private Authorisation authorisation;
    private Properties props;

    public OneDriveAuth(Client client, Properties props) {

        log.setLevel(Main.logLevel);

        this.client = client;
        this.clientId = props.getProperty("client_id");
        this.clientSecret = props.getProperty("client_secret");
        this.props = props;

        if (props.containsKey("code") && !props.getProperty("code").equals("[PUT AUTHORISATION CODE HERE]")) {
            authorisation = getTokenFromCode(props.getProperty("code"));
            props.remove("code");
        } else if (props.containsKey("refresh_token")) {
            authorisation = getTokenFromRefreshToken(props.getProperty("refresh_token"));
        } else {
            return;
        }

        props.setProperty("refresh_token", authorisation.getRefreshToken());

        try {
            props.storeToXML(new FileOutputStream("onedrive.xml"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Authorisation getAuthorisation() {
        if (authorisation != null) {

            // Refresh if needed
            if (authorisation.getTokenExpiryDate().before(new Date())) {
                log.info("Authorisation token has expired - refreshing");
                authorisation = getTokenFromRefreshToken(authorisation.getRefreshToken());

                props.setProperty("refresh_token", authorisation.getRefreshToken());
                log.info("Authorisation token has expired - refreshing");
                try {
                    props.storeToXML(new FileOutputStream("onedrive.xml"), null);
                } catch (IOException e) {
                    log.severe("Unable to save token " + e.toString());
                }
            }
        }

        return authorisation;
    }

    public void printAuthInstructions() {

        WebTarget target = ClientBuilder
                .newClient()
                .target("https://login.live.com/oauth20_authorize.srf")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("scope", "wl.signin wl.offline_access onedrive.readwrite")
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        log.severe("No authentication tokens found!");
        log.severe("Open the following in a browser and store the returned code in onedrive.xml");
        log.severe("Authorisation URL: " + target.getUri());
    }

    public Authorisation getTokenFromCode(String code) {

        log.fine("Fetching authorisation token using authorisation code");

        WebTarget tokenTarget =
                client.target("https://login.live.com/oauth20_token.srf")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("code", code)
                        .queryParam("grant_type", "authorization_code")
                        .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        Invocation.Builder invocationBuilder =
                tokenTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Response response = invocationBuilder.get();

        try {
            props.storeToXML(new FileOutputStream("onedrive.xml"), null);
        } catch (IOException e) {
            log.severe("Unable to save token " + e.toString());
        }

        return response.readEntity(Authorisation.class);
    }

    public Authorisation getTokenFromRefreshToken(String refreshToken) {

        log.fine("Fetching authorisation token using refresh token");

        WebTarget tokenTarget =
                client.target("https://login.live.com/oauth20_token.srf")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("refresh_token", refreshToken)
                        .queryParam("grant_type", "refresh_token")
                        .queryParam("redirect_uri", "https://login.live.com/oauth20_desktop.srf");

        Invocation.Builder invocationBuilder =
                tokenTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Response response = invocationBuilder.get();

        try {
            props.storeToXML(new FileOutputStream("onedrive.xml"), null);
        } catch (IOException e) {
            log.severe("Unable to save token " + e.toString());
        }

        return response.readEntity(Authorisation.class);
    }
}

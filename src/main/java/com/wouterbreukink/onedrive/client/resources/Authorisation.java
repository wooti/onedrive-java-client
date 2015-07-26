package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;

public class Authorisation {

    @Key("token_type")
    private String tokenType;
    @Key("expires_in")
    private int expiresIn;
    @Key("scope")
    private String scope;
    @Key("access_token")
    private String accessToken;
    @Key("refresh_token")
    private String refreshToken;
    @Key("user_id")
    private String userId;
    @Key("error")
    private String error;
    @Key("error_description")
    private String errorDescription;

    public String getTokenType() {
        return tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}

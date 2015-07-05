package com.wouterbreukink.onedrive.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class Authorisation {

    private long zeroTime = System.currentTimeMillis();

    private String tokenType;
    private int expiresIn;
    private String scope;
    private String accessToken;
    private String refreshToken;
    private String userId;

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    @JsonProperty("expires_in")
    public int getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    public Date getTokenExpiryDate() {
        return new Date(zeroTime + expiresIn);
    }
}

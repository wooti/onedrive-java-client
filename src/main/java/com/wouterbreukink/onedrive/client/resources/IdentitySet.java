package com.wouterbreukink.onedrive.client.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentitySet {

    private Identity user;
    private Identity application;
    private Identity device;

    public Identity getUser() {
        return user;
    }

    public Identity getApplication() {
        return application;
    }

    public Identity getDevice() {
        return device;
    }
}

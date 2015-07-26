package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;

public class IdentitySet {

    @Key
    private Identity user;
    @Key
    private Identity application;
    @Key
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

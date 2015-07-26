package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemSet {

    @Key
    private Item[] value;
    @Key("@odata.nextLink")
    private String nextPage;

    public Item[] getValue() {
        return value;
    }

    public String getNextToken() {

        if (nextPage == null) {
            return null;
        }

        String pattern = ".*skiptoken=(.*)";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(nextPage);
        if (m.find()) {
            return m.group(1);
        } else {
            throw new Error("Unable to find page token");
        }
    }
}

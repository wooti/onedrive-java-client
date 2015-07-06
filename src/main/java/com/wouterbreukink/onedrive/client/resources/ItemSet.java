package com.wouterbreukink.onedrive.client.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(value = "@odata.context")
public class ItemSet {

    private Item[] value;
    private String nextPage;

    public Item[] getValue() {
        return value;
    }

    @JsonProperty("@odata.nextLink")
    public String getNextPage() {
        return nextPage;
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

package com.wouterbreukink.onedrive.client.resources.facets;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MultiPartItem {

    private FileDetail item;

    private MultiPartItem(FileDetail item) {
        this.item = item;
    }

    public static MultiPartItem create(String name) {
        return new MultiPartItem(new FileDetail(name));
    }

    public FileDetail getItem() {
        return item;
    }

    public static class FileDetail {

        private String name;
        private String conflictBehavior = "replace";

        private FileDetail(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @JsonProperty("@name.conflictBehavior")
        public String getConflictBehavior() {
            return conflictBehavior;
        }
    }
}

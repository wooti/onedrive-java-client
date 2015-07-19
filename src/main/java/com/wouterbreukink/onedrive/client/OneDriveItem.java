package com.wouterbreukink.onedrive.client;

public interface OneDriveItem {
    String getId();

    boolean isDirectory();

    String getFullName();

    class FACTORY {

        public static OneDriveItem create(final OneDriveItem parent, final String name, final boolean isDirectory) {

            return new OneDriveItem() {
                public String getId() {
                    return null;
                }

                public boolean isDirectory() {
                    return isDirectory;
                }

                public String getFullName() {
                    return parent.getFullName() + name + (isDirectory ? "/" : "");
                }
            };
        }
    }
}

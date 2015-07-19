package com.wouterbreukink.onedrive.client;

public interface OneDriveItem {
    String getId();

    boolean isFolder();

    String getFullName();

    class FACTORY {

        public static OneDriveItem create(OneDriveItem parent, String name) {
            return create(parent, name, false);
        }

        public static OneDriveItem create(final OneDriveItem parent, final String name, final boolean isFolder) {

            return new OneDriveItem() {
                public String getId() {
                    return null;
                }

                public boolean isFolder() {
                    return isFolder;
                }

                public String getFullName() {
                    return parent.getFullName() + name;
                }
            };
        }
    }
}

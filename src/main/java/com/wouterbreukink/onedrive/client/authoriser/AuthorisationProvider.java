package com.wouterbreukink.onedrive.client.authoriser;

import java.io.IOException;
import java.nio.file.Path;

public interface AuthorisationProvider {

    String getAccessToken() throws IOException;

    void refresh() throws IOException;

    class FACTORY {
        public static AuthorisationProvider create(Path keyFile) throws IOException {
            return new OneDriveAuthorisationProvider(keyFile);
        }

        public static void printAuthInstructions() {
            OneDriveAuthorisationProvider.printAuthInstructions();
        }
    }
}

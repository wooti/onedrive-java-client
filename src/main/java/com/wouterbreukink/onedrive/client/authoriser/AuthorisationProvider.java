package com.wouterbreukink.onedrive.client.authoriser;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;

import javax.ws.rs.client.Client;
import java.nio.file.Path;

public interface AuthorisationProvider {

    String getAccessToken() throws OneDriveAPIException;

    void refresh() throws OneDriveAPIException;

    class FACTORY {
        public static AuthorisationProvider create(Client client, Path keyFile) throws OneDriveAPIException {
            return new OneDriveAuthorisationProvider(client, keyFile);
        }

        public static void printAuthInstructions() {
            OneDriveAuthorisationProvider.printAuthInstructions();
        }
    }
}

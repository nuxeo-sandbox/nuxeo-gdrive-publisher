package org.nuxeo.publisher.gdrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.liveconnect.core.CredentialFactory;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.liveconnect.core.OAuth2CredentialFactory;
import org.nuxeo.ecm.liveconnect.google.drive.GoogleDriveBlobProvider;
import org.nuxeo.ecm.liveconnect.google.drive.GoogleOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;


@Operation(
        id=PublishToGdrive.ID,
        category=Constants.CAT_BLOB,
        label="PublishToGdrive",
        description="This operation uploads the input blob to gdrive and returns the corresponding liveconnect blob")
public class PublishToGdrive {

    public static final String ID = "Blob.PublishToGdrive";

    @Context
    protected CoreSession session;

    @Param(name = "providerName", required=false)
    protected String providerName = "googledrive";

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob blob) {

        String user = session.getPrincipal().getName();

        GoogleOAuth2ServiceProvider oAuth2provider =
                (GoogleOAuth2ServiceProvider) Framework.getLocalService(OAuth2ServiceProviderRegistry.class)
                .getProvider(providerName);

        String gdriveUser = oAuth2provider.getServiceUser(user);

        CredentialFactory credentialFactory = new OAuth2CredentialFactory(oAuth2provider);

        Credential credential = null;

        try {
            credential = credentialFactory.build(gdriveUser);
            if (credential == null) {
                throw new NuxeoException("No credentials found for user "+user);
            }
            Long expiresInSeconds = credential.getExpiresInSeconds();
            if (expiresInSeconds != null && expiresInSeconds <= 0) {
                credential.refreshToken();
            }
        } catch (IOException e) {
            throw new NuxeoException("Couldn't get credential for user "+user,e);
        }

        HttpTransport httpTransport = credential.getTransport();
        JsonFactory jsonFactory = credential.getJsonFactory();
        Drive drive =  new Drive.Builder(httpTransport, jsonFactory, credential) //
                .setApplicationName("Nuxeo/0") // set application name to avoid a WARN
                .build();

        File body = new File();
        body.setTitle(blob.getFilename());
        body.setMimeType(blob.getMimeType());

        try {
            InputStreamContent inputStreamContent = new InputStreamContent(blob.getMimeType(),blob.getStream());
            File file = drive.files().insert(body,inputStreamContent).execute();

            GoogleDriveBlobProvider blobProvider =
                    (GoogleDriveBlobProvider) Framework.getService(BlobManager.class)
                    .getBlobProvider(providerName);

            return blobProvider.toBlob(new LiveConnectFileInfo(gdriveUser,file.getId()));

        } catch (IOException e) {
            throw new NuxeoException("Couldn't publish to Gdrive",e);
        }
    }
}

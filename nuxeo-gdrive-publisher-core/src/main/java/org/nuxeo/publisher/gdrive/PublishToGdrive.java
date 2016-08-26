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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.liveconnect.core.CredentialFactory;
import org.nuxeo.ecm.liveconnect.core.OAuth2CredentialFactory;
import org.nuxeo.ecm.liveconnect.google.drive.GoogleOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;

/**
 *
 */
@Operation(id=PublishToGdrive.ID, category=Constants.CAT_DOCUMENT, label="PublishToGdrive", description="Describe here what your operation does.")
public class PublishToGdrive {

    public static final String ID = "Document.PublishToGdrive";

    @Context
    protected CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {

        String user = session.getPrincipal().getName();

        GoogleOAuth2ServiceProvider oAuth2provider =
                (GoogleOAuth2ServiceProvider) Framework.getLocalService(OAuth2ServiceProviderRegistry.class)
                .getProvider("googledrive");

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

        Blob blob = (Blob) doc.getPropertyValue("file:content");

        File body = new File();
        body.setTitle(blob.getFilename());
        body.setMimeType(blob.getMimeType());

        try {
            InputStreamContent inputStreamContent = new InputStreamContent(blob.getMimeType(),blob.getStream());
            File file = drive.files().insert(body,inputStreamContent).execute();
        } catch (IOException e) {
            throw new NuxeoException("Couldn't publish to Gdrive",e);
        }

        return doc;
    }
}

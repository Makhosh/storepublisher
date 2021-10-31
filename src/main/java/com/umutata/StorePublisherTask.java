package com.umutata;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Apks.Upload;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Insert;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Tracks.Update;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.umutata.model.FileServerOriResult;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class StorePublisherTask extends DefaultTask {

    static final String MIME_TYPE_APK = "application/vnd.android.package-archive";

    private String fileType;

    @Input
    @Optional
    public abstract RegularFileProperty getApkFile();

    @Input
    @Optional
    public abstract Property<String> getApplicationId();

    @Input
    @Optional
    public abstract RegularFileProperty getCredential();

    @Input
    @Optional
    public abstract Property<String> getTrack();

    @Input
    @Optional
    public abstract Property<String> getAppId();

    @Input
    @Optional
    public abstract Property<String> getClientId();

    @Input
    @Optional
    public abstract Property<String> getClientSecret();

    @TaskAction
    private void storePublisher() {

        if (getApkFile().getOrNull() == null) {
            System.out.println("Apk File missed. Please add APK file to gradle config");
            return;
        } else {
            fileType = getApkFile().get().getAsFile().getAbsolutePath();
            fileType = fileType.substring(fileType.length() - 3);
            if (!fileType.equalsIgnoreCase("apk")) {
                System.out.println("File format not correct");
                return;
            }
        }

        if (getApplicationId().getOrNull() != null) {
            System.out.println("Started for upload Google Play for " + getApplicationId().get());
            uploadToGooglePlay();
        }

        if (getAppId().getOrNull() != null) {
            System.out.println("Started for upload Huawei App Gallery for " + getAppId().get());
            try {
                uploadToHuaweiAppGallery();
            } catch (IOException e) {
                System.out.println("Failed for upload Huawei App Gallery for " + getAppId().get());
                throw new GradleException(e.getLocalizedMessage());
            }
        }

        if (getApplicationId().getOrNull() == null && getAppId().getOrNull() == null) {
            System.out.println("Please enter Google Play or Huawei App Gallery info...");
        }
    }

    private void uploadToGooglePlay() {
        String packageName = getApplicationId().get();
        try {
            // Create the API service.
            AndroidPublisher service = getAndroidPublisher();
            if (service == null) {
                return;
            }
            final Edits edits = service.edits();

            // Create a new edit to make changes to your listing.
            Insert editRequest = edits
                    .insert(packageName,
                            null);

            AppEdit edit = editRequest.execute();
            final String editId = edit.getId();
            System.out.println("Created edit with id: " + editId);


            ApksListResponse apksResponse = edits
                    .apks()
                    .list(packageName,
                            edit.getId()).execute();

            // Print the apk info.
            for (Apk apk : apksResponse.getApks()) {
                System.out.println(
                        String.format("Version: %d - Binary sha1: %s", apk.getVersionCode(),
                                apk.getBinary().getSha1()));
            }

            // Upload new apk to developer console
            final AbstractInputStreamContent apkFile =
                    new FileContent(MIME_TYPE_APK, getApkFile().get().getAsFile());
            Upload uploadRequest = edits
                    .apks()
                    .upload(packageName,
                            editId,
                            apkFile);
            Apk apk = uploadRequest.execute();
            System.out.println("Version code " + apk.getVersionCode() + " has been uploaded");

            String trackRelease = "alpha";
            if (getTrack().getOrNull() != null) {
                trackRelease = getTrack().get();
            }

            // Assign apk to alpha track.
            List<Long> apkVersionCodes = new ArrayList<>();
            apkVersionCodes.add(Long.valueOf(apk.getVersionCode()));
            Update updateTrackRequest = edits
                    .tracks()
                    .update(packageName,
                            editId,
                            trackRelease,
                            new Track().setReleases(
                                    Collections.singletonList(
                                            new TrackRelease()
                                                    .setVersionCodes(apkVersionCodes)
                                                    .setStatus("completed"))));
            Track updatedTrack = updateTrackRequest.execute();
            System.out.println("Track " + updatedTrack.getTrack() + " has been updated.");

            // Commit changes for edit.
            Edits.Commit commitRequest = edits.commit(packageName, editId);
            AppEdit appEdit = commitRequest.execute();
            System.out.println("App edit with id " + appEdit.getId() + " has been committed");
            System.out.println("Successfully completed for upload Google Play for " + getApplicationId().get());

        } catch (Exception ex) {
            throw new GradleException(ex.getLocalizedMessage());
        }
    }

    private AndroidPublisher getAndroidPublisher() throws IOException {

        String credential = System.getenv("ANDROID_PUBLISHER_CREDENTIALS");

        InputStream inputStream;
        if (credential != null) {
            inputStream = new ByteArrayInputStream(credential.getBytes());
        } else {
            if (getCredential().getOrNull() == null) {
                System.out.println("Please add Google Play Credentials to gradle config");
                return null;
            } else {
                inputStream = getCredential().get().getAsFile().toURL().openStream();
            }
        }


        GoogleCredentials credentials = GoogleCredentials
                .fromStream(inputStream)
                .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
        AndroidPublisher.Builder ab = new AndroidPublisher.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), requestInitializer);
        return ab.setApplicationName(getApplicationId().get()).build();

    }

    private String callHuaweiServer(HttpRequestBase httpRequestBase) throws IOException {

        try (CloseableHttpClient httpClient = HttpClients.createSystem();
             CloseableHttpResponse response = httpClient.execute(httpRequestBase)) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8));
                return br.readLine();
            } else {
                throw new GradleException("Response:" + statusCode);
            }
        }
    }

    public String getToken() throws IOException {

        String secret = System.getenv("HUAWEI_PUBLISHER_CREDENTIALS");

        if (getClientSecret().getOrNull() != null) {
            secret = getClientSecret().get();
        }

        if (secret == null) {
            System.out.println("Please add Huawei App Gallery client secret to gradle config");
            throw new GradleException("Get Token Failed");
        }

        HttpPost post = new HttpPost("https://connect-api.cloud.huawei.com/api/oauth2/v1/token");

        JsonObject keyString = new JsonObject();
        keyString.addProperty("client_id", getClientId().get());
        keyString.addProperty("client_secret", secret);
        keyString.addProperty("grant_type", "client_credentials");

        StringEntity entity = new StringEntity(keyString.toString(), StandardCharsets.UTF_8);
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        post.setEntity(entity);

        String result = callHuaweiServer(post);
        JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
        return jsonObject.get("access_token").getAsString();
    }

    public JsonObject getUploadUrl(String clientId, String appId) throws IOException {

        String token = getToken();

        System.out.println("Get token for upload: " + token);

        HttpGet get = new HttpGet("https://connect-api.cloud.huawei.com/api/publish/v2/upload-url?appId=" + appId + "&suffix=" + fileType);
        get.setHeader("Authorization", "Bearer " + token);
        get.setHeader("client_id", clientId);

        String result = callHuaweiServer(get);
        return new Gson().fromJson(result, JsonObject.class);
    }

    public void uploadToHuaweiAppGallery() throws IOException {
        JsonObject object = getUploadUrl(getClientId().get(), getAppId().get());

        String authCode = object.get("authCode").getAsString();

        String uploadUrl = object.get("uploadUrl").getAsString();

        System.out.println("Auth code for upload: " + authCode);
        System.out.println("Upload URL: " + uploadUrl);

        HttpPost post = new HttpPost(uploadUrl);

        // File to upload.
        FileBody bin = new FileBody(getApkFile().get().getAsFile());

        // Construct a POST request.
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("file", bin)
                .addTextBody("authCode", authCode) // Obtain the authentication code.
                .addTextBody("fileCount", "1")
                .addTextBody("parseType", "1")
                .build();

        post.setEntity(reqEntity);
        post.addHeader("accept", "application/json");

        String result = callHuaweiServer(post);
        FileServerOriResult fileServerResult = new Gson().fromJson(result, FileServerOriResult.class);
        // Obtain the result code.
        if (!"0".equals(fileServerResult.getResult().getResultCode())) {
            System.out.println("Huawei App Gallery upload error");
        } else {
            System.out.println("Huawei App Gallery upload success");
        }
    }
}
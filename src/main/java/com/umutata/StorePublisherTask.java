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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.umutata.model.FileInfo;
import com.umutata.model.FileServerOriResult;
import com.umutata.model.UploadAppFileInfoResponse;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.*;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class StorePublisherTask extends DefaultTask {

    static final String MIME_TYPE_APK = "application/vnd.android.package-archive";

    private String fileType;

    Gson gson = new Gson();

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

    private JsonObject callHuaweiServer(HttpRequestBase httpRequestBase) throws IOException {

        try (CloseableHttpClient httpClient = HttpClients.createSystem();
             CloseableHttpResponse response = httpClient.execute(httpRequestBase)) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8));
                return gson.fromJson(br.readLine(), JsonObject.class);
            } else {
                throw new GradleException(response.getStatusLine().toString());
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

        JsonObject keyString = new JsonObject();
        keyString.addProperty("client_id", getClientId().get());
        keyString.addProperty("client_secret", secret);
        keyString.addProperty("grant_type", "client_credentials");

        HttpPost post = new HttpPost("https://connect-api.cloud.huawei.com/api/oauth2/v1/token");
        post.setEntity(getStringEntity(keyString.toString()));

        JsonObject result = callHuaweiServer(post);
        return result.get("access_token").getAsString();
    }

    public JsonObject getUploadUrl(String token) throws IOException {

        HttpGet get = new HttpGet("https://connect-api.cloud.huawei.com/api/publish/v2/upload-url?appId=" + getAppId().get() + "&suffix=" + fileType);
        get.setHeader("Authorization", "Bearer " + token);
        get.setHeader("client_id", getClientId().get());

        return callHuaweiServer(get);
    }

    public void uploadToHuaweiAppGallery() throws IOException {

        String token = getToken();
        System.out.println("Get token for upload: " + token);

        JsonObject object = getUploadUrl(token);
        String authCode = object.get("authCode").getAsString();
        String uploadUrl = object.get("uploadUrl").getAsString();

        System.out.println("Auth code for upload: " + authCode);
        System.out.println("Upload URL: " + uploadUrl);

        List<FileInfo> uploadFileList = uploadFile(authCode, uploadUrl);
        updateAppFileInfo(token, uploadFileList);
    }

    private List<FileInfo> uploadFile(String authCode, String uploadUrl) throws IOException {
        // File to upload.
        FileBody bin = new FileBody(getApkFile().get().getAsFile());

        // Construct a POST request.
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("file", bin)
                .addTextBody("authCode", authCode) // Obtain the authentication code.
                .addTextBody("fileCount", "1")
                .addTextBody("parseType", "1")
                .build();

        HttpPost post = new HttpPost(uploadUrl);
        post.setEntity(reqEntity);
        post.addHeader("accept", "application/json");

        JsonObject result = callHuaweiServer(post);
        FileServerOriResult fileServerResult = gson.fromJson(result, FileServerOriResult.class);
        // Obtain the result code.
        if (!"0".equals(fileServerResult.getResult().getResultCode()) || !fileServerResult.getResult().getUploadFileRsp().getIfSuccess().equalsIgnoreCase("1")) {
            throw new GradleException("Huawei App Gallery upload error");
        } else {
            return fileServerResult.getResult().getUploadFileRsp().getFileInfoList();
        }
    }

    private void updateAppFileInfo(String token, List<FileInfo> files) throws IOException {

        JsonObject keyString = new JsonObject();
        keyString.addProperty("fileType", 5);

        FileInfo fileInfo = files.get(0);

        JsonObject file = new JsonObject();
        file.addProperty("fileName", getApkFile().get().getAsFile().getName());
        file.addProperty("fileSize", fileInfo.getSize());
        file.addProperty("fileDestUrl", fileInfo.getFileDestUlr());

        JsonArray filesList = new JsonArray();
        filesList.add(file);

        keyString.add("files", file);

        HttpPut put = new HttpPut("https://connect-api.cloud.huawei.com/api/publish/v2/app-file-info?appId=" + getAppId().get());
        put.setHeader("Authorization", "Bearer " + token);
        put.setHeader("client_id", getClientId().get());
        put.setEntity(getStringEntity(keyString.toString()));

        JsonObject object = callHuaweiServer(put);
        UploadAppFileInfoResponse result = gson.fromJson(object, UploadAppFileInfoResponse.class);
        if (result.getRet().getCode().equalsIgnoreCase("0")){
            System.out.println("Successfully completed for upload Huawei App Gallery for " + getClientId().get());
        }
        else{
            throw new GradleException("Huawei App Gallery upload error" + result.getRet().getCode() + "- " + result.getRet().getMsg());
        }
    }

    private StringEntity getStringEntity(String keyString){
        StringEntity entity = new StringEntity(keyString, Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        return entity;
    }
}
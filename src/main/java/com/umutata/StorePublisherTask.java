package com.umutata;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Apks.Upload;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Insert;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Tracks.Update;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.Apk;
import com.google.api.services.androidpublisher.model.AppEdit;
import com.google.api.services.androidpublisher.model.Track;
import com.google.api.services.androidpublisher.model.TrackRelease;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.umutata.model.FileServerOriResult;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.gradle.api.DefaultTask;
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

    @Input @Optional
    public abstract RegularFileProperty getApkFile();

    @Input @Optional
    public abstract Property<String> getApplicationId();

    @Input @Optional
    public abstract RegularFileProperty getCredential();

    @Input @Optional
    public abstract Property<String> getTrack();

    @Input @Optional
    public abstract Property<String> getAppId();

    @Input @Optional
    public abstract Property<String> getClientId();

    @Input @Optional
    public abstract Property<String> getClientSecret();

    @TaskAction
    private void storePublisher(){

        if (getApkFile().getOrNull() == null){
            System.out.println("Apk File missed. Please APK file to gradle config");
            return;
        }

        if (getApplicationId().getOrNull() != null){
            System.out.println("Started for upload Google Play for " +  getApplicationId().get());
            uploadToGooglePlay();
        }

        if (getAppId().getOrNull() != null){
            System.setProperty("javax.net.debug","ssl,handshake,failure");
            System.out.println("Started for upload Huawei App Gallery for " +  getAppId().get());
            uploadToHuaweiAppGallery();
        }

        if(getApplicationId().getOrNull() == null && getAppId().getOrNull() == null){
            System.out.println("Please enter Google Play or Huawei App Gallery info...");
        }
    }

    private void uploadToGooglePlay(){
        String packageName = getApplicationId().get();
        try {

            // Create the API service.
            AndroidPublisher service = getAndroidPublisher();
            if (service == null){
                return;
            }
            final Edits edits = service.edits();

            // Create a new edit to make changes to your listing.
            Insert editRequest = edits
                    .insert(packageName,
                            null);

            AppEdit edit = editRequest.execute();
            final String editId = edit.getId();
            System.out.println("Created edit with id: " +  editId);

            // Upload new apk to developer console
            final AbstractInputStreamContent apkFile =
                    new FileContent(MIME_TYPE_APK, getApkFile().get().getAsFile());
            Upload uploadRequest = edits
                    .apks()
                    .upload(packageName,
                            editId,
                            apkFile);
            Apk apk = uploadRequest.execute();
            System.out.printf("Version code %d has been uploaded%n",
                    apk.getVersionCode());

            String trackRelease = "alpha";
            if (getTrack().getOrNull() != null){
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
            System.out.println(String.format("Track %s has been updated.", updatedTrack.getTrack()));

            // Commit changes for edit.
            Edits.Commit commitRequest = edits.commit(packageName, editId);
            AppEdit appEdit = commitRequest.execute();
            System.out.println("App edit with id " + appEdit.getId() + " has been committed");
            System.out.println("Successfully completed for upload Google Play for " +  getApplicationId().get());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private AndroidPublisher getAndroidPublisher() throws IOException {

        String credential = System.getenv("ANDROID_PUBLISHER_CREDENTIALS");

        InputStream inputStream;
        if (credential != null){
            inputStream = new ByteArrayInputStream(credential.getBytes());
        }else{
            if (getCredential().getOrNull() == null){
                System.out.println("Please add Google Play Credentials to gradle config");
                return null;
            }
            else{
                inputStream = getCredential().get().getAsFile().toURL().openStream();
            }
        }

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(inputStream)
                .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));

        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
        AndroidPublisher.Builder ab = new AndroidPublisher.Builder(transport, jsonFactory, requestInitializer);
        return ab.setApplicationName(getApplicationId().get()).build();

    }

    public String getToken() {
        String token = null;

        String secret = System.getenv("HUAWEI_PUBLISHER_CREDENTIALS");

        if (getClientSecret().getOrNull() != null){
            secret = getClientSecret().get();
        }

        if (secret == null){
            System.out.println("Please add Huawei App Gallery client secret to gradle config");
            return null;
        }

        try {
            HttpPost post = new HttpPost("https://connect-api.cloud.huawei.com/api/oauth2/v1/token");

            JSONObject keyString = new JSONObject();
            keyString.put("client_id", getClientId().get());
            keyString.put("client_secret", secret);
            keyString.put("grant_type", "client_credentials");

            StringEntity entity = new StringEntity(keyString.toString(), StandardCharsets.UTF_8);
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            post.setEntity(entity);

            CloseableHttpClient httpClient = HttpClients.createSystem();

            HttpResponse response = httpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Get Token Status:" + statusCode);
            if (statusCode == HttpStatus.SC_OK) {

                BufferedReader br =
                        new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8));
                String result = br.readLine();
                JSONObject object = JSON.parseObject(result);
                token = object.getString("access_token");
            }

            post.releaseConnection();
            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }

    public JSONObject getUploadUrl(String clientId, String appId) {
        HttpGet get = new HttpGet("https://connect-api.cloud.huawei.com/api/publish/v2/upload-url?appId=" + appId + "&suffix=apk");

        String token = getToken();
        if (token == null){
            return null;
        }
        get.setHeader("Authorization", "Bearer " + token);
        get.setHeader("client_id", clientId);
        try {

            CloseableHttpClient httpClient = HttpClients.createSystem();
            CloseableHttpResponse httpResponse = httpClient.execute(get);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), Consts.UTF_8));
                String result = br.readLine();

                // Object returned by the app information query API, which can be received using the AppInfo object. For details, please refer to the API reference.
                return JSON.parseObject(result);
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return null;
    }

    public void uploadToHuaweiAppGallery() {
        JSONObject object = getUploadUrl(getClientId().get(), getAppId().get());

        if (object == null){
            return;
        }

        String authCode = String.valueOf(object.get("authCode"));

        String uploadUrl = String.valueOf(object.get("uploadUrl"));

        HttpPost post = new HttpPost(uploadUrl);

        // File to upload.
        FileBody bin = new FileBody(getApkFile().get().getAsFile());

        // Construct a POST request.
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("file", bin)
                .addTextBody("authCode", authCode) // Obtain the authentication code.
                .addTextBody("fileCount", "1")
                .addTextBody("parseType","1")
                .build();

        post.setEntity(reqEntity);
        post.addHeader("accept", "application/json");

        try {

            CloseableHttpClient httpClient = HttpClients.createSystem();
            CloseableHttpResponse httpResponse = httpClient.execute(post);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {

                FileServerOriResult fileServerResult =
                        JSON.parseObject(EntityUtils.toString(httpResponse.getEntity()), FileServerOriResult.class);

                // Obtain the result code.
                if (!"0".equals(fileServerResult.getResult().getResultCode())) {
                    System.out.println("Huawei App Gallery upload error");
                }
                else{
                    System.out.println("Huawei App Gallery upload success");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

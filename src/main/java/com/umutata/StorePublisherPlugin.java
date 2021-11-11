package com.umutata;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class StorePublisherPlugin implements Plugin<Project> {
    public void apply(Project project) {
        StorePublisherExtension extension =
                project.getExtensions().create("storePublisher", StorePublisherExtension.class);

        project.getTasks().register("storePublisher", com.umutata.StorePublisherTask.class, task -> {
            task.getArtifactFile().set(extension.getArtifactFile());
            task.getApplicationId().set(extension.getGooglePlay().getApplicationId());
            task.getCredential().set(extension.getGooglePlay().getCredential());
            task.getTrack().set(extension.getGooglePlay().getTrack());
            task.getAppId().set(extension.getHuaweiAppGallery().getAppId());
            task.getClientId().set(extension.getHuaweiAppGallery().getClientId());
            task.getClientSecret().set(extension.getHuaweiAppGallery().getClientSecret());
        });
    }
}
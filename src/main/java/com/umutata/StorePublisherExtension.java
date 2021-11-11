package com.umutata;

import org.gradle.api.Action;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Nested;

public abstract class StorePublisherExtension {

    public abstract RegularFileProperty getArtifactFile();

    @Nested
    public abstract GooglePlay getGooglePlay();

    @Nested
    public abstract HuaweiAppGallery getHuaweiAppGallery();

    public void googlePlay(Action<? super GooglePlay> action) {
        action.execute(getGooglePlay());
    }

    public void huaweiAppGallery(Action<? super HuaweiAppGallery> action){
        action.execute(getHuaweiAppGallery());
    }
}

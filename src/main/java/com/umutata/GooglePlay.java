package com.umutata;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public abstract class GooglePlay {
    public abstract Property<String> getApplicationId();
    public abstract RegularFileProperty getCredential();
    public abstract Property<String> getTrack();
}
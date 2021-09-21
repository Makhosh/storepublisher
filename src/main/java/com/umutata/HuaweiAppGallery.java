package com.umutata;

import org.gradle.api.provider.Property;

public abstract class HuaweiAppGallery {
    public abstract Property<String> getAppId();
    public abstract Property<String> getClientId();
    public abstract Property<String> getClientSecret();
}

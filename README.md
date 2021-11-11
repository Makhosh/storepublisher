# Google Play and Huawei App Gallery APK/AAB Uploader

[![Maven Central](https://img.shields.io/maven-central/v/io.github.makhosh/storepublisher.svg)](https://search.maven.org/artifact/io.github.makhosh/storepublisher)
![Version](https://img.shields.io/badge/Version-1.0.4-green.svg)
[![License](https://img.shields.io/github/license/srs/gradle-node-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

# Experimental Build

The plugin allows you to publish the android release build file (*.apk or *.aab) to the Google Play and HUAWEI AppGallery.

# Features

The following features are available:

* Upload APK or AAB build file in HUAWEI AppGallery
* Publish APK or AAB build file in Google Play on any track(alpha, release...)

The following features are missing:

* Waits for feedback..

# Test

The plugin tested AGP 4.1.3 - Gradle 6.5.

# Adding the plugin to your project

in application module `./build.gradle`

## Using the `apply` method

```
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath "io.github.makhosh:storepublisher:1.0.4"
    }
}

apply plugin: 'com.android.application'
apply plugin: 'io.github.makhosh.storepublisher'
```
## Configuring Plugin

<details open><summary>Groovy</summary>

```groovy
storePublisher{
    artifactFile = file('')

    googlePlay{
            applicationId = ''
            track = ''
            credential = file('')
    }

    huaweiAppGallery{
            appId = ''
            clientId = ''
            clientSecret = ''
    }
}
```
</details>

How to get credentials for HUAWEI AppGallery, please see [AppGallery Connect API Getting Started](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agcapi-getstarted).

How to get credentials for Google Play, please see [How to create a Service Account for the Google Play Store](https://help.moreapp.com/en/support/solutions/articles/13000076096-how-to-create-a-service-account-for-the-google-play-store-moreapp).


| Parameter     | P         | Default Value | Description                     
|---------------|-----------|---------------|---------------------------------------------------------------------------------------------------------------------|
| apkFile       | Mandatory | null          | File path of artifact
| applicationId | Mandatory | null          | Defined your application default config. Example: com.facebook.katana
| track         | Optional  | "alpha"       | Target stage for Google Play, i.e. `internal`/`alpha`/`beta`/`production`
| credential    | Mandatory | null          | Google Play service account credential. Add JSON file path.
| appId         | Mandatory | null          | HUAWEI AppGallery Application ID. Example: Tiktok App ID: 100315379             
| clientId      | Mandatory | null          | HUAWEI AppGallery Connect API Client Id
| clientSecret  | Mandatory | null          | HUAWEI AppGallery Connect API Cliend Secret


> Note for Google Play: If you commit unencrypted Service Account keys to source, you run the risk of letting anyone
> access your Google Play account. To circumvent this issue, put the contents of your JSON file in
> the `ANDROID_PUBLISHER_CREDENTIALS` environment variable and don't specify the
> `credential` property.

> Note for HUAWEI AppGallery: If you commit unencrypted client secret to source, you run the risk of letting anyone
> access your Huawei App Gallery account. To circumvent this issue, put client secret value in
> the `HUAWEI_PUBLISHER_CREDENTIALS` environment variable and don't specify the
> `clientSecret` property.

# Usage

Gradle generate `storePublisher` task. You can upload a pre-existing artifact.

./gradlew storePublisher

If you want to upload after build, you can finalize your build task with `storePublisher` Example:

```groovy
project.afterEvaluate {
    tasks.findByName("assembleRelease").finalizedBy(tasks.findByName("storePublisher"))
}
```

# Known Huawei Issues

* I use correct `client_id` and `client_secret` but get [Huawei AppGallery Connect API - 403 client token authorization fail](https://stackoverflow.com/questions/63999681/huawei-appgallery-connect-api-403-client-token-authorization-fail)

## License

Free use of this plugin is permitted under the guidelines and in accordance with the [Apache License 2.0](https://opensource.org/licenses/Apache-2.0)

plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "io.github.makhosh"
version = "1.0.4"

val sonatypeUsername = project.properties["sonatypeUsername"]
val sonatypePassword = project.properties["sonatypePassword"]

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("storePublisher") {
            id = "io.github.makhosh.storepublisher"
            implementationClass = "com.umutata.StorePublisherPlugin"
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("com.google.auth:google-auth-library-oauth2-http:1.1.0")
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev142-1.25.0")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.apache.httpcomponents:httpmime:4.5.13")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "storepublisher"
            from(components["java"])
            pom {
                name.set("Store Publisher")
                description.set("Google Play And Huawei App Gallery File Uploader")
                url.set("https://github.com/Makhosh/storepublisher")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("makhosh")
                        name.set("Umut Atacan")
                        email.set("umutatacan@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Makhosh/storepublisher.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Makhosh/storepublisher.git")
                    url.set("https://github.com/Makhosh/storepublisher")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = sonatypeUsername as String?
                password = sonatypePassword as String?
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
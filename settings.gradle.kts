buildscript {
    repositories {
        mavenCentral()
    }
    configurations.classpath {
        resolutionStrategy {
            force("org.apache.httpcomponents.client5:httpclient5:5.6.3")
            force("org.apache.httpcomponents.core5:httpcore5:5.4.3")
            force("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ingatlan"


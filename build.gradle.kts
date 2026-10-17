buildscript {
    repositories {
        mavenCentral()
    }
    configurations.classpath {
        resolutionStrategy {
            force("org.apache.httpcomponents.client5:httpclient5:5.6.3")
            force("org.apache.httpcomponents.core5:httpcore5:5.4.3")
            force("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3")
            force("org.apache.commons:commons-lang3:3.21.0")
            force("org.springframework:spring-core:7.0.9")
            force("tools.jackson.core:jackson-core:3.1.7")
            force("tools.jackson.core:jackson-databind:3.1.7")
        }
    }
}

plugins {
    java
    application
    jacoco
    checkstyle
    id("com.github.spotbugs") version "6.5.13"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "hu.pushinsky.viktor"
version = "1.0.0"

extra["spring-framework.version"] = "7.0.9"
extra["tomcat.version"] = "11.0.25"
extra["jackson-bom.version"] = "3.1.7"
extra["postgresql.version"] = "42.7.12"
extra["log4j2.version"] = "2.25.5"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("ingatlanok.IngatlanApplication")
}

val jsoupVersion = "1.23.2"
val commonsIoVersion = "2.22.0"
val commonsLang3Version = "3.21.0"
val gsonVersion = "2.14.0"

dependencies {
    constraints {
        implementation("org.springframework:spring-webmvc:7.0.9")
        implementation("org.springframework:spring-core:7.0.9")
        implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.25")
        implementation("tools.jackson.core:jackson-core:3.1.7")
        implementation("tools.jackson.core:jackson-databind:3.1.7")
        implementation("org.postgresql:postgresql:42.7.12")
        implementation("org.apache.logging.log4j:log4j-api:2.25.5")
        implementation("org.apache.httpcomponents.client5:httpclient5:5.6.3")
        implementation("org.apache.httpcomponents.core5:httpcore5:5.4.3")
        implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3")
    }

    // Spring Boot Starter & Core Modules (Spring Framework 7.0.9)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Scraping & Utilities
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
    implementation("com.google.code.gson:gson:$gsonVersion")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("ingatlan-kereso")
            mainClass.set("ingatlanok.IngatlanApplication")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
        csv.outputLocation.set(file("build/reports/jacoco/test/jacocoTestReport.csv"))
    }
    doLast {
        val srcCsv = file("build/reports/jacoco/test/jacocoTestReport.csv")
        if (srcCsv.exists()) {
            val targetDir = file("target/site/jacoco")
            targetDir.mkdirs()
            srcCsv.copyTo(file("target/site/jacoco/jacoco.csv"), overwrite = true)
        }
    }
}

checkstyle {
    toolVersion = "10.21.4"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

configurations.matching { it.name.startsWith("checkstyle") }.configureEach {
    resolutionStrategy {
        force("org.codehaus.plexus:plexus-utils:3.6.1")
        force("commons-beanutils:commons-beanutils:1.11.0")
    }
}

tasks.checkstyleTest {
    enabled = false
}

tasks.matching { it.name.startsWith("checkstyleAot") }.configureEach {
    enabled = false
}

tasks.matching { it.name.startsWith("spotbugsAot") }.configureEach {
    enabled = false
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

spotbugs {
    toolVersion.set("4.9.2")
    ignoreFailures.set(false)
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs/spotbugs-exclude.xml"))
}

tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsTest") {
    enabled = false
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("xml") {
        required.set(true)
    }
    reports.create("html") {
        required.set(true)
    }
}

tasks.bootJar {
    archiveBaseName.set("ingatlan")
    archiveClassifier.set("")
    archiveVersion.set("1.0.0")
}

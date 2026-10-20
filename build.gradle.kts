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
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "hu.pushinsky.viktor"
version = providers.gradleProperty("releaseVersion").getOrElse("1.0.0")

extra["tomcat.version"] = "11.0.27"
extra["jackson-bom.version"] = "3.1.7"
extra["postgresql.version"] = "42.7.14"
extra["log4j2.version"] = "2.26.1"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

application {
    mainClass.set("ingatlanok.IngatlanApplication")
}

val jsoupVersion = "1.23.2"
val commonsIoVersion = "2.22.0"
val commonsLang3Version = "3.21.0"
val caffeineVersion = "3.2.3"

dependencies {
    constraints {
        implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.27")
        implementation("tools.jackson.core:jackson-core:3.1.7")
        implementation("tools.jackson.core:jackson-databind:3.1.7")
        implementation("org.postgresql:postgresql:42.7.14")
        implementation("org.apache.logging.log4j:log4j-api:2.26.1")
        implementation("org.apache.httpcomponents.client5:httpclient5:5.6.3")
        implementation("org.apache.httpcomponents.core5:httpcore5:5.4.3")
        implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3")
    }
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("org.brotli:dec:0.1.2")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform {
        excludeTags("live")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("liveTest") {
    description = "Runs live integration tests against real Hungarian property portals."
    group = "verification"
    useJUnitPlatform {
        includeTags("live")
    }
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

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.bootJar {
    archiveBaseName.set("ingatlan")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}

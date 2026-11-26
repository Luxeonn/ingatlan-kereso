buildscript {
    repositories {
        mavenCentral()
    }
    configurations.classpath {
        resolutionStrategy {
            // Bump buildscript transitive dependencies above plugin defaults (CVE pins)
            force("org.apache.httpcomponents.core5:httpcore5:5.5") // 5.4.4 -> 5.5
            force("org.apache.httpcomponents.core5:httpcore5-h2:5.5") // 5.4.4 -> 5.5
            force("tools.jackson.core:jackson-core:3.3.0") // 3.1.8 -> 3.3.0
            force("tools.jackson.core:jackson-databind:3.3.0") // 3.1.8 -> 3.3.0
        }
    }
}

plugins {
    java
    application
    jacoco
    checkstyle
    id("org.springframework.boot") version "4.2.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "hu.pushinsky.viktor"
version = providers.gradleProperty("releaseVersion").getOrElse("1.0.0")

// Bump Tomcat embed above Spring Boot 4.2.0 BOM default (11.0.27 -> 11.0.29)
extra["tomcat.version"] = "11.0.29"

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

val jsoupVersion = "1.24.1"
val caffeineVersion = "3.3.1"
val lombokVersion = "1.18.48"
val mapstructVersion = "1.6.3"
val archunitVersion = "1.5.1"

dependencies {
    constraints {
        // Track Tomcat embed CVE pin (enforced via extra["tomcat.version"] above BOM 11.0.27 -> 11.0.29)
        implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.29")
    }
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:unchecked",
            "-Xlint:deprecation",
            "-Xlint:rawtypes",
            "-Xlint:fallthrough",
            "-Xlint:cast",
            "-Xlint:overrides",
            "-Xlint:divzero",
            "-Xlint:empty",
            "-Werror"
        )
    )
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

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

checkstyle {
    toolVersion = "10.21.4"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

configurations.matching { it.name.startsWith("checkstyle") }.configureEach {
    resolutionStrategy {
        // Bump Checkstyle plugin transitive dependencies above tool defaults (CVE pins)
        force("org.codehaus.plexus:plexus-utils:4.2.0") // 3.3.0 -> 4.2.0
        force("commons-beanutils:commons-beanutils:1.11.0") // 1.10.1 -> 1.11.0
        force("com.google.guava:guava:33.8.0-jre") // 33.4.0-jre -> 33.8.0-jre (CVE-2026-102554)
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

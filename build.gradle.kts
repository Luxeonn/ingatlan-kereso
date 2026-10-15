plugins {
    java
    application
    jacoco
    checkstyle
    id("com.github.spotbugs") version "6.5.13"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "hu.pushinsky.viktor"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("ingatlanok.Main")
}

val jsoupVersion = "1.23.2"
val commonsIoVersion = "2.22.0"
val commonsLang3Version = "3.21.0"
val gsonVersion = "2.14.0"
val nitriteVersion = "5.4.0"
val junitVersion = "6.1.3"

dependencies {
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.dizitart:nitrite:$nitriteVersion")
    implementation("org.dizitart:nitrite-mvstore-adapter:$nitriteVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    configFile = file("checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.checkstyleTest {
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
    excludeFilter.set(file("spotbugs-exclude.xml"))
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

tasks.shadowJar {
    archiveBaseName.set("ingatlan")
    archiveClassifier.set("")
    archiveVersion.set("1.0")
    mergeServiceFiles()
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

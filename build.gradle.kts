import net.minecrell.gradle.licenser.LicenseProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile as CompileKotlin

plugins {
    idea
    java
    kotlin("jvm") version "1.4.30"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("net.minecrell.licenser") version "0.4.1"
    id("com.github.ben-manes.versions") version "0.36.0"
}

group = project.property("mavenGroup")!!
base.archivesBaseName = project.property("projectName") as String
version = project.property("currentVersion")!!

repositories {
    jcenter { name = "JCenter" }
    mavenCentral { name = "Maven Central" }
    maven("https://jitpack.io") { name = "Jitpack" }
    maven("https://oss.sonatype.org/content/repositories/snapshots") { name = "Sonatype Snapshots" }
    // TODO: remove?
    // for discord4j 3.2.0
    maven("https://repo.spring.io/milestone") { name = "Spring" }
    maven("https://repo.repsy.io/mvn/progamer28415/main") { name = "Repsy" }
}

dependencies {
    // junit
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.property("junitVersion")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${project.property("junitVersion")}")
    // discord4j
    implementation("com.discord4j:discord4j-core:${project.property("discord4jVersion")}")
    // reactor
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:${project.property("reactorKotlinExtensions")}")
    // music
    implementation("com.sedmelluq:lavaplayer:${project.property("lavaplayerVersion")}")
    // parsing libs
    implementation("com.beust:jcommander:${project.property("jCommanderVersion")}")
    // logging
    implementation("ch.qos.logback:logback-classic:${project.property("logbackClassicVersion")}")
    implementation("com.github.napstr:logback-discord-appender:${project.property("logbackDiscordAppenderVersion")}")
    // config
    implementation("com.electronwill.night-config:toml:${project.property("nightConfigVersion")}")
    // caching
    implementation("com.github.ben-manes.caffeine:caffeine:${project.property("caffeineVersion")}")
    // db
    implementation("io.r2dbc:r2dbc-postgresql:${project.property("driverVersion")}")
    implementation("io.r2dbc:r2dbc-pool:${project.property("poolVersion")}")
    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${project.property("coroutinesVersion")}")
    // scripting
    implementation("org.codehaus.groovy:groovy-all:${project.findProperty("groovyVersion")}")
    // encryption
    // implementation("com.google.crypto.tink:tink:${project.property("tinkVersion")}")
    // util
    implementation("org.apache.commons:commons-text:${project.property("commonsTextVersion")}")
    implementation("net.jodah:typetools:${project.property("typeToolsVersion")}")
    implementation("org.reflections:reflections:${project.property("reflectionsVersion")}")
    implementation("com.google.guava:guava:${project.property("guavaVersion")}")
    implementation("io.github.xf8b:utils:${project.property("utilsVersion")}")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    withType<CompileKotlin>().configureEach {
        kotlinOptions {
            jvmTarget = "15"
            languageVersion = "1.4"
        }
    }

    test {
        useJUnitPlatform()
    }

    dependencyUpdates {
        gradleReleaseChannel = "current"
        outputFormatter = "html"

        rejectVersionIf {
            return@rejectVersionIf candidate.version
                .contains("[.-]alpha|[.-]beta|[.-]rc\\d|[.-]m\\d".toRegex(RegexOption.IGNORE_CASE))
        }
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching("version.txt") {
            expand("version" to project.version)
        }
    }
}

application {
    @Suppress("DEPRECATION") //apparently shadow needs this
    mainClassName = project.property("mainClass") as String
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(15))
    }
}

license {
    matching("**/PingCommand.kt", delegateClosureOf<LicenseProperties> {
        header = rootProject.file("headers/PING_COMMAND_LICENSE_HEADER.txt")
    })

    header = rootProject.file("headers/LICENSE_HEADER.txt")

    ext {
        this["name"] = "xf8b"
        this["years"] = "2020, 2021"
        this["projectName"] = project.property("projectName")
    }

    include("**/*.kt")
}

import net.minecrell.gradle.licenser.LicenseProperties

plugins {
    application
    java
    idea
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("io.freefair.lombok") version "5.2.1"
    id("net.minecrell.licenser") version "0.4.1"
    id("com.github.ben-manes.versions") version "0.33.0"
}

group = property("mavenGroup")
version = property("currentVersion")

fun property(name: String): Any = project.findProperty(name)!!

repositories {
    jcenter()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    //TODO: remove?
    //for discord4j 3.2.0
    maven("https://repo.spring.io/milestone")
}

dependencies {
    //junit
    testImplementation("junit:junit:${property("junitVersion")}")
    //discord4j
    implementation("com.discord4j:discord4j-core:${property("discord4jVersion")}")
    implementation("com.discord4j:stores-caffeine:${property("discord4jStoresVersion")}")
    //music
    //implementation("com.sedmelluq:lavaplayer:${property("lavaplayerVersion")}")
    //see https://github.com/sedmelluq/lavaplayer/issues/517 for why this is here
    //TODO remove when new release
    implementation("com.github.sedmelluq:lavaplayer:${property("lavaplayerVersion")}")
    //command libs
    //TODO: remove?
    implementation("com.mojang:brigadier:${property("brigadierVersion")}")
    //parsing libs
    implementation("com.beust:jcommander:${property("jCommanderVersion")}")
    //logging
    implementation("ch.qos.logback:logback-classic:${property("logbackClassicVersion")}")
    implementation("com.github.napstr:logback-discord-appender:${property("logbackDiscordAppenderVersion")}")
    //config
    implementation("com.electronwill.night-config:toml:${property("nightConfigVersion")}")
    //caching
    implementation("com.github.ben-manes.caffeine:caffeine:${property("caffeineVersion")}")
    //drivers
    implementation("org.mongodb:mongodb-driver-reactivestreams:${property("mongoDbDriverVersion")}")
    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${property("coroutinesVersion")}")
    //util
    implementation("org.apache.commons:commons-text:${property("commonsTextVersion")}")
    implementation("net.jodah:typetools:${property("typeToolsVersion")}")
    implementation("org.reflections:reflections:${property("reflectionsVersion")}")
    implementation("com.google.guava:guava:${property("guavaVersion")}")
    implementation("org.codehaus.groovy:groovy-all:${property("groovyVersion")}")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "14"
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching("version.txt") {
            expand("version" to project.version)
        }
    }

    dependencyUpdates {
        gradleReleaseChannel = "current"
    }
}

application {
    @Suppress("DEPRECATION") //apparently shadow needs this
    mainClassName = property("mainClass").toString()
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

license {
    matching("**/PingCommandHandler.java", delegateClosureOf<LicenseProperties> {
        header = rootProject.file("headers/PING_COMMAND_HANDLER_LICENSE_HEADER.txt")
    })

    header = rootProject.file("headers/LICENSE_HEADER.txt")

    ext {
        set("name", "xf8b")
        set("years", "2020")
        set("projectName", property("projectName"))
    }

    include("**/*.java")
    include("**/*.kt")
}
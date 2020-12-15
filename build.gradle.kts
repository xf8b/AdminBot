import net.minecrell.gradle.licenser.LicenseProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    java
    idea
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("io.freefair.lombok") version "5.1.1"
    id("net.minecrell.licenser") version "0.4.1"
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
    //command libs
    //TODO: remove?
    implementation("com.mojang:brigadier:${property("brigaderVersion")}")
    //parsing libs
    implementation("com.beust:jcommander:${property("jcommanderVersion")}")
    //logging
    implementation("ch.qos.logback:logback-classic:${property("logbackClassicVersion")}")
    implementation("com.github.napstr:logback-discord-appender:${property("logbackDiscordAppenderVersion")}")
    //config
    implementation("com.electronwill.night-config:toml:${property("nightConfigVersion")}")
    //caching
    implementation("com.github.ben-manes.caffeine:caffeine:${property("caffeineVersion")}")
    //drivers
    implementation("org.mongodb:mongodb-driver-reactivestreams:4.2.0-SNAPSHOT")
    //util
    implementation("org.apache.commons:commons-text:${property("commonsTextVersion")}")
    implementation("net.jodah:typetools:${property("typeToolsVersion")}")
    implementation("org.reflections:reflections:${property("reflectionsVersion")}")
    implementation("com.google.guava:guava:${property("guavaVersion")}")
    implementation("org.codehaus.groovy:groovy-all:${property("groovyVersion")}")
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("version.txt") {
            expand("version" to project.version)
        }
    }
}

application {
    mainClassName = property("mainClass").toString()
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
    ignoreFailures = true
}
java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "14"
}

plugins {
    application
    java
    idea
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("io.freefair.lombok") version "5.1.1"
    id("net.minecrell.licenser") version "0.4.1"
}

group = project.findProperty("mavenGroup")!!
version = project.findProperty("currentVersion")!!

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
    testImplementation("junit:junit:${project.findProperty("junitVersion")}")
    //discord
    implementation("com.discord4j:discord4j-core:${project.findProperty("discord4jVersion")}")
    //command libs
    //TODO: remove?
    implementation("com.mojang:brigadier:${project.findProperty("brigaderVersion")}")
    //parsing libs
    implementation("com.beust:jcommander:${project.findProperty("jcommanderVersion")}")
    //logging
    implementation("ch.qos.logback:logback-classic:${project.findProperty("logbackClassicVersion")}")
    implementation("com.github.napstr:logback-discord-appender:${project.findProperty("logbackDiscordAppenderVersion")}")
    //config
    implementation("com.electronwill.night-config:toml:${project.findProperty("nightConfigVersion")}")
    //util
    implementation("org.apache.commons:commons-text:${project.findProperty("commonsTextVersion")}")
    implementation("net.jodah:typetools:${project.findProperty("typeToolsVersion")}")
    implementation("org.reflections:reflections:${project.findProperty("reflectionsVersion")}")
    implementation("org.xerial:sqlite-jdbc:${project.findProperty("sqliteJdbcVersion")}")
    implementation("com.google.guava:guava:${project.findProperty("guavaVersion")}")
    implementation("org.codehaus.groovy:groovy-all:${project.findProperty("groovyVersion")}")
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
    mainClassName = project.findProperty("mainClass").toString()
}

license {
    matching("**/PingCommandHandler.java", delegateClosureOf<net.minecrell.gradle.licenser.LicenseProperties> {
        header = rootProject.file("headers/PING_COMMAND_HANDLER_LICENSE_HEADER.txt")
    })
    header = rootProject.file("headers/LICENSE_HEADER.txt")
    ext {
        set("name", "xf8b")
        set("years", "2020")
        set("projectName", project.findProperty("projectName"))
    }
    include("**/*.java")
    ignoreFailures = true
}
java {
    sourceCompatibility = JavaVersion.VERSION_14
    targetCompatibility = JavaVersion.VERSION_14
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "14"
}
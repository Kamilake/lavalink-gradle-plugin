plugins {
    `kotlin-dsl`
    alias(libs.plugins.gradle.publish)
    `maven-publish`
}

group = "dev.arbjerg"
version = "1.1.2"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("lavalink-gradle-plugin") {
            id = "dev.arbjerg.lavalink.gradle-plugin"
            implementationClass = "dev.arbjerg.lavalink.gradle.LavalinkGradlePlugin"

            displayName = "Lavalink Gradle Plugin"
            description = "Gradle plugin which makes it easier to create Lavalink plugins"
            tags = setOf("lavalink", "discord")
        }

        website = "https://github.com/lavalink-devs/lavalink-gradle-plugin"
        vcsUrl = "https://github.com/lavalink-devs/lavalink-gradle-plugin"
    }
}

publishing {
    repositories {
        maven("https://maven.lavalink.dev/releases") {
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

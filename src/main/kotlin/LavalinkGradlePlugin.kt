package dev.arbjerg.lavalink.gradle

import dev.arbjerg.lavalink.gradle.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

private const val lavalinkExtensionName = "lavalinkPlugin"

internal val Project.extension get() = extensions.getByName<LavalinkExtension>(lavalinkExtensionName)

class LavalinkGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(plugins.hasPlugin("org.gradle.java")) { "Please apply the Java/Kotlin plugin before Lavalink" }
            configureExtension()
            val serverDependency = configureDependencies()
            configureTasks(serverDependency)
            configureSourceSets()
        }
    }
}

private fun Project.configureExtension(): LavalinkExtension {
    return extensions.create<LavalinkExtension>(lavalinkExtensionName).apply {
        version.convention(provider { project.version.toString() })
        name.convention(project.name)
        path.convention(provider { project.group.toString() })
        serverVersion.convention(apiVersion)
    }
}

private fun Project.configureDependencies(): Provider<Dependency> {
    project.repositories {
        mavenCentral()
        // Required for runtime
        maven("https://maven.arbjerg.dev/releases")
        maven("https://maven.arbjerg.dev/snapshots")
        // Required for Lavalink Dependencies
        @Suppress("DEPRECATION")
        jcenter()
        maven("https://jitpack.io")
    }

    dependencies {
        add("compileOnly", lavalink("plugin-api"))
    }

    return extension.serverVersion.map { serverVersion ->
        project.dependencies.create("dev.arbjerg.lavalink:Lavalink-Server:$serverVersion")
    }
}

private fun Project.configureSourceSets() {
    configure<SourceSetContainer> {
        named("main") {
            resources {
                srcDir(project.generatedPluginManifest)
            }
        }
    }
}

private fun Project.configureTasks(serverDependency: Provider<Dependency>) {
    tasks {
        val generatePluginProperties by registering(GeneratePluginPropertiesTask::class)
        named("processResources") {
            dependsOn(generatePluginProperties)
        }

        val jar = named<Jar>("jar") {
            // Workaround for Gradle only depending on the classes task, but this approach needing the actual jar file
            configurations.getByName("runtimeClasspath").dependencies.filterIsInstance<ProjectDependency>()
                .forEach {
                    val jarTask = it.dependencyProject.tasks.findByPath("jar")
                    if (jarTask != null) {
                        dependsOn(jarTask)
                    } else {
                        logger.warn("Could not autoconfigure project dependency for ${it.dependencyProject.path} you might need to create a task dependency manually")
                    }
                }
            doFirst {
                configurations.getByName("runtimeClasspath").resolvedConfiguration.resolvedArtifacts
                    .mapNotNull { dep -> dep.file }.forEach {
                        from(zipTree(it))
                    }
            }
        }

        val installPlugin by registering(Copy::class) {
            from(jar)
            into(project.testServerFolder)
            rename { "plugin.jar" }
        }

        val downloadTask =
            register("downloadLavalink", DownloadLavalinkTask::class.java, constructorArgs = arrayOf(serverDependency))

        register<RunLavalinkTask>("runLavaLink") {
            dependsOn(installPlugin, downloadTask)
        }
    }
}

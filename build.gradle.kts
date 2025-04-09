plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.17.4"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

group = "com.hxb"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.5")
    type.set("IC") // Target IDE Platform
    pluginName.set("starter")
    plugins.set(listOf("maven"))

}


publishing {
    publications {
        create<MavenPublication>("mavenPlugin") {
            from(components["java"])
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        archiveFileName.set("restart.jar")
        mergeServiceFiles()
        configurations = listOf(project.configurations.runtimeClasspath.get())
    }


    patchPluginXml {
        untilBuild.set("232.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
    val jar by getting(Jar::class) {
        from("src/main/resources") {
            include("plugin.xml")
        }
    }
}

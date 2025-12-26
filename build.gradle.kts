import java.util.Properties

// --- Load .env file ---
val envProps = Properties()
val envFile = rootProject.file(".env")

if (envFile.exists()) {
    envFile.forEachLine { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
            val (key, value) = trimmed.split("=", limit = 2)
            envProps[key.trim()] = value.trim()
        }
    }
    println("✔ Loaded .env variables")
} else {
    println("⚠ No .env file found")
}

plugins {
    java
    `maven-publish`
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "xyz.overdyn"
version = "1.0.2.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://libraries.minecraft.net/") {
        name = "minecraft-repo"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        name = "placeholderapi"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-text-minimessage:4.24.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.mojang:authlib:6.0.58")
}

tasks.runServer {
    minecraftVersion("1.18.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "dyngui"
            version = project.version.toString()

            pom {
                name.set("DynGui")
                description.set("GUI Framework for Overdyn")
                url.set("https://overdyn.xyz")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("Overdyn")
                        name.set("Overdyn Studio")
                        email.set("support@overdyn.xyz")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            val isSnapshot = project.version.toString().endsWith("SNAPSHOT")
            val repoType = if (isSnapshot) "snapshots" else "releases"

            name = "overdynRepo"
            url = uri("https://repo.overdyn.xyz/$repoType")

            credentials {
                username = envProps["REPOSILITE_USER"]?.toString()
                password = envProps["REPOSILITE_TOKEN"]?.toString()
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

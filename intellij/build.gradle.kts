plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.24"
  id("org.jetbrains.intellij") version "1.17.3"
}

import java.util.concurrent.TimeUnit
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask

group = "com.manorrock.assistant"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  mavenLocal()
}

val projectVersion = gradle.extra["projectVersion"] as String

dependencies {
    implementation("org.json:json:20231013")
    implementation("com.manorrock.assistant:impl:${projectVersion}")
    // ...other dependencies...
}

// Configure Gradle IntelliJ Plugin
intellij {
  version.set("2023.2.6")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf(/* Plugin Dependencies */))
}

// Use a simpler approach to avoid configuration cache problems
tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }

  // Make compile task depend on shared jar copy
  named("compileJava") {
  }
}

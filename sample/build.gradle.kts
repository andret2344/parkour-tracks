/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

plugins {
    java
    jacoco
    `maven-publish`
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
    id("com.gradleup.shadow") version "8.3.6"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://gitlab.com/api/v4/projects/12063927/packages/maven") }
}

dependencies {
    compileOnly(project(":"))
    compileOnly(libs.spigot.api)
    compileOnly(libs.jetbrains.annotations)

    implementation(libs.ats.arguments)
}

tasks {
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
        dependsOn(rootProject.tasks.jar)
        options.compilerArgs.addAll(listOf("-parameters", "-g", "-Xlint:deprecation", "-Xlint:unchecked"))
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveFileName.set("${project.name}-${project.version}.jar")
        relocate("eu.andret.arguments", "${project.group}.sample.arguments")
    }
}

project.tasks.publish.get().enabled = false

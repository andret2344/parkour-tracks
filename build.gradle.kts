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

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    compileOnly(libs.gson)
    compileOnly(libs.worldedit.bukkit)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.lombok)
    compileOnly(libs.spigot.api)
    implementation(libs.ats.arguments)
    implementation(libs.bstats.bukkit)
    annotationProcessor(libs.lombok)

    testCompileOnly(libs.jetbrains.annotations)
    testCompileOnly(libs.lombok)
    testImplementation(libs.spigot.api)
    testImplementation(libs.worldedit.bukkit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.testng)
    testAnnotationProcessor(libs.lombok)
}

tasks {
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
        options.compilerArgs.addAll(listOf("-parameters", "-g", "-Xlint:deprecation", "-Xlint:unchecked"))
    }

    test {
        useTestNG()
        finalizedBy(jacocoTestCoverageVerification, jacocoAggregatedReport)
        jvmArgs("-javaagent:${mockitoAgent.asPath}")
    }

    jacocoTestReport {
        classDirectories.setFrom(classDirectories.files.map {
            fileTree(it).matching {
                exclude("**/*Plugin.*")
            }
        })
    }

    jacocoTestCoverageVerification {
        violationRules {
            rule {
                classDirectories.setFrom(jacocoTestReport.get().classDirectories)
                limit {
                    minimum = "0.8".toBigDecimal()
                }
            }
        }
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveFileName.set("${project.name}-${project.version}.jar")
        relocate("org.bstats", "${project.group}.parkour.bstats")
        relocate("eu.andret.arguments", "${project.group}.parkour.arguments")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifact(jar)
                groupId = project.properties["group"].toString()
                version = project.properties["version"].toString()
                artifactId = project.properties["artifact"].toString()
            }
        }
        repositories {
            maven {
                name = "GitLab"

                url = uri("https://gitlab.com/api/v4/projects/6426481/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }
}

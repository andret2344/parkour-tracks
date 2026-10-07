plugins {
	java
	jacoco
	alias(libs.plugins.jacocolog)
	alias(libs.plugins.changelog)
	alias(libs.plugins.shadow)
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
	}
}

jacoco {
	toolVersion = libs.versions.jacoco.get()
}

val artifact = providers.gradleProperty("artifact").get()

configurations {
	// Tests run against the same server API that the plugin compiles against
	testImplementation {
		extendsFrom(configurations.compileOnly.get())
	}
}

dependencies {
	compileOnly(libs.paper.api)
	compileOnly(libs.jetbrains.annotations)
	compileOnly(libs.worldedit.bukkit) {
		// The server API comes from Paper; WorldEdit's own copy would clash with it
		exclude(group = "org.bukkit")
		exclude(group = "org.spigotmc")
		exclude(group = "io.papermc.paper")
	}
	compileOnly(libs.vault.api) {
		// The server API comes from Paper
		exclude(group = "org.bukkit")
	}
	implementation(libs.lamp.common)
	implementation(libs.lamp.bukkit)

	// Loaded by the server from Maven Central through `libraries` in plugin.yml, so it is not shaded
	testRuntimeOnly(libs.sqlite.jdbc)

	testImplementation(libs.assertj.core)
	testImplementation(libs.mockbukkit)
	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
	withType<JavaCompile> {
		// Lamp reads the parameter names to build the command usage
		options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation", "-Xlint:unchecked"))
	}

	processResources {
		val version = project.version.toString()
		val sqlite = libs.sqlite.jdbc.get().toString()
		inputs.property("version", version)
		inputs.property("sqlite", sqlite)
		filesMatching("plugin.yml") {
			expand("version" to version, "sqlite" to sqlite)
		}
	}

	test {
		useJUnitPlatform()
		finalizedBy(jacocoTestCoverageVerification, jacocoLogTestCoverage)
		// MockBukkit throws UnimplementedOperationException, a TestAbortedException, from what it does not implement,
		// so JUnit reports such a test as skipped. Fail instead of passing without running it.
		addTestListener(object : TestListener {
			override fun beforeSuite(suite: TestDescriptor) {}
			override fun beforeTest(testDescriptor: TestDescriptor) {}
			override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
			override fun afterSuite(suite: TestDescriptor, result: TestResult) {
				if (suite.parent == null && result.skippedTestCount > 0) {
					throw GradleException("${result.skippedTestCount} test(s) skipped, most likely by MockBukkit's UnimplementedOperationException")
				}
			}
		})
	}

	jacocoTestCoverageVerification {
		violationRules {
			rule {
				limit {
					minimum = "0.8".toBigDecimal()
				}
			}
		}
	}

	// The shadow jar is the only jar - the plain one would miss the shaded libraries
	jar {
		enabled = false
	}

	build {
		dependsOn(shadowJar)
	}

	withType<Jar> {
		// The suffix stops the LICENSE and NOTICE files of the shaded libraries from replacing ours
		metaInf {
			from("LICENSE", "NOTICE")
			rename { "$it-$artifact" }
		}
	}

	shadowJar {
		archiveFileName.set("${project.name}-${project.version}.jar")
		relocate("revxrsal.commands", "${project.group}.parkourtracks.lamp")
		// Kotlin metadata of Lamp, of no use to a plugin running without Kotlin
		exclude("META-INF/*.kotlin_module")
	}
}

changelog {
	groups.empty()
}

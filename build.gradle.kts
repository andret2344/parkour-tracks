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

	testImplementation(libs.assertj.core)
	testImplementation(libs.mockbukkit)
	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
	withType<JavaCompile> {
		options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation", "-Xlint:unchecked"))
	}

	processResources {
		val version = project.version.toString()
		inputs.property("version", version)
		filesMatching("plugin.yml") {
			expand("version" to version)
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
	}
}

changelog {
	groups.empty()
}

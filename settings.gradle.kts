pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
		maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
		maven { url = uri("https://maven.enginehub.org/repo/") }
		maven { url = uri("https://jitpack.io") }
	}
}

rootProject.name = "ParkourTracks"

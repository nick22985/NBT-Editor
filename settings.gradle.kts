pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.10"
	id("dev.kikugie.loom-back-compat") version "0.4.3"
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "nbtedit"

include(":core")

val modulesDir = file("modules")
modulesDir.walkTopDown().maxDepth(2)
	.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
	.map { ":modules:" + it.relativeTo(modulesDir).invariantSeparatorsPath.replace('/', ':') }
	.sorted()
	.forEach { include(it) }

include(":platform")

stonecutter {
	create(":platform") {
		versions("1.17.1", "1.18.2", "1.19.2", "1.19.3", "1.19.4", "1.20.1", "1.20.2", "1.20.4", "1.20.6", "1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2", "26.2", "26.3")
		vcsVersion = "26.3"
	}
}

val kernelProjects = subprojects.filter { it.path == ":core" || (it.path.startsWith(":modules:") && it.buildFile.exists()) }

configure(kernelProjects) {
	apply(plugin = "java-library")

	group = "com.nick22985.nbteditor"

	repositories {
		mavenCentral()
	}

	dependencies {
		if (path != ":core") {
			"api"(project(":core"))
		}
		"testImplementation"(platform("org.junit:junit-bom:5.11.4"))
		"testImplementation"("org.junit.jupiter:junit-jupiter")
		"testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
	}

	tasks.withType<JavaCompile>().configureEach {
		options.release = 16
		options.encoding = "UTF-8"
		options.compilerArgs.addAll(listOf("-Xlint:-options", "-Xmaxerrs", "10000"))
	}

	tasks.named<Test>("test") {
		useJUnitPlatform()
		testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
	}
}

tasks.register<Exec>("checkStructure") {
	group = "verification"
	description = "Fails on nested or anonymous types and static method imports outside tests"
	commandLine("python3", rootProject.file("tools/check-structure.py").absolutePath, rootProject.projectDir.absolutePath)
}

tasks.register<Exec>("checkLang") {
	group = "verification"
	description = "Fails on unknown keys, broken placeholders or bad metadata in translations, prints each language's coverage and writes build/lang/en_us.json"
	commandLine("python3", rootProject.file("tools/check-lang.py").absolutePath, rootProject.projectDir.absolutePath)
}

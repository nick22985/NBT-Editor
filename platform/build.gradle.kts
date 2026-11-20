import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.Properties

plugins {
	id("dev.kikugie.loom-back-compat")
}

val modId = sc.properties["mod.id"]

version = "${sc.properties["mod.version"]}+${sc.current.version}"
base.archivesName = modId

val requiredJava: JavaVersion = when {
	sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
	sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
	sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
	else -> JavaVersion.VERSION_16
}

val gameTests = sc.current.parsed >= "1.21.4"

val modulesRoot = rootProject.file("modules")

val kernelPaths = listOf(":core") + modulesRoot.walkTopDown().maxDepth(2)
	.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
	.map { ":modules:" + it.relativeTo(modulesRoot).invariantSeparatorsPath.replace('/', ':') }
	.sorted().toList()

val moduleDirs = modulesRoot.walkTopDown().maxDepth(2)
	.filter { it.isDirectory && it.resolve("module.properties").exists() }
	.sortedBy { it.relativeTo(modulesRoot).invariantSeparatorsPath }.toList()

val bundle: Configuration by configurations.creating {
	isTransitive = false
}

repositories {
	exclusiveContent {
		forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
		filter { includeGroup("maven.modrinth") }
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${sc.current.version}")
	loomx.applyMojangMappings()

	modImplementation("net.fabricmc:fabric-loader:${sc.properties["deps.fabric_loader"]}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties["deps.fabric_api"]}")
	modImplementation("maven.modrinth:modmenu:${sc.properties["deps.modmenu"]}")
	sc.properties.getOrNull("deps.nbtac")?.let { modImplementation("maven.modrinth:nbt-autocomplete:$it") }

	for (path in kernelPaths) {
		implementation(project(path))
		bundle(project(path))
	}
}

loom {
	fabricModJsonPath = rootProject.file("platform/src/main/resources/fabric.mod.json")
	accessWidenerPath = sc.process(rootProject.file("platform/src/main/resources/$modId.accesswidener"), "build/processed.accesswidener")
}

if (gameTests) {
	fabricApi {
		configureTests {
			createSourceSet = true
			modId = "${sc.properties["mod.id"]}-gametest"
			enableGameTests = false
			enableClientGameTests = true
			eula = true
		}
	}

	configurations.named("gametestRuntimeClasspath") {
		exclude(group = "maven.modrinth", module = "nbt-autocomplete")
		exclude(group = "remapped.maven.modrinth")
	}
}

if (!gameTests) {
	val main = sourceSets.main.get()
	val smoke = sourceSets.create("smoke") {
		compileClasspath += main.compileClasspath + main.output
		runtimeClasspath = output + main.runtimeClasspath.minus(main.output).minus(bundle)
	}

	val smokeMods = layout.projectDirectory.dir("build/run/smoke/mods")
	val installSmokeMod by tasks.registering(Sync::class) {
		from(loomx.modJar.flatMap { it.archiveFile })
		into(smokeMods)
	}

	loom {
		mods {
			register("$modId-smoke") { sourceSet(smoke) }
		}
		runs {
			register("smoke") {
				client()
				source(smoke)
				runDir = "build/run/smoke"
			}
		}
	}

	tasks.named("runSmoke") { dependsOn(installSmokeMod) }
}

java {
	withSourcesJar()
	sourceCompatibility = requiredJava
	targetCompatibility = requiredJava
}

val generatedResources = layout.buildDirectory.dir("generated/kernel-resources")

fun langKey(key: String, moduleId: String?): String = when {
	key.startsWith("key.") -> key
	moduleId == null -> "$modId.$key"
	else -> "$modId.$moduleId.$key"
}

@Suppress("UNCHECKED_CAST")
fun readLang(file: File): Map<String, Any?> = JsonSlurper().parse(file, "UTF-8") as Map<String, Any?>

val generateKernelResources by tasks.registering {
	val englishFiles = moduleDirs.map { it.name to it.resolve("src/main/lang/en_us.json") } + (null to rootProject.file("platform/src/main/lang/en_us.json"))
	val translationDir = rootProject.file("lang")
	inputs.files(moduleDirs.map { it.resolve("module.properties") })
	inputs.files(englishFiles.map { it.second }.filter { it.exists() })
	inputs.dir(translationDir).optional()
	outputs.dir(generatedResources)
	doLast {
		val out = generatedResources.get().asFile
		out.deleteRecursively()
		val index = out.resolve("$modId/modules.index")
		index.parentFile.mkdirs()
		index.writeText(moduleDirs.joinToString("") { dir ->
			val props = Properties().apply { dir.resolve("module.properties").reader().use { load(it) } }
			"${props.getProperty("entry")}\n"
		})
		val english = sortedMapOf<String, Any?>()
		for ((moduleId, file) in englishFiles.filter { it.second.exists() }) {
			for ((key, value) in readLang(file)) {
				val full = langKey(key, moduleId)
				if (english.put(full, value) != null) {
					throw GradleException("Translation key $full is defined twice (${file.relativeTo(rootDir)})")
				}
			}
		}
		val locales = sortedMapOf<String, Map<String, Any?>>("en_us" to english)
		val credits = StringBuilder()
		translationDir.listFiles { f -> f.name.endsWith(".json") }?.sortedBy { it.name }?.forEach { file ->
			val entries = readLang(file)
			val translators = (entries["@translators"] as? List<*>).orEmpty().map { it.toString() }
			if (translators.isNotEmpty()) {
				credits.append("${entries["@language"]}: ${translators.joinToString(", ")}\n")
			}
			locales[file.nameWithoutExtension] = entries.filterKeys { !it.startsWith("@") }.mapKeysTo(sortedMapOf<String, Any?>()) { langKey(it.key, null) }
		}
		for ((locale, entries) in locales) {
			val target = out.resolve("assets/$modId/lang/$locale.json")
			target.parentFile.mkdirs()
			target.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(entries)), Charsets.UTF_8)
		}
		out.resolve("$modId/translators.txt").writeText(credits.toString(), Charsets.UTF_8)
	}
}

sourceSets.main {
	resources.srcDir(generateKernelResources)
}

tasks {
	withType<JavaCompile>().configureEach {
		options.release = requiredJava.majorVersion.toInt()
		options.encoding = "UTF-8"
		options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
	}

	processResources {
		val props = mapOf(
			"version" to project.version.toString(),
			"mod_id" to modId,
			"mod_name" to sc.properties["mod.name"],
			"minecraft" to sc.properties["mod.mc_compat"],
			"fabric_api" to if (sc.current.parsed >= "26.1") "fabric-api" else "fabric",
			"java" to requiredJava.majorVersion
		)
		inputs.properties(props)

		filesMatching("fabric.mod.json") { expand(props) }
		val clientMixins = buildList {
			add("AbstractContainerScreenAccessor")
			add("ScreenFilesDropMixin")
			add("ContainerScreenClickMixin")
			add("ChatScreenAccessor")
			add("ChatScreenInputMixin")
			if (sc.current.parsed >= "1.21.4") add("PickBlockMixin")
			if (sc.current.parsed < "1.19") add("GuiChatMixin")
			else if (sc.current.parsed < "1.19.3") add("ChatListenerMixin")
		}
		val commonMixins = buildList {
			add("AbstractContainerMenuMixin")
			add("SlotMixin")
			if (sc.current.parsed < "1.20.5") add("BundleItemAddMixin")
			if (sc.current.parsed < "1.21.2") add("BundleItemMixin")
			if (sc.current.parsed >= "1.20.5" && sc.current.parsed < "1.21.2") add("BundleContentsMutableMixin")
			if (sc.current.parsed >= "1.21.2") add("BundleContentsMixin")
		}
		val mixinProps = mapOf(
			"java" to "JAVA_${minOf(requiredJava.majorVersion.toInt(), 21)}",
			"common_mixins" to commonMixins.joinToString(", ") { "\"$it\"" },
			"client_mixins" to clientMixins.joinToString(", ") { "\"$it\"" }
		)
		inputs.properties(mixinProps)
		filesMatching("*.mixins.json") { expand(mixinProps) }
	}

	named<Jar>("jar") {
		dependsOn(bundle)
		from(provider { bundle.files.map { zipTree(it) } }) {
			exclude("META-INF/MANIFEST.MF")
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		}
	}

	withType<Jar> {
		inputs.property("mod_id", modId)
		from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
	}

	register<Copy>("buildAndCollect") {
		group = "build"
		description = "Builds the mod jar and copies it to build/libs/{mod version}/"
		from(loomx.modJar.flatMap { it.archiveFile })
		into(rootProject.layout.buildDirectory.dir("libs/${sc.properties["mod.version"]}"))
	}
}

plugins {
	id("dev.kikugie.stonecutter")
}

stonecutter active "26.3"

stonecutter parameters {
	constants["nbtac"] = properties.getOrNull("deps.nbtac") != null

	replacements {
		string(current.parsed >= "1.21.11") {
			replace("ResourceLocation", "Identifier")
			replace(".location()", ".identifier()")
			replace("import net.minecraft.Util;", "import net.minecraft.util.Util;")
		}

		string(current.parsed < "1.20.2") {
			replace("net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket", "net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket")
			replace("net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket", "net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket")
		}

		string(current.parsed >= "1.20.5") {
			replace("isSameItemSameTags(", "isSameItemSameComponents(")
		}

		string(current.parsed >= "26.1") {
			replace("accessWidener v2 named", "accessWidener v2 official")
		}

		regex(current.parsed >= "1.21.2") {
			replace("\\b(BuiltInRegistries\\.[A-Z_]+)\\.get\\(" to "$1.getValue(", "(?<=\\bBuiltInRegistries\\.[A-Z_]{1,64})\\.getValue\\(" to ".get(")
		}

		regex(current.parsed >= "26.1") {
			replace("\\bGuiGraphics\\b" to "GuiGraphicsExtractor", "\\bGuiGraphicsExtractor\\b" to "GuiGraphics")
			replace("\\bClickType\\b" to "ContainerInput", "\\bContainerInput\\b" to "ClickType")
		}

		regex(current.parsed >= "26.2") {
			replace("\\b((?:Block)?EntityType)\\.([A-Z][A-Z0-9_]*)\\b" to "$1s.$2", "\\b((?:Block)?EntityType)s\\.([A-Z][A-Z0-9_]*)\\b" to "$1.$2")
		}

		regex(current.parsed >= "1.19.3") {
			replace("import net\\.minecraft\\.core\\.Registry;(?=\\R//builtin)" to "import net.minecraft.core.registries.BuiltInRegistries;", "import net\\.minecraft\\.core\\.registries\\.BuiltInRegistries;" to "import net.minecraft.core.Registry;")
			replace("import net\\.minecraft\\.core\\.Registry;(?=\\R//keys)" to "import net.minecraft.core.registries.Registries;", "import net\\.minecraft\\.core\\.registries\\.Registries;" to "import net.minecraft.core.Registry;")
			replace("\\bRegistry\\.([A-Z_]+)\\b(?<!_REGISTRY)" to "BuiltInRegistries.$1", "\\bBuiltInRegistries\\.([A-Z_]+)\\b" to "Registry.$1")
			replace("\\bRegistry\\.([A-Z_]+)_REGISTRY\\b" to "Registries.$1", "\\bRegistries\\.([A-Z_]+)\\b" to "Registry.$1_REGISTRY")
		}

		string(current.parsed >= "26.3") {
			replace(".scancode()", ".keycode()")
			replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
			replace("RegistryDataLoader.WORLDGEN_REGISTRIES", "RegistryDataLoader.WORLD_REGISTRIES")
			replace("com/mojang/blaze3d/pipeline/RenderPipeline", "com/mojang/renderpearl/api/pipeline/RenderPipeline")
		}
	}
}

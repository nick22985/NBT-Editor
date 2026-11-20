# NBT Editor

A Fabric mod for editing NBT in game. It edits items, blocks, entities, your own player, command storage, game rules and NBT files on disk. It also has a client-side chest for storing items, opens containers straight from your inventory, remembers items you lose and has a head database.

It's a new mod, written from scratch, that covers what [mega12345mega's NBT Editor](https://github.com/mega12345mega/NBT-Editor) does. Every feature is a module you can turn off. It isn't affiliated with the original, so please report problems here rather than to them.

## Install

Grab the jar for your Minecraft version from the Releases page and put it in your `mods` folder with [Fabric API](https://modrinth.com/mod/fabric-api). You need Fabric Loader 0.18 or newer. [Mod Menu](https://modrinth.com/mod/modmenu) and [NBT Autocomplete](https://modrinth.com/mod/nbt-autocomplete) are optional.

There's one jar per version: 1.17.1, 1.18.2, 1.19.2, 1.19.3, 1.19.4, 1.20.1, 1.20.2, 1.20.4, 1.20.6, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3.

## Editing

Hold an item, or look at a block or entity, and run `/nbt`. The tabs along the top switch between the raw NBT tree and friendlier tabs for display, enchantments, attributes, tooltip, potion, armor, mob, sign, book, block states and more. The panel on the right shows the selected tag and a live tooltip preview.

| Key | Action |
| --- | --- |
| ↑ ↓, Page Up, Page Down, Home, End | Move the selection (hold Shift to select a range) |
| Ctrl + A | Select everything |
| ← → | Fold or unfold the selection |
| Shift + ← → | Fold or unfold everything under the selection |
| Enter | Edit the selected value |
| Ctrl + E | Edit as text |
| Ctrl + R or F2 | Rename |
| Ctrl + D | Duplicate |
| Ctrl + C, X, V | Copy, cut and paste as SNBT |
| Ctrl + N or Insert | Add an entry |
| Delete or Backspace | Remove |
| Ctrl + F | Search |
| Ctrl + Z, Ctrl + Y | Undo and redo |
| Ctrl + S | Save |

On macOS, use Cmd instead of Ctrl. With NBT Autocomplete installed, value fields suggest keys and values. In edit as text, Ctrl + F opens find and replace, which can also use a regular expression.

The Display tab has Browse buttons next to custom model data and item model. They show each model as an item preview, with a search box. Custom model data previews leave out the item model, because an item model hides custom model data on 1.21.2 and newer.

Names, lore, signs and books use a text format with colors, gradients and click events. `/get help textformat` and `/get help gradient` explain it.

### On servers

If the server has the mod too, block, entity, player and storage edits go through the mod's own channel, and the server checks your permission level (`/nbt config`, Server bridge).

If it doesn't, and you're an operator, the mod makes the same edits with vanilla commands (`/data`, `/setblock`, `/summon`, `/gamerule`) and reads from `/data get` replies. It sends them a few at a time so the server's spam filter doesn't kick you. Game rules need op too. You can turn all of this off under Vanilla Commands.

If the server has a version of the mod that can't talk to yours, a toast tells you which side is older and the mod uses vanilla commands instead.

In vanilla, removing a tag from a block or entity doesn't always stick ([MC-128225](https://bugs.mojang.com/browse/MC-128225)). Turn on Recreate blocks and entities in the settings and saving replaces the block or respawns the entity instead. On a vanilla server that only works for blocks.

Without either, you can still edit items in creative mode, use the client chest and edit files.

## Commands

`/nbt help`, `/get help` and `/factory help` list every command. Click a line to fill it in. Some commands have a longer explanation, like `/nbt help open` and `/nbt help clientchest`.

| Command | Does |
| --- | --- |
| `/nbt` | Edits the held item, or the block or entity you're looking at |
| `/nbt slot <slot>`, `/nbt offhand` | Edits an inventory slot or your off hand |
| `/nbt block <x y z>`, `/nbt self`, `/nbt storage <id>`, `/nbt gamerules` | Edits a block, your player, a command storage or the game rules |
| `/nbt new <item>` | Puts a new item in your empty hand and edits it |
| `/nbt file [path]` | Opens the file browser (`.nbt`, `.snbt`, `.dat`, `.mca`, `.schem`, `.litematic`) |
| `/nbt import` | Opens the import screen. Dropping files onto an inventory works too |
| `/nbt export [file\|json\|cmd\|cmdblock\|get\|item]` | Exports what you're holding or looking at |
| `/nbt config` | Opens the settings |
| `/factory <tab>` | Opens an editor tab, or changes the held item directly with `name`, `lore`, `amount`, `damage`, `model`, `color`, `glint`, `repaircost`, `unbreakable`, `maxstacksize`, `max`, `randomuuid`, `signature`, `unbindskull`, `sign` and more |
| `/factory lore [list]` | Lists the held item's lore with buttons to add, edit and remove lines |
| `/factory attributes newuuids` | Gives the held item's attribute modifiers new ids |
| `/factory book new\|make_writable` | Puts a new written book in your hand, or turns the held written book back into a book and quill |
| `/factory randomuuid add\|remove` | Adds or removes a random UUID on the held item |
| `/get item\|snbt\|preset\|skull\|potion\|soup\|block\|entity` | Gives you items |
| `/get hdb [search]` | Opens the head database. `tagsearch <tag>` searches tags, `update` downloads it again |
| `/get hdb all <category> [search <text>]` | Gives you a shulker box of heads, with nested boxes when there are more than 27 |
| `/get models [search]` | Opens every custom model from your resource packs. In creative, click or drag one to take it as an item |
| `/get lostitem` | Opens your lost items |
| `/get credits` | Shows who made the mod |
| `/open [echest]` | Opens the container you're holding or looking at, or your ender chest |
| `/clientchest [page]` | Opens the client chest |

Short aliases like `/n`, `/g` and `/f`, and aliases of your own, can be turned on in the settings.

Under Controls → NBT Editor there are keybinds to open the editor on the hovered or held item, copy the hovered item's NBT, and copy the hovered item to the client chest.

## In your inventory

Tabs beside the inventory switch to the client chest (the barrel) and your ender chest. The ender chest needs the mod on the server, or op on a vanilla server.

| Key | Action |
| --- | --- |
| Space | Edit the hovered item |
| Shift + Space | Open the hovered item's factory |
| Ctrl + Space | Open the hovered item as a container |
| Delete | Delete the hovered item |
| Ctrl + Click | In creative, apply the enchanted book on your cursor to the clicked item |

Item tooltips list these keys. Both the keys and the hints can be turned off in the settings.

In the file browser, right-click a file to delete it. Image imports take a width, a height or both, and keep the aspect ratio when you give only one.

## Settings

Open them with `/nbt config` or through Mod Menu. The search box at the top finds a setting in any tab. Modules with settings get their own tab, and any module can be turned off from the Modules tab. Long chat input, under Commands, lets you type past the 256 character chat limit. NBT Editor commands get the whole text, and a warning shows when a message to the server will be cut. Settings are saved in `config/nbtedit/config.json`. Client chest pages, lost items and exports go in `nbtedit/` in the game folder.

## Translating

The mod is in English, German, Polish, Spanish, French, Italian, Brazilian Portuguese, Russian, Simplified Chinese, Japanese and Korean. Everything after Polish is a machine translation nobody has reviewed yet. Corrections are welcome, and they get your name into `/get credits`.

Each language other than English is a single file in `lang/`, named by its Minecraft locale code:

```json
{
  "@language": "Deutsch",
  "@translators": ["Your name"],
  "name": "NBT Editor",
  "potion.add": "Effekt hinzufügen"
}
```

`@language` is the language's name in that language. Everyone in `@translators` shows up in `/get credits`. A file doesn't have to be finished, since anything missing shows in English.

To start a new language, run `./gradlew checkLang`. It writes all the English strings to `build/lang/en_us.json` in this same format. Copy that to `lang/<locale>.json`, fill in the two `@` fields and translate the values. Run `./gradlew checkLang` again to see how far along each language is. It fails on keys English doesn't have, on bad `@` fields and on placeholders that don't match English. Putting `%2$s` before `%1$s` is fine. When English gets new strings, `python3 tools/check-lang.py . --missing <locale>` prints the ones a language is missing, with the English text, ready to paste in.

English itself lives next to the code that uses it, in `<module>/src/main/lang/en_us.json` and `platform/src/main/lang/en_us.json`. Keys there are short and the build adds the prefix, so `"add"` in the potion module becomes `nbtedit.potion.add`. A module's `module.name` is its name in the settings and `config.<setting>` (plus `.desc`) labels its settings. Keys starting with `key.` are keybind names and categories and are used as written.

## Development

Builds use [Stonecutter](https://stonecutter.kikugie.dev/) and Java 25.

```
./gradlew :platform:26.3:build                           # one version, jar in platform/versions/26.3/build/libs/
./gradlew :platform:26.3:buildAndCollect                 # same, copied to build/libs/<mod version>/
./gradlew checkStructure checkLang test                  # structure and translation checks plus unit tests
./gradlew :platform:26.3:runClientGameTest               # client gametests, 1.21.4 and later
./gradlew :platform:1.17.1:runSmoke                      # smoke run of the built jar, before 1.21.4
tools/install-labymod.sh                                 # builds 26.3 and copies it into the LabyMod Fabric mods folder
```

The gametests and the smoke run open a real client. On a machine without a display, run them under `xvfb-run`.

Gametests need Fabric's client gametest API, which starts at 1.21.4. Older versions get the smoke run instead. It loads the built jar, opens the settings, creates a world, opens every editor tab and runs the main commands. It writes `PASS` or `FAIL` per step to `platform/versions/<version>/build/run/smoke/smoke-result.txt`, with screenshots in `screenshots/` next to it, then closes the game.

`tools/install-labymod.sh` moves older NBT Editor jars out of the mods folder into `nbteditor-backup` next to it, so Fabric doesn't load two copies. Set `MC_VERSION` to install another version and `MODS_DIR` to use another folder.

### Layout

```
core/            NBT and text models, SNBT, binary NBT and region files, item fields per version,
                 the UI toolkit, the module system and the platform API. No Minecraft classes.
modules/<id>/    one Gradle project per feature. Sees core and nothing else.
modules/tabs/    the editor tab modules (display, enchantments, potion, mob and the rest)
lang/            translations, one file per language
platform/        Fabric and Stonecutter. Implements the platform API for each version, holds every
                 mixin and bundles core and the modules into the mod jar.
tools/           the structure and translation checks and the LabyMod install script
```

`core` and the modules can't use `net.minecraft`, `com.mojang` or `net.fabricmc`. Minecraft and Fabric aren't on their classpath, so the build fails if they try. Differences in data between versions, like where an item's name is stored on 1.17 and on 1.21.5, are plain data in `core` that's checked at runtime and unit tested.

### Modules and addons

A module is a class that implements `com.nick22985.nbteditor.module.Module`. It registers its commands, editor tabs, targets, settings, hooks and network handlers through the `ModuleContext` it gets. The build finds the built-in modules from their `module.properties` files under `modules/` and `modules/tabs/`. Other mods can add modules with the `nbtedit:module` entrypoint in their `fabric.mod.json`:

```json
"entrypoints": {
  "nbtedit:module": ["com.example.MyModule"]
}
```

### Releasing

Set `mod.version` in `platform/stonecutter.properties.toml`, add a section for it to `CHANGELOG.md` and push the tag `v<version>`. The release workflow builds every version and publishes a GitHub release with the jars and that changelog section.

## Credits

- [mega12345mega](https://github.com/mega12345mega/NBT-Editor) wrote the original NBT Editor, which this mod's features are based on.
- [Nebulajinxed](https://github.com/nebulajinxed/nbt-editor-port) ported it to newer versions, including the first 26.x support.
- Heads in the head database come from [minecraft-heads.com](https://minecraft-heads.com/).

## License

MIT. See [LICENSE](LICENSE).

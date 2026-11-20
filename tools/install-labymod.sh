#!/usr/bin/env bash
set -euo pipefail

MC_VERSION="${MC_VERSION:-26.3}"
CACHE_DIR="${CACHE_DIR:-.gradle-install}"
MODS_DIR="${MODS_DIR:-$HOME/.var/app/net.labymod.LabyMod/data/LabyMod/instances/0406140e-dce3-439b-8d4d-05fba677c9ee/loader/fabric/$MC_VERSION/mods}"
BACKUP_DIR="$MODS_DIR/../nbteditor-backup"

cd "$(dirname "$0")/.."

if [ ! -d "$MODS_DIR" ]; then
	echo "Mods folder not found: $MODS_DIR" >&2
	exit 1
fi

if [ ! -d "platform/versions/$MC_VERSION" ]; then
	echo "Unknown Minecraft version: $MC_VERSION" >&2
	exit 1
fi

property() {
	grep -E "^\s*$1\s*=" platform/stonecutter.properties.toml | head -n1 | cut -d= -f2 | tr -d '[:space:]"'
}

MOD_ID="$(property mod.id)"
VERSION="$(property mod.version)"

chmod +x ./gradlew
if command -v mise >/dev/null 2>&1; then
	mise exec java@25 -- ./gradlew --project-cache-dir "$CACHE_DIR" ":platform:$MC_VERSION:build"
else
	./gradlew --project-cache-dir "$CACHE_DIR" ":platform:$MC_VERSION:build"
fi

JAR="platform/versions/$MC_VERSION/build/libs/$MOD_ID-$VERSION+$MC_VERSION.jar"
if [ ! -f "$JAR" ]; then
	echo "Built jar not found: $JAR" >&2
	exit 1
fi

shopt -s nullglob nocaseglob
OLD_JARS=("$MODS_DIR"/*nbt*edit*.jar "$MODS_DIR"/"$MOD_ID"-*.jar)
shopt -u nullglob nocaseglob
if [ ${#OLD_JARS[@]} -gt 0 ]; then
	mkdir -p "$BACKUP_DIR"
	for old in "${OLD_JARS[@]}"; do
		[ -f "$old" ] || continue
		echo "Moving old jar to backup: $(basename "$old")"
		mv -f "$old" "$BACKUP_DIR/"
	done
fi

cp "$JAR" "$MODS_DIR/"
echo "Installed $(basename "$JAR") to $MODS_DIR"

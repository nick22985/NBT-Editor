#!/usr/bin/env python3
import glob
import json
import os
import re
import sys

MOD_ID = "nbtedit"
META = {"@language", "@translators"}
placeholder = re.compile(r"%(?:(\d+)\$)?(.?)")
locale_name = re.compile(r"^[a-z]{2,3}_[a-z0-9]{2,}$")

args = sys.argv[1:]
missing_for = set()
if "--missing" in args:
    i = args.index("--missing")
    missing_for = set(args[i + 1:])
    args = args[:i]
root = args[0] if args else os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
errors = []


def rel(path):
    return os.path.relpath(path, root)


def load(path):
    try:
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    except ValueError as e:
        errors.append("%s: invalid JSON: %s" % (rel(path), e))
        return {}
    if not isinstance(data, dict):
        errors.append("%s: must be a JSON object" % rel(path))
        return {}
    return data


def placeholders(path, key, value):
    found = set()
    position = 0
    for m in placeholder.finditer(value):
        kind = m.group(2)
        if kind == "%" and m.group(1) is None:
            continue
        if kind != "s":
            errors.append("%s: %s uses %s, only %%s, %%1$s and %%%% work in Minecraft" % (rel(path), key, m.group(0)))
            continue
        if m.group(1) is None:
            position += 1
            found.add(position)
        else:
            found.add(int(m.group(1)))
    return found


def short_key(full):
    return full if full.startswith("key.") else full[len(MOD_ID) + 1:]


def full_key(key, module):
    if key.startswith("key."):
        return key
    return MOD_ID + "." + (key if module is None else module + "." + key)


english = {}
sources = [(None, os.path.join(root, "platform/src/main/lang/en_us.json"))]
for props in sorted(glob.glob(os.path.join(root, "modules/**/module.properties"), recursive=True)):
    directory = os.path.dirname(props)
    sources.append((os.path.basename(directory), os.path.join(directory, "src/main/lang/en_us.json")))
for module, path in sources:
    if not os.path.exists(path):
        continue
    for key, value in load(path).items():
        if not isinstance(value, str):
            errors.append("%s: %s must be a string" % (rel(path), key))
            continue
        if key.startswith(MOD_ID + "."):
            errors.append("%s: %s should be written without the %s. prefix" % (rel(path), key, MOD_ID if module is None else MOD_ID + "." + module))
        if key.startswith("key.") and not (key.startswith("key." + MOD_ID + ".") or key.startswith("key.category." + MOD_ID + ".")):
            errors.append("%s: %s must start with key.%s. or key.category.%s." % (rel(path), key, MOD_ID, MOD_ID))
        full = full_key(key, module)
        if full in english:
            errors.append("%s: %s is already defined in %s" % (rel(path), full, rel(english[full][1])))
            continue
        english[full] = (placeholders(path, key, value), path, value)

report = []
for path in sorted(glob.glob(os.path.join(root, "lang/*.json"))):
    locale = os.path.basename(path)[:-5]
    if locale == "en_us":
        errors.append("%s: English lives next to each module, not in lang/" % rel(path))
        continue
    if not locale_name.match(locale):
        errors.append("%s: file name must be a Minecraft locale code like de_de" % rel(path))
    data = load(path)
    language = data.get("@language")
    if not isinstance(language, str) or not language.strip():
        errors.append("%s: @language must be the language's own name, like Deutsch" % rel(path))
    translators = data.get("@translators")
    if not isinstance(translators, list) or not all(isinstance(name, str) and name.strip() for name in translators):
        errors.append("%s: @translators must be a list of names" % rel(path))
    done = set()
    for key, value in data.items():
        if key.startswith("@"):
            if key not in META:
                errors.append("%s: unknown field %s" % (rel(path), key))
            continue
        full = full_key(key, None)
        if full not in english:
            errors.append("%s: %s is not an English key" % (rel(path), key))
            continue
        if not isinstance(value, str):
            errors.append("%s: %s must be a string" % (rel(path), key))
            continue
        if placeholders(path, key, value) != english[full][0]:
            errors.append("%s: %s must use the same placeholders as English" % (rel(path), key))
        if "§" in value and "§" not in english[full][2]:
            errors.append("%s: %s uses §, which Minecraft reads as a format code" % (rel(path), key))
        done.add(full)
    missing = [key for key in english if key not in done]
    report.append("%s %s: %d/%d (%d%%)" % (locale, language, len(done), len(english), len(done) * 100 // max(1, len(english))))
    if locale in missing_for:
        for key in missing:
            report.append("  %s: %s," % (json.dumps(short_key(key), ensure_ascii=False), json.dumps(english[key][2], ensure_ascii=False)))

reference = {"@language": "English", "@translators": []}
for key, entry in english.items():
    reference[short_key(key)] = entry[2]
reference_path = os.path.join(root, "build/lang/en_us.json")
os.makedirs(os.path.dirname(reference_path), exist_ok=True)
with open(reference_path, "w", encoding="utf-8") as f:
    f.write(json.dumps(reference, indent=2, ensure_ascii=False) + "\n")
report.append("all English strings, in the same format: " + rel(reference_path))

for line in errors + report:
    print(line)
if errors:
    print("%d translation problem(s)" % len(errors), file=sys.stderr)
    sys.exit(1)
print("lang ok, %d English keys" % len(english))

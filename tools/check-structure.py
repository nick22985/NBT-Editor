#!/usr/bin/env python3
import os
import re
import sys

root = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
scan = [os.path.join(root, d) for d in ("core", "modules", "platform/src")]
declaration = re.compile(r"(?<![\w.])(class|interface|enum|record)\s+[A-Z]\w*")
anonymous = re.compile(r"(?<![\w.])new\s+[A-Za-z_][\w.]*\s*(<[^;(){}]*>)?\s*\(")
static_import = re.compile(r"^\s*import\s+static\s+[\w.]+\.([a-z]\w*|\*)\s*;", re.M)


def strip(source):
    out = []
    i = 0
    n = len(source)
    while i < n:
        c = source[i]
        if source.startswith("//", i):
            j = source.find("\n", i)
            i = n if j < 0 else j
        elif source.startswith("/*", i):
            j = source.find("*/", i + 2)
            i = n if j < 0 else j + 2
        elif source.startswith('"""', i):
            j = source.find('"""', i + 3)
            out.append('""')
            i = n if j < 0 else j + 3
        elif c == '"' or c == "'":
            j = i + 1
            while j < n and source[j] != c:
                j += 2 if source[j] == "\\" else 1
            out.append(c + c)
            i = j + 1
        else:
            out.append(c)
            i += 1
    return "".join(out)


def problems(path, test):
    source = open(path, encoding="utf-8").read()
    code = strip(source)
    found = []
    if not test:
        for m in static_import.finditer(source):
            found.append((source.count("\n", 0, m.start()) + 1, "static method import"))
    depth = 0
    depth_at = []
    for c in code:
        depth_at.append(depth)
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
    for m in declaration.finditer(code):
        if depth_at[m.start()] > 0:
            found.append((code.count("\n", 0, m.start()) + 1, "nested " + m.group(1)))
    for m in anonymous.finditer(code):
        j = m.end()
        level = 1
        while j < len(code) and level:
            if code[j] == "(":
                level += 1
            elif code[j] == ")":
                level -= 1
            j += 1
        rest = code[j:j + 64].lstrip()
        if rest.startswith("{"):
            found.append((code.count("\n", 0, m.start()) + 1, "anonymous class"))
    return found


failed = False
for base in scan:
    for directory, dirs, files in os.walk(base):
        dirs[:] = [d for d in dirs if d not in ("build", ".gradle")]
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(directory, name)
            test = os.sep + "test" + os.sep in path or os.sep + "gametest" + os.sep in path
            for line, message in problems(path, test):
                print("%s:%d: %s" % (os.path.relpath(path, root), line, message))
                failed = True
if failed:
    print("types must be top-level and static helpers must be called through their util class", file=sys.stderr)
    sys.exit(1)
print("structure ok")

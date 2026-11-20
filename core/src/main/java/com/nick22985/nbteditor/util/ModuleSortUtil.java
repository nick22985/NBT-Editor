package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleInfo;
import com.nick22985.nbteditor.module.ModuleState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModuleSortUtil {
	private ModuleSortUtil() {
	}

	public static Map<String, ModuleInfo> resolve(Map<String, Module> modules, Function<Module, ModuleState> initial) {
		Map<String, ModuleInfo> result = new LinkedHashMap<>();
		Map<String, Integer> marks = new HashMap<>();
		List<String> ids = new ArrayList<>(modules.keySet());
		Collections.sort(ids);
		for (String id : ids) {
			visit(id, modules, initial, marks, result, new ArrayDeque<>());
		}
		return result;
	}

	private static ModuleInfo visit(String id, Map<String, Module> modules, Function<Module, ModuleState> initial, Map<String, Integer> marks, Map<String, ModuleInfo> result, Deque<String> stack) {
		ModuleInfo done = result.get(id);
		if (done != null) {
			return done;
		}
		Module module = modules.get(id);
		if (marks.getOrDefault(id, 0) == 1) {
			List<String> cycle = new ArrayList<>(stack);
			Collections.reverse(cycle);
			cycle.add(id);
			return new ModuleInfo(module, ModuleState.CYCLE, "dependency cycle " + String.join(" -> ", cycle));
		}
		marks.put(id, 1);
		stack.push(id);
		ModuleInfo info = null;
		for (String dependency : module.dependencies()) {
			if (!modules.containsKey(dependency)) {
				info = new ModuleInfo(module, ModuleState.MISSING_DEPENDENCY, "missing dependency " + dependency);
				break;
			}
			ModuleInfo dependencyInfo = visit(dependency, modules, initial, marks, result, stack);
			if (dependencyInfo.state() == ModuleState.CYCLE) {
				info = new ModuleInfo(module, ModuleState.CYCLE, dependencyInfo.reason());
				break;
			}
			if (!dependencyInfo.enabled()) {
				info = new ModuleInfo(module, ModuleState.DEPENDENCY_DISABLED, "dependency " + dependency + " is " + dependencyInfo.state().name().toLowerCase(Locale.ROOT));
				break;
			}
		}
		if (info == null) {
			for (String dependency : module.optionalDependencies()) {
				if (modules.containsKey(dependency)) {
					visit(dependency, modules, initial, marks, result, stack);
				}
			}
			ModuleState state = initial.apply(module);
			info = new ModuleInfo(module, state, state == ModuleState.ENABLED ? "" : state.name().toLowerCase(Locale.ROOT));
		}
		stack.pop();
		marks.put(id, 2);
		result.put(id, info);
		return info;
	}

	public static List<Module> instantiate(ClassLoader loader, String resource, Consumer<String> onError) {
		List<Module> out = new ArrayList<>();
		try {
			Enumeration<URL> urls = loader.getResources(resource);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						line = line.trim();
						if (line.isEmpty() || line.startsWith("#")) {
							continue;
						}
						try {
							Class<?> type = Class.forName(line, true, loader);
							out.add((Module) type.getDeclaredConstructor().newInstance());
						} catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
							onError.accept("Could not create module " + line + ": " + e);
						}
					}
				}
			}
		} catch (IOException e) {
			onError.accept("Could not read " + resource + ": " + e);
		}
		return out;
	}
}

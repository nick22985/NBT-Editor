package com.nick22985.nbteditor.module;

import com.nick22985.nbteditor.util.ModuleSortUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
class ModuleSortTest {
	private static Map<String, Module> modules(Module... modules) {
		Map<String, Module> out = new LinkedHashMap<>();
		for (Module module : modules) {
			out.put(module.id(), module);
		}
		return out;
	}

	private static Module module(String id, List<String> dependencies, List<String> optional) {
		return new TestModule(id, dependencies, optional, new ArrayList<>());
	}

	@Test
	void dependenciesComeFirst() {
		Map<String, ModuleInfo> result = ModuleSortUtil.resolve(modules(
			module("c", List.of("b"), List.of()),
			module("b", List.of("a"), List.of()),
			module("a", List.of(), List.of()),
			module("d", List.of(), List.of("c"))), m -> ModuleState.ENABLED);
		assertEquals(List.of("a", "b", "c", "d"), new ArrayList<>(result.keySet()));
	}

	@Test
	void missingAndDisabledDependenciesCascade() {
		Map<String, ModuleInfo> result = ModuleSortUtil.resolve(modules(
			module("a", List.of("ghost"), List.of()),
			module("b", List.of("a"), List.of()),
			module("x", List.of(), List.of()),
			module("y", List.of("x"), List.of())), m -> m.id().equals("x") ? ModuleState.DISABLED : ModuleState.ENABLED);
		assertEquals(ModuleState.MISSING_DEPENDENCY, result.get("a").state());
		assertEquals(ModuleState.DEPENDENCY_DISABLED, result.get("b").state());
		assertEquals(ModuleState.DISABLED, result.get("x").state());
		assertEquals(ModuleState.DEPENDENCY_DISABLED, result.get("y").state());
	}

	@Test
	void cyclesAreReported() {
		Map<String, ModuleInfo> result = ModuleSortUtil.resolve(modules(
			module("a", List.of("b"), List.of()),
			module("b", List.of("a"), List.of()),
			module("c", List.of(), List.of())), m -> ModuleState.ENABLED);
		assertEquals(ModuleState.CYCLE, result.get("a").state());
		assertEquals(ModuleState.CYCLE, result.get("b").state());
		assertEquals(ModuleState.ENABLED, result.get("c").state());
	}

	@Test
	void optionalCyclesDoNotBlock() {
		Map<String, ModuleInfo> result = ModuleSortUtil.resolve(modules(
			module("a", List.of(), List.of("b")),
			module("b", List.of(), List.of("a"))), m -> ModuleState.ENABLED);
		assertEquals(ModuleState.ENABLED, result.get("a").state());
		assertEquals(ModuleState.ENABLED, result.get("b").state());
	}
}

package com.nick22985.nbteditor.module;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
final class TestModule implements Module {
	private final String id;
	private final List<String> dependencies;
	private final List<String> optional;
	private final List<String> initOrder;

	TestModule(String id, List<String> dependencies, List<String> optional, List<String> initOrder) {
		this.id = id;
		this.dependencies = dependencies;
		this.optional = optional;
		this.initOrder = initOrder;
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public List<String> dependencies() {
		return this.dependencies;
	}

	@Override
	public List<String> optionalDependencies() {
		return this.optional;
	}

	@Override
	public void init(ModuleContext ctx) {
		this.initOrder.add(this.id);
	}
}

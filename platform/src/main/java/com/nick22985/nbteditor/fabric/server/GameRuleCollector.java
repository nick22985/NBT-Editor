package com.nick22985.nbteditor.fabric.server;

//? if <1.21.11 {
/*import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.level.GameRules;

/^*
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 ^/
public final class GameRuleCollector implements GameRules.GameRuleTypeVisitor {
	private final GameRules rules;
	private final Map<String, String> values = new TreeMap<>();

	public GameRuleCollector(GameRules rules) {
		this.rules = rules;
	}

	public Map<String, String> values() {
		return this.values;
	}

	@Override
	public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
		this.values.put(key.getId(), this.rules.getRule(key).serialize());
	}
}
*///?}

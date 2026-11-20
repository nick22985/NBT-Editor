package com.nick22985.nbteditor.text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ScoreContent(String name, String objective) implements Content {
	@Override
	public String kind() {
		return "score";
	}

	@Override
	public String plain() {
		return "";
	}
}

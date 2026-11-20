package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FancyTextSyntax implements TextSyntax {
	private static final String MARKERS = "&§[\\<";

	private final FancyGrammar grammar;

	public FancyTextSyntax(FancyGrammar grammar) {
		this.grammar = grammar;
	}

	public FancyGrammar grammar() {
		return this.grammar;
	}

	@Override
	public Text parse(String input) {
		return new FancyParser(this.grammar, input).parse();
	}

	@Override
	public String stringify(Text text) {
		return new FancyWriter(this.grammar).write(text);
	}

	@Override
	public boolean recognizes(String input) {
		for (int i = 0; i < input.length(); i++) {
			if (MARKERS.indexOf(input.charAt(i)) >= 0) {
				return true;
			}
		}
		return false;
	}
}

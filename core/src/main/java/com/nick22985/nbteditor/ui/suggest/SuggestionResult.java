package com.nick22985.nbteditor.ui.suggest;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record SuggestionResult(int generation, String text, List<TextSuggestion> entries) {
}

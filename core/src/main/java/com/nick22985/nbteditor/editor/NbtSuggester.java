package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.NbtCompletionService;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.ui.suggest.SuggestionSource;
import com.nick22985.nbteditor.ui.suggest.TextSuggestion;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.NbtSuggestUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtSuggester {
	private static final int LIMIT = 200;

	private final EditorSession session;
	private final NbtCompletionService completions;

	public NbtSuggester(EditorSession session) {
		this.session = session;
		this.completions = session.client().completions();
	}

	public SuggestionSource values(Supplier<NbtPath> shown) {
		if (!this.completions.available()) {
			return null;
		}
		return (text, cursor) -> {
			NbtPath path = shown.get();
			if (path == null || path.isRoot()) {
				return CompletableFuture.completedFuture(List.of());
			}
			Object last = path.last();
			String key = last instanceof String ? (String) last : "";
			String typed = text.substring(0, Math.min(cursor, text.length()));
			if (this.session.document().get(path) instanceof StringTag) {
				return this.suggest(path.parent(), key, NbtSuggestUtil.openString(typed), List.of()).thenApply(list -> NbtSuggestUtil.unquoteAll(list, typed.length()));
			}
			return this.suggest(path.parent(), key, typed, List.of());
		};
	}

	public SuggestionSource keys(NbtPath container, String allowed) {
		return (text, cursor) -> {
			Tag parent = this.session.document().get(container);
			List<String> siblings = new ArrayList<>();
			if (parent instanceof CompoundTag) {
				for (String key : ((CompoundTag) parent).keys()) {
					if (!key.equals(allowed)) {
						siblings.add(key);
					}
				}
			}
			return this.suggest(container, text.substring(0, Math.min(cursor, text.length())), null, siblings);
		};
	}

	private CompletableFuture<List<TextSuggestion>> suggest(NbtPath container, String key, String value, Collection<String> siblings) {
		NbtSuggestSubject subject = NbtSuggestUtil.subject(this.session);
		if (subject == null || !container.startsWith(subject.dataPath()) || key.indexOf('{') >= 0 || key.indexOf('[') >= 0) {
			return CompletableFuture.completedFuture(List.of());
		}
		List<Object> relative = container.segments().subList(subject.dataPath().size(), container.size());
		Tag data = this.session.document().get(subject.dataPath());
		if (subject.components() && relative.isEmpty() && value == null) {
			return CompletableFuture.completedFuture(this.componentKeys(key, siblings));
		}
		if (!this.completions.available()) {
			return CompletableFuture.completedFuture(List.of());
		}
		if (!subject.components()) {
			NbtSuggestPrefix prefix = NbtSuggestUtil.prefix(data, relative, key, value);
			if (prefix == null) {
				return CompletableFuture.completedFuture(List.of());
			}
			return this.completions.nbt(subject.name(), prefix.snbt()).thenApply(raw -> this.filter(raw, prefix, key, value, siblings));
		}
		if (relative.isEmpty()) {
			if (key.startsWith("!")) {
				return CompletableFuture.completedFuture(List.of());
			}
			NbtSuggestPrefix prefix = new NbtSuggestPrefix(value, 0);
			return this.completions.component(subject.id(), NbtSuggestUtil.componentId(key), value).thenApply(raw -> this.filter(raw, prefix, key, value, siblings));
		}
		Object component = relative.get(0);
		if (!(component instanceof String) || ((String) component).startsWith("!") || !(data instanceof CompoundTag)) {
			return CompletableFuture.completedFuture(List.of());
		}
		Tag start = ((CompoundTag) data).get((String) component);
		NbtSuggestPrefix prefix = NbtSuggestUtil.prefix(start, relative.subList(1, relative.size()), key, value);
		if (prefix == null) {
			return CompletableFuture.completedFuture(List.of());
		}
		return this.completions.component(subject.id(), NbtSuggestUtil.componentId((String) component), prefix.snbt()).thenApply(raw -> this.filter(raw, prefix, key, value, siblings));
	}

	private List<TextSuggestion> componentKeys(String typed, Collection<String> siblings) {
		List<TextSuggestion> out = new ArrayList<>();
		boolean removal = typed.startsWith("!");
		RegistryService registries = this.session.client().registries();
		for (String id : registries.ids(RegistryService.DATA_COMPONENT_TYPE)) {
			String full = ItemStackUtil.namespaced(id);
			if (NbtSuggestUtil.componentMatches(typed, full) && !NbtSuggestUtil.componentTaken(siblings, full)) {
				out.add(new TextSuggestion(0, typed.length(), removal ? "!" + full : full, null));
			}
		}
		out.sort((a, b) -> a.text().compareTo(b.text()));
		return out.size() > LIMIT ? out.subList(0, LIMIT) : out;
	}

	private List<TextSuggestion> filter(List<TextSuggestion> raw, NbtSuggestPrefix prefix, String key, String value, Collection<String> siblings) {
		Set<String> seen = new LinkedHashSet<>();
		List<TextSuggestion> out = new ArrayList<>();
		for (TextSuggestion suggestion : raw) {
			TextSuggestion shifted = suggestion.shift(-prefix.fieldStart());
			String text = shifted.text();
			if (text.isEmpty() || shifted.start() < 0) {
				continue;
			}
			if (value == null) {
				if (text.endsWith(":")) {
					text = text.substring(0, text.length() - 1);
				}
				text = NbtSuggestUtil.unquote(text);
				if (text.isEmpty() || text.startsWith("!") || text.indexOf('{') >= 0 || text.indexOf('[') >= 0 || text.indexOf(',') >= 0 || text.indexOf('}') >= 0 || siblings.contains(text)) {
					continue;
				}
				shifted = new TextSuggestion(0, key.length(), text, suggestion.tooltip());
			} else if (NbtSuggestUtil.closesOutside(text, value)) {
				continue;
			}
			if (seen.add(shifted.start() + ":" + shifted.text())) {
				out.add(shifted);
			}
			if (out.size() >= LIMIT) {
				break;
			}
		}
		return out;
	}
}

package com.nick22985.nbteditor.ui.suggest;

import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.text.TextBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SuggestionSession {
	private final Widget field;
	private final TextBuffer buffer;
	private final SuggestionSource source;
	private final SuggestionPopup popup = new SuggestionPopup(this::accept);
	private final AtomicReference<SuggestionResult> pending = new AtomicReference<>();
	private int generation;
	private boolean applying;
	private boolean dismissed;

	public SuggestionSession(Widget field, TextBuffer buffer, SuggestionSource source) {
		this.field = field;
		this.buffer = buffer;
		this.source = source;
	}

	public void changed() {
		if (!this.applying && this.field.focused()) {
			this.dismissed = false;
			this.request();
		}
	}

	public void request() {
		int id = ++this.generation;
		String text = this.buffer.text();
		int cursor = this.buffer.cursor();
		CompletableFuture<List<TextSuggestion>> future;
		try {
			future = this.source.suggest(text, cursor);
		} catch (RuntimeException e) {
			this.hide();
			return;
		}
		if (future == null) {
			this.hide();
			return;
		}
		future.whenComplete((entries, failure) -> this.pending.set(new SuggestionResult(id, text, failure == null && entries != null ? entries : List.of())));
	}

	public void hide() {
		this.generation++;
		this.pending.set(null);
		this.popup.entries(List.of());
		UiScreen screen = this.field.screen();
		if (screen != null) {
			screen.hidePopup(this.popup);
		}
	}

	public boolean showing() {
		UiScreen screen = this.field.screen();
		return screen != null && screen.popup() == this.popup && !this.popup.entries().isEmpty();
	}

	public void sync(int screenX, int screenY) {
		SuggestionResult result = this.pending.getAndSet(null);
		UiScreen screen = this.field.screen();
		if (screen == null) {
			return;
		}
		if (result != null && result.generation() == this.generation && result.text().equals(this.buffer.text())) {
			if (result.entries().isEmpty() || this.dismissed || !this.field.focused()) {
				this.hide();
			} else {
				this.popup.entries(result.entries());
				screen.showPopup(this.popup);
			}
		}
		if (this.showing()) {
			this.popup.place(screenX, screenY, this.field.height(), screen.width(), screen.height());
		}
	}

	public boolean keyPressed(KeyInput input) {
		if (input.key() == Key.SPACE && input.modifiers().onlyPrimary()) {
			this.dismissed = false;
			this.request();
			return true;
		}
		if (!this.showing()) {
			return false;
		}
		if (input.plain(Key.DOWN)) {
			this.popup.move(1);
			return true;
		}
		if (input.plain(Key.UP)) {
			this.popup.move(-1);
			return true;
		}
		if (input.plain(Key.TAB) || input.isEnter() && this.popup.navigated()) {
			this.accept(this.popup.selected());
			return true;
		}
		if (input.plain(Key.ESCAPE)) {
			this.dismissed = true;
			this.hide();
			return true;
		}
		return false;
	}

	private void accept(TextSuggestion suggestion) {
		if (suggestion == null) {
			return;
		}
		int length = this.buffer.text().length();
		this.applying = true;
		try {
			this.buffer.select(Math.min(suggestion.start(), length), Math.min(Math.max(suggestion.start(), suggestion.end()), length));
			this.buffer.insert(suggestion.text());
		} finally {
			this.applying = false;
		}
		this.request();
	}
}

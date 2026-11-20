package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record BookContent(boolean written, String title, String author, int generation, boolean resolved, List<Text> pages) {
	public static final int MAX_PAGES = 100;
	public static final int MAX_TITLE = 32;
	public static final int GENERATIONS = 4;

	public static BookContent empty(boolean written) {
		return new BookContent(written, written ? "" : null, written ? "" : null, 0, false, List.of());
	}

	public BookContent withTitle(String title) {
		return new BookContent(this.written, title, this.author, this.generation, this.resolved, this.pages);
	}

	public BookContent withAuthor(String author) {
		return new BookContent(this.written, this.title, author, this.generation, this.resolved, this.pages);
	}

	public BookContent withGeneration(int generation) {
		return new BookContent(this.written, this.title, this.author, generation, this.resolved, this.pages);
	}

	public BookContent withResolved(boolean resolved) {
		return new BookContent(this.written, this.title, this.author, this.generation, resolved, this.pages);
	}

	public BookContent withPages(List<Text> pages) {
		return new BookContent(this.written, this.title, this.author, this.generation, this.resolved, pages);
	}

	public BookContent withPage(int index, Text page) {
		List<Text> next = new ArrayList<>(this.pages);
		next.set(index, page);
		return this.withPages(next);
	}

	public BookContent asWritten(boolean written) {
		return new BookContent(written, written && this.title == null ? "" : this.title, written && this.author == null ? "" : this.author, this.generation, this.resolved, this.pages);
	}
}

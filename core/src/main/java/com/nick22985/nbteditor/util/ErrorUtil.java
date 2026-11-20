package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.text.Text;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ErrorUtil {
	private ErrorUtil() {
	}

	public static Throwable unwrap(Throwable error) {
		Throwable current = error;
		while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

	public static Text message(Throwable error) {
		Throwable cause = unwrap(error);
		if (cause instanceof TargetException) {
			return ((TargetException) cause).reason();
		}
		if (cause == null) {
			return Text.translatable("nbtedit.error.unknown");
		}
		String message = cause.getMessage();
		return Text.literal(message == null || message.isEmpty() ? cause.getClass().getSimpleName() : message);
	}
}

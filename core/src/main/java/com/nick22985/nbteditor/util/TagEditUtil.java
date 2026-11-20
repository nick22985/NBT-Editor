package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.nbt.TagType;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TagEditUtil {
	private TagEditUtil() {
	}

	public static String uniqueKey(CompoundTag compound, String key) {
		if (!compound.contains(key)) {
			return key;
		}
		String stem = key;
		int number = 2;
		int digits = key.length();
		while (digits > 0 && Character.isDigit(key.charAt(digits - 1))) {
			digits--;
		}
		if (digits > 0 && digits < key.length() && key.length() - digits <= 9) {
			stem = key.substring(0, digits);
			number = Integer.parseInt(key.substring(digits)) + 1;
		}
		while (compound.contains(stem + number)) {
			number++;
		}
		return stem + number;
	}

	public static boolean acceptsElement(Tag container, Tag element, SnbtDialect dialect) {
		if (container instanceof ListTag) {
			ListTag list = (ListTag) container;
			return dialect == SnbtDialect.MODERN || list.isEmpty() || list.get(0).type() == element.type();
		}
		if (container.type().isArray()) {
			return element instanceof NumericTag && ((NumericTag) element).type().isInteger();
		}
		return container instanceof CompoundTag;
	}

	public static Tag adaptElement(Tag container, Tag element) {
		Tag template = TagDisplayUtil.elementTemplate(container);
		if (container.type().isArray() && template != null && element instanceof NumericTag && element.type() != template.type()) {
			return NumericTag.of(template.type(), ((NumericTag) element).number());
		}
		return element;
	}

	public static TagType fixedElementType(Tag container, SnbtDialect dialect) {
		if (container.type().isArray()) {
			Tag template = TagDisplayUtil.elementTemplate(container);
			return template == null ? null : template.type();
		}
		if (container instanceof ListTag && dialect == SnbtDialect.LEGACY && !((ListTag) container).isEmpty()) {
			return ((ListTag) container).get(0).type();
		}
		return null;
	}
}

package com.nick22985.nbteditor.data;

import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EntityFields {
	public static final Field<Map<String, Double>> ATTRIBUTES = new EntityAttributesField();

	private EntityFields() {
	}
}

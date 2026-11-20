package com.nick22985.nbteditor.data;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record PackModel(String item, String itemModel, Integer value, String model) {
	public String label() {
		String id = this.itemModel != null ? this.itemModel : this.item;
		return this.value == null ? id : id + " #" + this.value;
	}
}

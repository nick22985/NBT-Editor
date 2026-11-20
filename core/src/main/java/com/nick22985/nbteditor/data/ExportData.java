package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.target.SubjectKind;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ExportData(SubjectKind kind, CompoundTag tag, int dataVersion) {
}

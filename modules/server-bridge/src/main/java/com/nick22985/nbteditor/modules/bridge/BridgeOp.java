package com.nick22985.nbteditor.modules.bridge;

import com.nick22985.nbteditor.target.SubjectKind;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum BridgeOp {
	READ_BLOCK(SubjectKind.BLOCK, false),
	WRITE_BLOCK(SubjectKind.BLOCK, true),
	READ_ENTITY(SubjectKind.ENTITY, false),
	WRITE_ENTITY(SubjectKind.ENTITY, true),
	READ_PLAYER(SubjectKind.PLAYER, false),
	WRITE_PLAYER(SubjectKind.PLAYER, true),
	SET_SLOT(SubjectKind.PLAYER, true),
	STORAGE_IDS(SubjectKind.STORAGE, false),
	READ_STORAGE(SubjectKind.STORAGE, false),
	WRITE_STORAGE(SubjectKind.STORAGE, true),
	GAME_RULES(SubjectKind.WORLD, false),
	SET_GAME_RULE(SubjectKind.WORLD, true),
	SUMMON_ENTITY(SubjectKind.ENTITY, true);

	private final SubjectKind kind;
	private final boolean write;

	BridgeOp(SubjectKind kind, boolean write) {
		this.kind = kind;
		this.write = write;
	}

	public SubjectKind kind() {
		return this.kind;
	}

	public boolean write() {
		return this.write;
	}
}

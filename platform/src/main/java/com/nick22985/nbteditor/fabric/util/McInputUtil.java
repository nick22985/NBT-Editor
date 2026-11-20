package com.nick22985.nbteditor.fabric.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.Modifiers;
import com.nick22985.nbteditor.ui.MouseButton;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
//? if <26.3 {
/*import org.lwjgl.glfw.GLFW;
*///?}

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McInputUtil {
	private static final int UNMAPPED_OFFSET = 100000;
	private static final Map<Integer, Integer> TO_CORE = new HashMap<>();
	private static final Map<Integer, Integer> TO_NATIVE = new HashMap<>();

	static {
		//? if >=26.3 {
		map(InputConstants.KEY_0, Key.NUM_0);
		map(InputConstants.KEY_1, Key.NUM_1);
		map(InputConstants.KEY_2, Key.NUM_2);
		map(InputConstants.KEY_3, Key.NUM_3);
		map(InputConstants.KEY_4, Key.NUM_4);
		map(InputConstants.KEY_5, Key.NUM_5);
		map(InputConstants.KEY_6, Key.NUM_6);
		map(InputConstants.KEY_7, Key.NUM_7);
		map(InputConstants.KEY_8, Key.NUM_8);
		map(InputConstants.KEY_9, Key.NUM_9);
		for (int i = 0; i < 26; i++) {
			map(InputConstants.KEY_A + i, Key.A + i);
		}
		for (int i = 0; i < 12; i++) {
			map(InputConstants.KEY_F1 + i, Key.F1 + i);
		}
		map(InputConstants.KEY_NUMPADENTER, Key.KP_ENTER);
		map(InputConstants.KEY_DOWN, Key.DOWN);
		map(InputConstants.KEY_LEFT, Key.LEFT);
		map(InputConstants.KEY_RIGHT, Key.RIGHT);
		map(InputConstants.KEY_UP, Key.UP);
		map(InputConstants.KEY_APOSTROPHE, Key.APOSTROPHE);
		map(InputConstants.KEY_BACKSLASH, Key.BACKSLASH);
		map(InputConstants.KEY_COMMA, Key.COMMA);
		map(InputConstants.KEY_EQUALS, Key.EQUAL);
		map(InputConstants.KEY_GRAVE, Key.GRAVE);
		map(InputConstants.KEY_LBRACKET, Key.LEFT_BRACKET);
		map(InputConstants.KEY_MINUS, Key.MINUS);
		map(InputConstants.KEY_PERIOD, Key.PERIOD);
		map(InputConstants.KEY_RBRACKET, Key.RIGHT_BRACKET);
		map(InputConstants.KEY_SEMICOLON, Key.SEMICOLON);
		map(InputConstants.KEY_SLASH, Key.SLASH);
		map(InputConstants.KEY_SPACE, Key.SPACE);
		map(InputConstants.KEY_TAB, Key.TAB);
		map(InputConstants.KEY_LALT, Key.LEFT_ALT);
		map(InputConstants.KEY_LCONTROL, Key.LEFT_CONTROL);
		map(InputConstants.KEY_LSHIFT, Key.LEFT_SHIFT);
		map(InputConstants.KEY_LGUI, Key.LEFT_SUPER);
		map(InputConstants.KEY_RALT, Key.RIGHT_ALT);
		map(InputConstants.KEY_RCONTROL, Key.RIGHT_CONTROL);
		map(InputConstants.KEY_RSHIFT, Key.RIGHT_SHIFT);
		map(InputConstants.KEY_RGUI, Key.RIGHT_SUPER);
		map(InputConstants.KEY_RETURN, Key.ENTER);
		map(InputConstants.KEY_ESCAPE, Key.ESCAPE);
		map(InputConstants.KEY_BACKSPACE, Key.BACKSPACE);
		map(InputConstants.KEY_DELETE, Key.DELETE);
		map(InputConstants.KEY_END, Key.END);
		map(InputConstants.KEY_HOME, Key.HOME);
		map(InputConstants.KEY_INSERT, Key.INSERT);
		map(InputConstants.KEY_PAGEDOWN, Key.PAGE_DOWN);
		map(InputConstants.KEY_PAGEUP, Key.PAGE_UP);
		//?}
	}

	private McInputUtil() {
	}

	private static void map(int nativeKey, int coreKey) {
		TO_CORE.put(nativeKey, coreKey);
		TO_NATIVE.put(coreKey, nativeKey);
	}

	public static int toCore(int nativeKey, int keycode) {
		//? if >=26.3 {
		if (keycode >= InputConstants.KEYCODE_A && keycode <= InputConstants.KEYCODE_Z) {
			return Key.A + (keycode - InputConstants.KEYCODE_A);
		}
		if (nativeKey < 0) {
			return Key.UNKNOWN;
		}
		Integer mapped = TO_CORE.get(nativeKey);
		return mapped == null ? UNMAPPED_OFFSET + nativeKey : mapped;
		//?} else
		//return nativeKey;
	}

	public static int toNative(int coreKey) {
		//? if >=26.3 {
		if (coreKey < 0) {
			return InputConstants.UNKNOWN.getValue();
		}
		if (coreKey >= UNMAPPED_OFFSET) {
			return coreKey - UNMAPPED_OFFSET;
		}
		Integer mapped = TO_NATIVE.get(coreKey);
		return mapped == null ? InputConstants.UNKNOWN.getValue() : mapped;
		//?} else
		//return coreKey;
	}

	public static Modifiers modifiers(int flags) {
		//? if >=26.3 {
		return new Modifiers((flags & InputConstants.MOD_SHIFT) != 0, (flags & InputConstants.MOD_CONTROL) != 0, (flags & InputConstants.MOD_ALT) != 0, (flags & InputConstants.MOD_SUPER) != 0);
		//?} else
		//return new Modifiers((flags & GLFW.GLFW_MOD_SHIFT) != 0, (flags & GLFW.GLFW_MOD_CONTROL) != 0, (flags & GLFW.GLFW_MOD_ALT) != 0, (flags & GLFW.GLFW_MOD_SUPER) != 0);
	}

	public static Modifiers currentModifiers() {
		return new Modifiers(isDown(Key.LEFT_SHIFT) || isDown(Key.RIGHT_SHIFT), isDown(Key.LEFT_CONTROL) || isDown(Key.RIGHT_CONTROL), isDown(Key.LEFT_ALT) || isDown(Key.RIGHT_ALT), isDown(Key.LEFT_SUPER) || isDown(Key.RIGHT_SUPER));
	}

	public static boolean isDown(int coreKey) {
		int nativeKey = toNative(coreKey);
		if (nativeKey < 0) {
			return false;
		}
		//? if >=26.3 {
		return InputConstants.isKeyDown(nativeKey);
		//?} else if >=1.21.9 {
		/*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), nativeKey);
		*///?} else
		//return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), nativeKey);
	}

	public static MouseButton button(int nativeButton) {
		if (nativeButton == InputConstants.MOUSE_BUTTON_LEFT) {
			return MouseButton.LEFT;
		}
		if (nativeButton == InputConstants.MOUSE_BUTTON_RIGHT) {
			return MouseButton.RIGHT;
		}
		if (nativeButton == InputConstants.MOUSE_BUTTON_MIDDLE) {
			return MouseButton.MIDDLE;
		}
		return MouseButton.OTHER;
	}
}

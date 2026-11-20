package com.nick22985.nbteditor.platform;

import com.nick22985.nbteditor.text.Text;
import java.util.function.BooleanSupplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface ChatService {
	void message(Text text);

	void actionBar(Text text);

	void longInput(BooleanSupplier enabled);
}

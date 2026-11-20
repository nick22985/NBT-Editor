package com.nick22985.nbteditor.modules.files;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
@FunctionalInterface
public interface ExportWriter {
	void write(Path path) throws IOException;
}

package com.nick22985.nbteditor.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CreditsUtil {
	private static final String TRANSLATORS = "nbtedit/translators.txt";

	private CreditsUtil() {
	}

	public static List<String> translators() {
		List<String> lines = new ArrayList<>();
		try (InputStream in = CreditsUtil.class.getClassLoader().getResourceAsStream(TRANSLATORS)) {
			if (in == null) {
				return lines;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					lines.add(line);
				}
			}
		} catch (IOException e) {
			return lines;
		}
		return lines;
	}
}

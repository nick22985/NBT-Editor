package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ImageLoreUtil {
	public static final String PIXEL = "█";
	public static final int MAX_SIZE = 128;
	public static final int DEFAULT_WIDTH = 32;
	private static final Set<String> EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

	private ImageLoreUtil() {
	}

	public static boolean isImage(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		int dot = name.lastIndexOf('.');
		return dot >= 0 && EXTENSIONS.contains(name.substring(dot + 1));
	}

	public static BufferedImage read(Path path) throws IOException {
		BufferedImage image;
		try (var in = Files.newInputStream(path)) {
			image = ImageIO.read(in);
		}
		if (image == null) {
			throw new IOException("Not a readable image");
		}
		return image;
	}

	public static int defaultHeight(int imageWidth, int imageHeight, int width) {
		return DrawUtil.clamp(Math.round(width * (float) imageHeight / Math.max(1, imageWidth)), 1, MAX_SIZE);
	}

	public static int[] size(int imageWidth, int imageHeight, int width, int height) {
		if (width > 0 && height > 0) {
			return new int[] {DrawUtil.clamp(width, 1, MAX_SIZE), DrawUtil.clamp(height, 1, MAX_SIZE)};
		}
		if (height > 0) {
			int clamped = DrawUtil.clamp(height, 1, MAX_SIZE);
			return new int[] {defaultHeight(imageHeight, imageWidth, clamped), clamped};
		}
		int clamped = DrawUtil.clamp(width > 0 ? width : DEFAULT_WIDTH, 1, MAX_SIZE);
		return new int[] {clamped, defaultHeight(imageWidth, imageHeight, clamped)};
	}

	public static int[] scale(BufferedImage image, int width, int height) {
		int sourceWidth = image.getWidth();
		int sourceHeight = image.getHeight();
		int[] source = image.getRGB(0, 0, sourceWidth, sourceHeight, null, 0, sourceWidth);
		int[] out = new int[width * height];
		for (int y = 0; y < height; y++) {
			int fromY = y * sourceHeight / height;
			int toY = Math.max(fromY + 1, (y + 1) * sourceHeight / height);
			for (int x = 0; x < width; x++) {
				int fromX = x * sourceWidth / width;
				int toX = Math.max(fromX + 1, (x + 1) * sourceWidth / width);
				long a = 0;
				long r = 0;
				long g = 0;
				long b = 0;
				for (int sy = fromY; sy < toY; sy++) {
					for (int sx = fromX; sx < toX; sx++) {
						int argb = source[sy * sourceWidth + sx];
						int alpha = argb >>> 24;
						a += alpha;
						r += ((argb >> 16) & 0xFF) * alpha;
						g += ((argb >> 8) & 0xFF) * alpha;
						b += (argb & 0xFF) * alpha;
					}
				}
				int count = (toX - fromX) * (toY - fromY);
				out[y * width + x] = a == 0 ? 0 : (int) (a / count) << 24 | (int) (r / a) << 16 | (int) (g / a) << 8 | (int) (b / a);
			}
		}
		return out;
	}

	public static List<Text> lore(BufferedImage image, int width, int height) {
		int[] pixels = scale(image, width, height);
		List<Text> lines = new ArrayList<>();
		for (int y = 0; y < height; y++) {
			List<Text> runs = new ArrayList<>();
			int x = 0;
			while (x < width) {
				int rgb = pixels[y * width + x] & 0xFFFFFF;
				int end = x + 1;
				while (end < width && (pixels[y * width + end] & 0xFFFFFF) == rgb) {
					end++;
				}
				runs.add(Text.literal(PIXEL.repeat(end - x), Style.EMPTY.withColor(rgb)));
				x = end;
			}
			lines.add(Text.literal("", Style.EMPTY.withItalic(false)).withSiblings(runs));
		}
		return lines;
	}

	public static String baseName(Path path) {
		String name = path.getFileName().toString();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}
}

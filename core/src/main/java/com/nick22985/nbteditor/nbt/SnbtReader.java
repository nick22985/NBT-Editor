package com.nick22985.nbteditor.nbt;

import com.nick22985.nbteditor.util.SnbtUtil;
import com.nick22985.nbteditor.util.UuidUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SnbtReader {
	private static final Pattern LEGACY_DOUBLE_NO_SUFFIX = Pattern.compile("[-+]?(?:[0-9]+[.]|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEGACY_DOUBLE = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?d", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEGACY_FLOAT = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?f", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEGACY_BYTE = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)b", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEGACY_LONG = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)l", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEGACY_SHORT = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)s", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEGACY_INT = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)");
	private static final Pattern SPECIAL = Pattern.compile("([-+]?)(NaN|Infinity)([fFdD]?)");
	private static final Pattern EXPONENT_ONLY = Pattern.compile("[-+]?[0-9]+e[-+]?[0-9]+", Pattern.CASE_INSENSITIVE);

	private final String input;
	private final SnbtDialect dialect;
	private final boolean specialNumbers;
	private int cursor;

	public SnbtReader(String input, SnbtDialect dialect) {
		this(input, dialect, true);
	}

	public SnbtReader(String input, SnbtDialect dialect, boolean specialNumbers) {
		this.input = input;
		this.dialect = dialect;
		this.specialNumbers = specialNumbers;
	}

	public Tag read() throws SnbtException {
		skipWhitespace();
		Tag tag = readValue();
		skipWhitespace();
		if (canRead()) {
			throw error("Unexpected trailing data");
		}
		return tag;
	}

	public Tag readPrefix() throws SnbtException {
		skipWhitespace();
		return readValue();
	}

	public int cursor() {
		return cursor;
	}

	private boolean canRead() {
		return cursor < input.length();
	}

	private char peek() {
		return input.charAt(cursor);
	}

	private char peek(int offset) {
		return input.charAt(cursor + offset);
	}

	private void skipWhitespace() {
		while (canRead() && Character.isWhitespace(peek())) {
			cursor++;
		}
	}

	private SnbtException error(String message) {
		return new SnbtException(message, input, cursor);
	}

	private void expect(char c) throws SnbtException {
		skipWhitespace();
		if (!canRead() || peek() != c) {
			throw error("Expected '" + c + "'");
		}
		cursor++;
	}

	private Tag readValue() throws SnbtException {
		skipWhitespace();
		if (!canRead()) {
			throw error("Expected value");
		}
		char c = peek();
		if (c == '{') {
			return readCompound();
		}
		if (c == '[') {
			return readListOrArray();
		}
		if (c == '"' || c == '\'') {
			return StringTag.of(readQuoted());
		}
		return readScalar();
	}

	private CompoundTag readCompound() throws SnbtException {
		expect('{');
		CompoundTag compound = new CompoundTag();
		skipWhitespace();
		while (canRead() && peek() != '}') {
			int start = cursor;
			String key = readKey();
			if (key.isEmpty() && dialect == SnbtDialect.LEGACY) {
				cursor = start;
				throw error("Expected key");
			}
			expect(':');
			compound.put(key, readValue());
			if (!separator()) {
				break;
			}
			skipWhitespace();
			if (!canRead()) {
				throw error("Expected key");
			}
		}
		expect('}');
		return compound;
	}

	private String readKey() throws SnbtException {
		skipWhitespace();
		if (!canRead()) {
			throw error("Expected key");
		}
		char c = peek();
		if (c == '"' || c == '\'') {
			return readQuoted();
		}
		int start = cursor;
		while (canRead() && SnbtUtil.isUnquotedChar(peek())) {
			cursor++;
		}
		if (cursor == start) {
			throw error("Expected key");
		}
		return input.substring(start, cursor);
	}

	private boolean separator() {
		skipWhitespace();
		if (canRead() && peek() == ',') {
			cursor++;
			skipWhitespace();
			return true;
		}
		return false;
	}

	private Tag readListOrArray() throws SnbtException {
		if (cursor + 2 < input.length() && peek(2) == ';' && "BIL".indexOf(peek(1)) >= 0) {
			return readArray();
		}
		return readList();
	}

	private ListTag readList() throws SnbtException {
		expect('[');
		skipWhitespace();
		if (!canRead()) {
			throw error("Expected value");
		}
		ListTag list = new ListTag();
		TagType type = null;
		while (peek() != ']') {
			int start = cursor;
			Tag value = readValue();
			if (type == null) {
				type = value.type();
			} else if (value.type() != type && !dialect.allowsMixedLists()) {
				cursor = start;
				throw error("Can't insert " + value.type().nbtName() + " into list of " + type.nbtName());
			}
			list.add(value);
			if (!separator()) {
				break;
			}
			if (!canRead()) {
				throw error("Expected value");
			}
		}
		expect(']');
		return list;
	}

	private Tag readArray() throws SnbtException {
		expect('[');
		char kind = peek();
		cursor += 2;
		skipWhitespace();
		if (!canRead()) {
			throw error("Expected value");
		}
		TagType elementType = kind == 'B' ? TagType.BYTE : kind == 'I' ? TagType.INT : TagType.LONG;
		List<Long> values = new ArrayList<>();
		while (peek() != ']') {
			int start = cursor;
			Tag value = readValue();
			long number;
			if (value.type() == elementType) {
				number = ((NumericTag) value).longValue();
			} else if (dialect == SnbtDialect.MODERN && value instanceof NumericTag && value.type().isInteger() && fits(((NumericTag) value).longValue(), elementType)) {
				number = ((NumericTag) value).longValue();
			} else {
				cursor = start;
				throw error("Can't insert " + value.type().nbtName() + " into " + arrayName(elementType));
			}
			values.add(number);
			if (!separator()) {
				break;
			}
			if (!canRead()) {
				throw error("Expected value");
			}
		}
		expect(']');
		switch (elementType) {
			case BYTE: {
				byte[] out = new byte[values.size()];
				for (int i = 0; i < out.length; i++) {
					out[i] = values.get(i).byteValue();
				}
				return new ByteArrayTag(out);
			}
			case INT: {
				int[] out = new int[values.size()];
				for (int i = 0; i < out.length; i++) {
					out[i] = values.get(i).intValue();
				}
				return new IntArrayTag(out);
			}
			default: {
				long[] out = new long[values.size()];
				for (int i = 0; i < out.length; i++) {
					out[i] = values.get(i);
				}
				return new LongArrayTag(out);
			}
		}
	}

	private String arrayName(TagType elementType) {
		return elementType == TagType.BYTE ? TagType.BYTE_ARRAY.nbtName() : elementType == TagType.INT ? TagType.INT_ARRAY.nbtName() : TagType.LONG_ARRAY.nbtName();
	}

	private boolean fits(long value, TagType type) {
		switch (type) {
			case BYTE:
				return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
			case SHORT:
				return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
			case INT:
				return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
			default:
				return true;
		}
	}

	private String readQuoted() throws SnbtException {
		char quote = peek();
		cursor++;
		StringBuilder out = new StringBuilder();
		while (canRead()) {
			char c = peek();
			cursor++;
			if (c == quote) {
				return out.toString();
			}
			if (c != '\\') {
				out.append(c);
				continue;
			}
			if (!canRead()) {
				break;
			}
			char escaped = peek();
			cursor++;
			if (escaped == quote || escaped == '\\') {
				out.append(escaped);
				continue;
			}
			if (dialect == SnbtDialect.LEGACY) {
				cursor -= 2;
				throw error("Invalid escape sequence '\\" + escaped + "' in quoted string");
			}
			switch (escaped) {
				case '"':
				case '\'':
					out.append(escaped);
					break;
				case 'b':
					out.append('\b');
					break;
				case 's':
					out.append(' ');
					break;
				case 't':
					out.append('\t');
					break;
				case 'n':
					out.append('\n');
					break;
				case 'f':
					out.append('\f');
					break;
				case 'r':
					out.append('\r');
					break;
				case 'x':
					out.appendCodePoint(readHex(2));
					break;
				case 'u':
					out.appendCodePoint(readHex(4));
					break;
				case 'U':
					out.appendCodePoint(readHex(8));
					break;
				case 'N':
					out.appendCodePoint(readNamedChar());
					break;
				default:
					cursor -= 2;
					throw error("Invalid escape sequence '\\" + escaped + "' in quoted string");
			}
		}
		throw error("Unclosed quoted string");
	}

	private int readHex(int digits) throws SnbtException {
		if (cursor + digits > input.length()) {
			throw error("Expected " + digits + " hex digits");
		}
		String hex = input.substring(cursor, cursor + digits);
		try {
			int value = (int) Long.parseLong(hex, 16);
			cursor += digits;
			if (!Character.isValidCodePoint(value)) {
				throw error("Invalid code point");
			}
			return value;
		} catch (NumberFormatException e) {
			throw error("Expected " + digits + " hex digits");
		}
	}

	private int readNamedChar() throws SnbtException {
		if (!canRead() || peek() != '{') {
			throw error("Expected '{'");
		}
		int close = input.indexOf('}', cursor);
		if (close < 0) {
			throw error("Expected '}'");
		}
		String name = input.substring(cursor + 1, close);
		try {
			int codePoint = Character.codePointOf(name);
			cursor = close + 1;
			return codePoint;
		} catch (IllegalArgumentException e) {
			throw error("Unknown character name '" + name + "'");
		}
	}

	private Tag readScalar() throws SnbtException {
		int start = cursor;
		while (canRead() && SnbtUtil.isUnquotedChar(peek())) {
			cursor++;
		}
		if (cursor == start) {
			throw error("Expected value");
		}
		String token = input.substring(start, cursor);
		if (dialect == SnbtDialect.LEGACY) {
			return legacyScalar(token);
		}
		int afterToken = cursor;
		skipWhitespace();
		if (canRead() && peek() == '(') {
			return readOperation(token, start);
		}
		cursor = afterToken;
		return modernScalar(token, start);
	}

	private Tag legacyScalar(String token) {
		try {
			if (LEGACY_FLOAT.matcher(token).matches()) {
				return FloatTag.of(Float.parseFloat(token.substring(0, token.length() - 1)));
			}
			if (LEGACY_BYTE.matcher(token).matches()) {
				return ByteTag.of(Byte.parseByte(token.substring(0, token.length() - 1)));
			}
			if (LEGACY_LONG.matcher(token).matches()) {
				return LongTag.of(Long.parseLong(token.substring(0, token.length() - 1)));
			}
			if (LEGACY_SHORT.matcher(token).matches()) {
				return ShortTag.of(Short.parseShort(token.substring(0, token.length() - 1)));
			}
			if (LEGACY_INT.matcher(token).matches()) {
				return IntTag.of(Integer.parseInt(token));
			}
			if (LEGACY_DOUBLE.matcher(token).matches()) {
				return DoubleTag.of(Double.parseDouble(token.substring(0, token.length() - 1)));
			}
			if (LEGACY_DOUBLE_NO_SUFFIX.matcher(token).matches()) {
				return DoubleTag.of(Double.parseDouble(token));
			}
		} catch (NumberFormatException ignored) {
		}
		if (token.equalsIgnoreCase("true")) {
			return ByteTag.ONE;
		}
		if (token.equalsIgnoreCase("false")) {
			return ByteTag.ZERO;
		}
		if (specialNumbers) {
			Tag special = special(token);
			if (special != null) {
				return special;
			}
		}
		return StringTag.of(token);
	}

	private Tag special(String token) {
		java.util.regex.Matcher matcher = SPECIAL.matcher(token);
		if (matcher.matches()) {
			double value = matcher.group(2).equals("NaN") ? Double.NaN : Double.POSITIVE_INFINITY;
			if (matcher.group(1).equals("-")) {
				value = -value;
			}
			return matcher.group(3).equalsIgnoreCase("f") ? FloatTag.of((float) value) : DoubleTag.of(value);
		}
		if (EXPONENT_ONLY.matcher(token).matches()) {
			return DoubleTag.of(Double.parseDouble(token));
		}
		return null;
	}

	private Tag modernScalar(String token, int start) throws SnbtException {
		if (token.equals("true")) {
			return ByteTag.ONE;
		}
		if (token.equals("false")) {
			return ByteTag.ZERO;
		}
		char first = token.charAt(0);
		boolean numberLike = first >= '0' && first <= '9' || first == '+' || first == '-' || first == '.';
		if (!numberLike) {
			if (specialNumbers) {
				Tag special = special(token);
				if (special != null) {
					return special;
				}
			}
			return StringTag.of(token);
		}
		Tag number = modernNumber(token);
		if (number == null && specialNumbers) {
			number = special(token);
		}
		if (number == null) {
			cursor = start;
			throw error("Invalid number '" + token + "'");
		}
		return number;
	}

	private Tag modernNumber(String token) {
		String body = token;
		boolean negative = false;
		if (body.startsWith("+") || body.startsWith("-")) {
			negative = body.charAt(0) == '-';
			body = body.substring(1);
		}
		if (body.isEmpty()) {
			return null;
		}
		String lower = body.toLowerCase(java.util.Locale.ROOT);
		int radix = 10;
		if (lower.startsWith("0x")) {
			radix = 16;
			lower = lower.substring(2);
		} else if (lower.startsWith("0b") && lower.length() > 2 && (lower.charAt(2) == '0' || lower.charAt(2) == '1')) {
			radix = 2;
			lower = lower.substring(2);
		}
		if (lower.isEmpty()) {
			return null;
		}
		if (radix == 10) {
			Tag floating = modernFloat(lower, negative);
			if (floating != null) {
				return floating;
			}
		}
		int digitsEnd = 0;
		while (digitsEnd < lower.length() && (isDigitFor(lower.charAt(digitsEnd), radix) || lower.charAt(digitsEnd) == '_')) {
			digitsEnd++;
		}
		String digits = lower.substring(0, digitsEnd);
		String suffix = lower.substring(digitsEnd);
		Character signedness = null;
		TagType type = TagType.INT;
		if (suffix.length() == 2 && (suffix.charAt(0) == 's' || suffix.charAt(0) == 'u')) {
			signedness = suffix.charAt(0);
			suffix = suffix.substring(1);
		}
		if (suffix.length() == 1) {
			switch (suffix.charAt(0)) {
				case 'b':
					type = TagType.BYTE;
					break;
				case 's':
					type = TagType.SHORT;
					break;
				case 'i':
					type = TagType.INT;
					break;
				case 'l':
					type = TagType.LONG;
					break;
				default:
					return null;
			}
		} else if (!suffix.isEmpty() || signedness != null) {
			return null;
		}
		if (digits.isEmpty() || digits.startsWith("_") || digits.endsWith("_")) {
			return null;
		}
		java.math.BigInteger value = new java.math.BigInteger(digits.replace("_", ""), radix);
		if (negative) {
			value = value.negate();
		}
		int bits = type == TagType.BYTE ? 8 : type == TagType.SHORT ? 16 : type == TagType.INT ? 32 : 64;
		java.math.BigInteger signedMin = java.math.BigInteger.ONE.shiftLeft(bits - 1).negate();
		java.math.BigInteger signedMax = java.math.BigInteger.ONE.shiftLeft(bits - 1).subtract(java.math.BigInteger.ONE);
		java.math.BigInteger unsignedMax = java.math.BigInteger.ONE.shiftLeft(bits).subtract(java.math.BigInteger.ONE);
		java.math.BigInteger min;
		java.math.BigInteger max;
		if (signedness == null) {
			min = signedMin;
			max = radix == 10 ? signedMax : unsignedMax;
		} else if (signedness == 'u') {
			min = java.math.BigInteger.ZERO;
			max = unsignedMax;
		} else {
			min = signedMin;
			max = signedMax;
		}
		if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
			return null;
		}
		return NumericTag.of(type, value.longValue());
	}

	private Tag modernFloat(String lower, boolean negative) {
		char last = lower.charAt(lower.length() - 1);
		TagType type = TagType.DOUBLE;
		String body = lower;
		boolean suffixed = false;
		if (last == 'f' || last == 'd') {
			type = last == 'f' ? TagType.FLOAT : TagType.DOUBLE;
			body = lower.substring(0, lower.length() - 1);
			suffixed = true;
		}
		if (body.isEmpty()) {
			return null;
		}
		boolean hasDot = body.indexOf('.') >= 0;
		boolean hasExp = body.indexOf('e') >= 0;
		if (!suffixed && !hasDot && !hasExp) {
			return null;
		}
		if (!body.matches("(?:[0-9](?:[0-9_]*[0-9])?)?(?:\\.(?:[0-9](?:[0-9_]*[0-9])?)?)?(?:e[-+]?[0-9](?:[0-9_]*[0-9])?)?")) {
			return null;
		}
		String clean = body.replace("_", "");
		if (clean.isEmpty() || clean.equals(".") || clean.startsWith("e") || clean.startsWith(".e")) {
			return null;
		}
		double value = Double.parseDouble(clean);
		if (negative) {
			value = -value;
		}
		if (type == TagType.FLOAT) {
			return FloatTag.of((float) value);
		}
		return DoubleTag.of(value);
	}

	private boolean isDigitFor(char c, int radix) {
		return Character.digit(c, radix) >= 0;
	}

	private Tag readOperation(String name, int start) throws SnbtException {
		expect('(');
		List<Tag> args = new ArrayList<>();
		skipWhitespace();
		while (canRead() && peek() != ')') {
			args.add(readValue());
			if (!separator()) {
				break;
			}
		}
		expect(')');
		switch (name) {
			case "bool":
				if (args.size() == 1) {
					Tag arg = args.get(0);
					if (arg instanceof NumericTag) {
						return ByteTag.of(((NumericTag) arg).doubleValue() != 0);
					}
				}
				break;
			case "uuid":
				if (args.size() == 1 && args.get(0) instanceof StringTag) {
					try {
						return UuidUtil.toIntArray(UUID.fromString(((StringTag) args.get(0)).value()));
					} catch (IllegalArgumentException ignored) {
					}
				}
				break;
			default:
				cursor = start;
				throw error("Unknown operation '" + name + "'");
		}
		cursor = start;
		throw error("Invalid arguments for '" + name + "'");
	}
}

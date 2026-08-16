package me.kall.narutotv.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import me.kall.narutotv.data.file.GamePaths;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonConfig {
    private final Path configPath;
    private final Map<String, Object> configMap = Object2ObjectMaps.synchronize(new Object2ObjectLinkedOpenHashMap<>());

    private JsonConfig(@NotNull Path configPath, String version) {
        this.configPath = configPath;
        this.put("version", version);
    }

    @Contract("_, _ -> new")
    public static @NotNull JsonConfig create(Path configPath, String version) {
        return new JsonConfig(configPath, version);
    }

    @Contract("_, _ -> new")
    public static @NotNull JsonConfig create(String modID, String version) {
        return create(GamePaths.CONFIG.resolve(modID + ".json"), version);
    }

    @SuppressWarnings("unchecked")
    static Object toJsonValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map || obj instanceof List
                || obj instanceof String || obj instanceof Boolean || obj instanceof Number) {
            if (obj instanceof Map) {
                Map<String, Object> result = new LinkedHashMap<>();
                ((Map<Object, Object>) obj).forEach((k, v) -> result.put(String.valueOf(k), toJsonValue(v)));
                return result;
            }
            if (obj instanceof List) {
                List<Object> result = new ArrayList<>();
                for (Object item : (List<?>) obj) result.add(toJsonValue(item));
                return result;
            }
            return obj;
        }
        if (obj.getClass().isEnum()) {
            return ((Enum<?>) obj).name();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            field.setAccessible(true);
            try {
                result.put(field.getName(), toJsonValue(field.get(obj)));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> T fromJsonValue(Object value, Class<T> type) {
        if (value == null) return null;
        if (type == String.class) return (T) String.valueOf(value);
        if (type == Boolean.class || type == boolean.class) return (T) value;
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            Number n = (Number) value;
            if (type == int.class || type == Integer.class) return (T) Integer.valueOf(n.intValue());
            if (type == long.class || type == Long.class) return (T) Long.valueOf(n.longValue());
            if (type == double.class || type == Double.class) return (T) Double.valueOf(n.doubleValue());
            if (type == float.class || type == Float.class) return (T) Float.valueOf(n.floatValue());
            if (type == short.class || type == Short.class) return (T) Short.valueOf(n.shortValue());
            if (type == byte.class || type == Byte.class) return (T) Byte.valueOf(n.byteValue());
            return (T) n;
        }
        if (type.isEnum()) return (T) Enum.valueOf((Class<Enum>) type, String.valueOf(value));
        switch (value) {
            case Map ignored when Map.class.isAssignableFrom(type) -> {
                return (T) value;
            }
            case List ignored when List.class.isAssignableFrom(type) -> {
                return (T) value;
            }
            case Map ignored -> {
                try {
                    T instance = type.getDeclaredConstructor().newInstance();
                    Map<String, Object> map = (Map<String, Object>) value;
                    for (Field field : type.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))
                            continue;
                        if (!map.containsKey(field.getName())) continue;
                        field.setAccessible(true);
                        field.set(instance, fromJsonValue(map.get(field.getName()), field.getType()));
                    }
                    return instance;
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to map JSON object to " + type.getName() + " (needs a no-arg constructor)", e);
                }
            }
            default -> {}
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to " + type.getName());
    }

    static void write(Object value, Writer writer) throws IOException {
        writeValue(value, writer, 0);
        writer.write('\n');
    }

    private static void writeValue(Object value, Writer writer, int indent) throws IOException {
        if (value == null) {
            writer.write("null");
        } else if (value instanceof Map) {
            writeObject((Map<?, ?>) value, writer, indent);
        } else if (value instanceof List) {
            writeArray((List<?>) value, writer, indent);
        } else if (value instanceof String) {
            writeString((String) value, writer);
        } else if (value instanceof Boolean || value instanceof Number) {
            writer.write(value.toString());
        } else {
            writeString(value.toString(), writer);
        }
    }

    private static void writeObject(@NotNull Map<?, ?> map, Writer writer, int indent) throws IOException {
        if (map.isEmpty()) { writer.write("{}"); return; }
        writer.write("{\n");
        int i = 0;
        int size = map.size();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writeIndent(writer, indent + 1);
            writeString(String.valueOf(entry.getKey()), writer);
            writer.write(": ");
            writeValue(entry.getValue(), writer, indent + 1);
            if (++i < size) writer.write(',');
            writer.write('\n');
        }
        writeIndent(writer, indent);
        writer.write('}');
    }

    private static void writeArray(@NotNull List<?> list, Writer writer, int indent) throws IOException {
        if (list.isEmpty()) { writer.write("[]"); return; }
        writer.write("[\n");
        for (int i = 0; i < list.size(); i++) {
            writeIndent(writer, indent + 1);
            writeValue(list.get(i), writer, indent + 1);
            if (i < list.size() - 1) writer.write(',');
            writer.write('\n');
        }
        writeIndent(writer, indent);
        writer.write(']');
    }

    private static void writeString(@NotNull String s, @NotNull Writer writer) throws IOException {
        writer.write('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': writer.write("\\\""); break;
                case '\\': writer.write("\\\\"); break;
                case '\n': writer.write("\\n"); break;
                case '\r': writer.write("\\r"); break;
                case '\t': writer.write("\\t"); break;
                default:
                    if (c < 0x20) {
                        writer.write(String.format("\\u%04x", (int) c));
                    } else {
                        writer.write(c);
                    }
            }
        }
        writer.write('"');
    }

    private static void writeIndent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i++) writer.write("  ");
    }

    public JsonConfig initialize() {
        if (Files.exists(this.configPath)) {
            this.read();
        } else {
            this.create();
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private void read() {
        try {
            String content = Files.readString(this.configPath, StandardCharsets.UTF_8);
            Object parsed = new JsonParser(content).parseValue();
            if (!(parsed instanceof Map)) {
                throw new RuntimeException("Config root is not a JSON object: " + configPath);
            }
            Map<String, Object> fileConfig = (Map<String, Object>) parsed;

            Map<String, Object> defaultConfig = new LinkedHashMap<>(this.configMap);

            this.configMap.clear();
            this.configMap.putAll(fileConfig);

            for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
                this.configMap.putIfAbsent(entry.getKey(), entry.getValue());
            }

            Object fileVersion = fileConfig.get("version");
            Object defaultVersion = defaultConfig.get("version");
            if (fileVersion == null || !fileVersion.equals(defaultVersion)) {
                this.configMap.put("version", defaultVersion);
                this.saveToFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + configPath, e);
        }
    }

    private void create() {
        try {
            Files.createDirectories(this.configPath.getParent());
            this.saveToFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file: " + configPath, e);
        }
    }

    public void saveToFile() {
        try (Writer writer = Files.newBufferedWriter(this.configPath, StandardCharsets.UTF_8)) {
            write(this.configMap, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config file: " + this.configPath, e);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public JsonConfig put(String key, Object value) {
        this.configMap.put(key, toJsonValue(value));
        return this;
    }

    private Object get(String key) {
        Object value = this.configMap.get(key);
        if (value == null && !this.configMap.containsKey(key)) throw new NoSuchElementException("Missing config key: " + key);
        return value;
    }

    public int getInt(String key) {
        return ((Number) this.get(key)).intValue();
    }

    public double getDouble(String key) {
        return ((Number) this.get(key)).doubleValue();
    }

    public float getFloat(String key) {
        return ((Number) this.get(key)).floatValue();
    }

    public long getLong(String key) {
        return ((Number) this.get(key)).longValue();
    }

    public boolean getBoolean(String key) {
        return (Boolean) this.get(key);
    }

    public String getString(String key) {
        return String.valueOf(this.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> Stream<T> getStream(String key, @NotNull Class<T> valueType) {
        Object value = this.get(key);
        if (!(value instanceof List)) {
            throw new IllegalStateException("Config key '" + key + "' is not an array");
        }
        return ((List<Object>) value).stream()
                .map(element -> fromJsonValue(element, valueType));
    }

    public <T> List<T> getList(String key, @NotNull Class<T> valueType) {
        return this.getStream(key, valueType).collect(Collectors.toList());
    }

    public <T> Set<T> getSet(String key, @NotNull Class<T> valueType) {
        return this.getStream(key, valueType).collect(Collectors.toSet());
    }

    private static final class JsonParser {
        private final String s;
        private int pos;

        JsonParser(String s) {
            this.s = s;
            this.pos = 0;
        }

        @Nullable Object parseValue() {
            skipWhitespace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> {
                    expect("true");
                    yield Boolean.TRUE;
                }
                case 'f' -> {
                    expect("false");
                    yield Boolean.FALSE;
                }
                case 'n' -> {
                    expect("null");
                    yield null;
                }
                default -> parseNumber();
            };
        }

        private @NotNull Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expectChar('{');
            skipWhitespace();
            if (peek() == '}') { pos++; return map; }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expectChar(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char c = next();
                if (c == '}') break;
                if (c != ',') throw error("Expected ',' or '}'");
            }
            return map;
        }

        private @NotNull List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expectChar('[');
            skipWhitespace();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') break;
                if (c != ',') throw error("Expected ',' or ']'");
                skipWhitespace();
            }
            return list;
        }

        private @NotNull String parseString() {
            expectChar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            String hex = s.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default: throw error("Invalid escape: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < s.length() && Character.isDigit(peek())) pos++;
            boolean isFloating = false;
            if (pos < s.length() && peek() == '.') {
                isFloating = true;
                do pos++;
                while (pos < s.length() && Character.isDigit(peek()));
            }
            if (pos < s.length() && (peek() == 'e' || peek() == 'E')) {
                isFloating = true;
                pos++;
                if (peek() == '+' || peek() == '-') pos++;
                while (pos < s.length() && Character.isDigit(peek())) pos++;
            }
            String num = s.substring(start, pos);
            if (num.isEmpty() || num.equals("-")) throw error("Invalid number");
            return isFloating ? (Object) Double.parseDouble(num) : (Object) Long.parseLong(num);
        }

        private void expect(@NotNull String literal) {
            if (pos + literal.length() > s.length() || !s.startsWith(literal, pos)) {
                throw error("Expected '" + literal + "'");
            }
            pos += literal.length();
        }

        private void expectChar(char expected) {
            char c = next();
            if (c != expected) throw error("Expected '" + expected + "' but got '" + c + "'");
        }

        private char next() {
            if (pos >= s.length()) throw error("Unexpected end of input");
            return s.charAt(pos++);
        }

        private char peek() {
            if (pos >= s.length()) throw error("Unexpected end of input");
            return s.charAt(pos);
        }

        private void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        @Contract("_ -> new")
        private @NotNull RuntimeException error(String message) {
            return new RuntimeException("JSON parse error at position " + pos + ": " + message);
        }
    }
}
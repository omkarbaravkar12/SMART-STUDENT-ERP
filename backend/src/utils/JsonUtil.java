package utils;

import java.util.*;

/**
 * JsonUtil - Lightweight JSON serializer/deserializer
 * (No external library required - uses manual parsing)
 * For production, replace with Jackson or Gson
 */
public class JsonUtil {

    /**
     * Serialize Java object to JSON string
     */
    @SuppressWarnings("unchecked")
    public static String toJson(Object obj) {
        if (obj == null)                   return "null";
        if (obj instanceof String s)       return "\"" + escapeJson(s) + "\"";
        if (obj instanceof Boolean b)      return b.toString();
        if (obj instanceof Number n)       return n.toString();
        if (obj instanceof Map<?,?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?,?> e : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(e.getKey().toString())).append("\":");
                sb.append(toJson(e.getValue()));
                first = false;
            }
            return sb.append("}").toString();
        }
        if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            return sb.append("]").toString();
        }
        if (obj instanceof java.sql.Date d)      return "\"" + d + "\"";
        if (obj instanceof java.sql.Time t)      return "\"" + t + "\"";
        if (obj instanceof java.sql.Timestamp ts) return "\"" + ts.toInstant() + "\"";
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    /**
     * Deserialize JSON string to Map<String, Object>
     */
    public static Map<String, Object> fromJson(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return map;
        json = json.trim();
        if (!json.startsWith("{")) return map;
        json = json.substring(1, json.lastIndexOf("}")).trim();
        parseJsonObject(json, map);
        return map;
    }

    private static void parseJsonObject(String json, Map<String, Object> map) {
        int i = 0;
        while (i < json.length()) {
            // Skip whitespace and commas
            while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == ',' || json.charAt(i) == '\n' || json.charAt(i) == '\r')) i++;
            if (i >= json.length()) break;

            // Parse key
            if (json.charAt(i) != '"') break;
            int keyStart = i + 1;
            int keyEnd   = json.indexOf('"', keyStart);
            if (keyEnd < 0) break;
            String key   = json.substring(keyStart, keyEnd);
            i = keyEnd + 1;

            // Skip colon
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++; // skip ':'
            while (i < json.length() && json.charAt(i) == ' ') i++;

            // Parse value
            Object value;
            char c = json.charAt(i);
            if (c == '"') {
                // String value
                int valStart = i + 1;
                int valEnd   = findStringEnd(json, valStart);
                value = json.substring(valStart, valEnd);
                i = valEnd + 1;
            } else if (c == 't') {
                value = true;  i += 4;
            } else if (c == 'f') {
                value = false; i += 5;
            } else if (c == 'n') {
                value = null;  i += 4;
            } else if (c == '{') {
                // Nested object - find matching brace
                int depth = 0, j = i;
                do { if (json.charAt(j) == '{') depth++; else if (json.charAt(j) == '}') depth--; j++; } while (depth > 0 && j < json.length());
                value = json.substring(i, j);
                i = j;
            } else if (c == '[') {
                // Array - find matching bracket
                int depth = 0, j = i;
                do { if (json.charAt(j) == '[') depth++; else if (json.charAt(j) == ']') depth--; j++; } while (depth > 0 && j < json.length());
                value = json.substring(i, j);
                i = j;
            } else {
                // Number
                int numEnd = i;
                while (numEnd < json.length() && ",} \n".indexOf(json.charAt(numEnd)) < 0) numEnd++;
                String numStr = json.substring(i, numEnd).trim();
                try { value = numStr.contains(".") ? Double.parseDouble(numStr) : Long.parseLong(numStr); }
                catch (NumberFormatException e) { value = numStr; }
                i = numEnd;
            }
            map.put(key, value);
        }
    }

    private static int findStringEnd(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '"' && (i == 0 || s.charAt(i - 1) != '\\')) return i;
        }
        return s.length();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

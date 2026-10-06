package com.vault.util;

import com.vault.model.Resource;
import com.vault.model.Subject;
import com.vault.model.User;

import java.util.*;

public class JsonUtil {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String userToJson(User user) {
        if (user == null) return "null";
        return "{" +
                "\"userId\":" + user.getUserId() + "," +
                "\"name\":\"" + escape(user.getName()) + "\"," +
                "\"email\":\"" + escape(user.getEmail()) + "\"," +
                "\"role\":\"" + escape(user.getRole()) + "\"," +
                "\"isAdmin\":" + user.isAdmin() +
                "}";
    }

    public static String subjectToJson(Subject subject) {
        if (subject == null) return "null";
        return "{" +
                "\"subjectCode\":\"" + escape(subject.getSubjectCode()) + "\"," +
                "\"subjectName\":\"" + escape(subject.getSubjectName()) + "\"," +
                "\"semester\":" + subject.getSemester() + "," +
                "\"semesterType\":\"" + escape(subject.getSemesterType()) + "\"," +
                "\"department\":\"" + escape(subject.getDepartment()) + "\"," +
                "\"credits\":" + subject.getCredits() + "," +
                "\"resourceCount\":" + subject.getResourceCount() +
                "}";
    }

    public static String subjectsToJson(List<Subject> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(subjectToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String resourceToJson(Resource r) {
        if (r == null) return "null";
        return "{" +
                "\"resourceId\":" + r.getResourceId() + "," +
                "\"subjectCode\":\"" + escape(r.getSubjectCode()) + "\"," +
                "\"subjectName\":\"" + escape(r.getSubjectName()) + "\"," +
                "\"semester\":" + r.getSemester() + "," +
                "\"semesterType\":\"" + escape(r.getSemesterType()) + "\"," +
                "\"department\":\"" + escape(r.getDepartment()) + "\"," +
                "\"resourceType\":\"" + escape(r.getResourceType()) + "\"," +
                "\"title\":\"" + escape(r.getTitle()) + "\"," +
                "\"description\":\"" + escape(r.getDescription()) + "\"," +
                "\"fileName\":\"" + escape(r.getFileName()) + "\"," +
                "\"filePath\":\"" + escape(r.getFilePath()) + "\"," +
                "\"fileSize\":\"" + escape(r.getFileSize()) + "\"," +
                "\"fileExtension\":\"" + escape(r.getFileExtension()) + "\"," +
                "\"uploadedBy\":" + r.getUploadedBy() + "," +
                "\"uploaderName\":\"" + escape(r.getUploaderName()) + "\"," +
                "\"uploadDate\":\"" + escape(r.getUploadDate()) + "\"," +
                "\"downloadsCount\":" + r.getDownloadsCount() + "," +
                "\"isBookmarked\":" + r.isBookmarked() +
                "}";
    }

    public static String resourcesToJson(List<Resource> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(resourceToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            Object val = entry.getValue();
            if (val == null) {
                sb.append("null");
            } else if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else if (val instanceof String) {
                sb.append("\"").append(escape((String) val)).append("\"");
            } else {
                sb.append("\"").append(escape(val.toString())).append("\"");
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Simple JSON parser for flat key-value pairs (String, Number, Boolean)
     */
    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        int len = trimmed.length();
        int i = 0;
        while (i < len) {
            // Find key start
            while (i < len && (Character.isWhitespace(trimmed.charAt(i)) || trimmed.charAt(i) == ',')) i++;
            if (i >= len) break;
            if (trimmed.charAt(i) != '"') {
                i++;
                continue;
            }
            i++; // skip opening quote
            int keyStart = i;
            while (i < len && trimmed.charAt(i) != '"') {
                if (trimmed.charAt(i) == '\\' && i + 1 < len) i++;
                i++;
            }
            String key = unescape(trimmed.substring(keyStart, i));
            i++; // skip closing quote

            // Find colon
            while (i < len && trimmed.charAt(i) != ':') i++;
            i++; // skip colon

            // Find value start
            while (i < len && Character.isWhitespace(trimmed.charAt(i))) i++;
            if (i >= len) break;

            String value = "";
            if (trimmed.charAt(i) == '"') {
                i++; // skip opening quote
                int valStart = i;
                while (i < len && trimmed.charAt(i) != '"') {
                    if (trimmed.charAt(i) == '\\' && i + 1 < len) i++;
                    i++;
                }
                value = unescape(trimmed.substring(valStart, i));
                i++; // skip closing quote
            } else {
                int valStart = i;
                while (i < len && trimmed.charAt(i) != ',' && trimmed.charAt(i) != '}') {
                    i++;
                }
                value = trimmed.substring(valStart, i).trim();
                if ("null".equalsIgnoreCase(value)) value = null;
            }
            map.put(key, value);
        }
        return map;
    }

    private static String unescape(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = s.charAt(i + 1);
                if (next == '"') { sb.append('"'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else if (next == '/') { sb.append('/'); i++; }
                else if (next == 'n') { sb.append('\n'); i++; }
                else if (next == 'r') { sb.append('\r'); i++; }
                else if (next == 't') { sb.append('\t'); i++; }
                else if (next == 'b') { sb.append('\b'); i++; }
                else if (next == 'f') { sb.append('\f'); i++; }
                else if (next == 'u' && i + 5 < len) {
                    try {
                        int code = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                        sb.append((char) code);
                        i += 5;
                    } catch (NumberFormatException e) {
                        sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

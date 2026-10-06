package com.vault.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.vault.model.Resource;
import com.vault.model.Subject;
import com.vault.model.User;
import com.vault.service.AuthService;
import com.vault.service.ResourceService;
import com.vault.service.SubjectService;
import com.vault.util.JsonUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;

public class WebServer {
    private final int port;
    private final String host;
    private final AuthService authService = new AuthService();
    private final SubjectService subjectService = new SubjectService();
    private final ResourceService resourceService = new ResourceService();
    private HttpServer server;

    public WebServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        // API Contexts
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/auth/me", new CurrentUserHandler());
        server.createContext("/api/auth/logout", new LogoutHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/subjects", new SubjectsHandler());
        server.createContext("/api/resources", new ResourcesHandler());
        server.createContext("/api/download", new DownloadHandler());
        server.createContext("/api/preview", new PreviewHandler());
        server.createContext("/api/bookmarks", new BookmarksHandler());

        // File/Static contexts
        server.createContext("/uploads", new UploadsHandler());
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("Academic Resource Vault Server running at http://" + host + ":" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }

    // Helper to extract Authenticated User from Request
    private User getAuthenticatedUser(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else {
            token = exchange.getRequestHeaders().getFirst("X-Auth-Token");
        }
        if (token == null) {
            // Check query param token
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            token = query.get("token");
        }
        return authService.getUserByToken(token);
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Auth-Token");
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(statusCode, -1);
            return;
        }
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void handleCorsPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Auth-Token");
        exchange.sendResponseHeaders(204, -1);
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                try {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    map.put(key, value);
                } catch (UnsupportedEncodingException ignored) {}
            }
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // AUTH HANDLERS
    // -------------------------------------------------------------------------

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> creds = JsonUtil.parseJson(body);
            String email = creds.get("email");
            String password = creds.get("password");

            Map<String, Object> result = authService.login(email, password);
            if (Boolean.TRUE.equals(result.get("success"))) {
                User user = (User) result.get("user");
                String token = (String) result.get("token");
                String resp = "{" +
                        "\"success\":true," +
                        "\"token\":\"" + token + "\"," +
                        "\"user\":" + JsonUtil.userToJson(user) +
                        "}";
                sendJsonResponse(exchange, 200, resp);
            } else {
                sendJsonResponse(exchange, 401, "{\"success\":false,\"message\":\"" + result.get("message") + "\"}");
            }
        }
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseJson(body);
            String name = data.get("name");
            String email = data.get("email");
            String password = data.get("password");
            String role = data.get("role");

            Map<String, Object> result = authService.register(name, email, password, role);
            if (Boolean.TRUE.equals(result.get("success"))) {
                User user = (User) result.get("user");
                String token = (String) result.get("token");
                String resp = "{" +
                        "\"success\":true," +
                        "\"token\":\"" + token + "\"," +
                        "\"user\":" + JsonUtil.userToJson(user) +
                        "}";
                sendJsonResponse(exchange, 201, resp);
            } else {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + result.get("message") + "\"}");
            }
        }
    }

    private class CurrentUserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }
            User user = getAuthenticatedUser(exchange);
            if (user != null) {
                sendJsonResponse(exchange, 200, "{\"authenticated\":true,\"user\":" + JsonUtil.userToJson(user) + "}");
            } else {
                sendJsonResponse(exchange, 200, "{\"authenticated\":false,\"user\":null}");
            }
        }
    }

    private class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authService.logout(authHeader.substring(7).trim());
            }
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Logged out\"}");
        }
    }

    // -------------------------------------------------------------------------
    // STATS HANDLER
    // -------------------------------------------------------------------------

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }
            Map<String, Object> stats = resourceService.getPlatformStats();
            sendJsonResponse(exchange, 200, JsonUtil.mapToJson(stats));
        }
    }

    // -------------------------------------------------------------------------
    // SUBJECTS HANDLER
    // -------------------------------------------------------------------------

    private class SubjectsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }
            String method = exchange.getRequestMethod();
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());

            if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
                Integer sem = null;
                if (query.get("semester") != null && !query.get("semester").isEmpty()) {
                    try { sem = Integer.parseInt(query.get("semester")); } catch (Exception ignored) {}
                }
                String semType = query.get("semester_type");
                List<Subject> list = subjectService.getSubjects(sem, semType);
                sendJsonResponse(exchange, 200, JsonUtil.subjectsToJson(list));
            } else if ("POST".equalsIgnoreCase(method)) {
                User user = getAuthenticatedUser(exchange);
                if (user == null || !user.isAdmin()) {
                    sendJsonResponse(exchange, 403, "{\"error\":\"Admin privileges required\"}");
                    return;
                }
                String body = readRequestBody(exchange);
                Map<String, String> data = JsonUtil.parseJson(body);

                Subject s = new Subject();
                s.setSubjectCode(data.get("subjectCode"));
                s.setSubjectName(data.get("subjectName"));
                int sem = 1;
                try { sem = Integer.parseInt(data.get("semester")); } catch (Exception ignored) {}
                s.setSemester(sem);
                s.setSemesterType(sem % 2 == 1 ? "ODD" : "EVEN");
                s.setDepartment(data.get("department"));
                int creds = 3;
                try { creds = Integer.parseInt(data.get("credits")); } catch (Exception ignored) {}
                s.setCredits(creds);

                boolean ok = subjectService.createSubject(s);
                if (ok) {
                    sendJsonResponse(exchange, 201, "{\"success\":true,\"subject\":" + JsonUtil.subjectToJson(s) + "}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Could not create subject. Check code uniqueness.\"}");
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                User user = getAuthenticatedUser(exchange);
                if (user == null || !user.isAdmin()) {
                    sendJsonResponse(exchange, 403, "{\"error\":\"Admin privileges required\"}");
                    return;
                }
                String code = query.get("code");
                if (code != null && subjectService.deleteSubject(code)) {
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Subject deleted\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Failed to delete subject\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    // -------------------------------------------------------------------------
    // RESOURCES HANDLER
    // -------------------------------------------------------------------------

    private class ResourcesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            User user = getAuthenticatedUser(exchange);
            int currentUserId = user != null ? user.getUserId() : 0;

            // Check if path has ID: e.g. /api/resources/12
            if (path.matches("^/api/resources/\\d+$")) {
                int id = Integer.parseInt(path.substring("/api/resources/".length()));
                if ("GET".equalsIgnoreCase(method)) {
                    Resource r = resourceService.getResourceById(id, currentUserId);
                    if (r != null) {
                        sendJsonResponse(exchange, 200, JsonUtil.resourceToJson(r));
                    } else {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Resource not found\"}");
                    }
                } else if ("PUT".equalsIgnoreCase(method)) {
                    if (user == null || !user.isAdmin()) {
                        sendJsonResponse(exchange, 403, "{\"error\":\"Admin privileges required\"}");
                        return;
                    }
                    String body = readRequestBody(exchange);
                    Map<String, String> data = JsonUtil.parseJson(body);
                    Resource r = resourceService.getResourceById(id, currentUserId);
                    if (r == null) {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Resource not found\"}");
                        return;
                    }
                    if (data.containsKey("title")) r.setTitle(data.get("title"));
                    if (data.containsKey("description")) r.setDescription(data.get("description"));
                    if (data.containsKey("subjectCode")) r.setSubjectCode(data.get("subjectCode"));
                    if (data.containsKey("resourceType")) r.setResourceType(data.get("resourceType"));
                    boolean ok = resourceService.updateResource(r);
                    sendJsonResponse(exchange, ok ? 200 : 400, "{\"success\":" + ok + "}");
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    if (user == null || !user.isAdmin()) {
                        sendJsonResponse(exchange, 403, "{\"error\":\"Admin privileges required\"}");
                        return;
                    }
                    boolean ok = resourceService.deleteResource(id);
                    sendJsonResponse(exchange, ok ? 200 : 400, "{\"success\":" + ok + "}");
                } else {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
                return;
            }

            if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
                String q = query.get("q");
                Integer sem = null;
                if (query.get("semester") != null && !query.get("semester").isEmpty()) {
                    try { sem = Integer.parseInt(query.get("semester")); } catch (Exception ignored) {}
                }
                String semType = query.get("semester_type");
                String resType = query.get("type");
                String subCode = query.get("subject_code");
                boolean bookmarked = "true".equalsIgnoreCase(query.get("bookmarked"));

                List<Resource> list = resourceService.searchResources(q, sem, semType, resType, subCode, bookmarked, currentUserId);
                sendJsonResponse(exchange, 200, JsonUtil.resourcesToJson(list));
            } else if ("POST".equalsIgnoreCase(method)) {
                if (user == null || !user.isAdmin()) {
                    sendJsonResponse(exchange, 403, "{\"error\":\"Staff / Admin privileges required to upload resources\"}");
                    return;
                }

                String body = readRequestBody(exchange);
                Map<String, String> data = JsonUtil.parseJson(body);

                Resource r = new Resource();
                r.setSubjectCode(data.get("subjectCode"));
                r.setResourceType(data.get("resourceType"));
                r.setTitle(data.get("title"));
                r.setDescription(data.get("description"));
                r.setFileName(data.get("fileName"));
                r.setUploadedBy(user.getUserId());

                byte[] fileBytes = null;
                String base64Content = data.get("fileBase64");
                if (base64Content != null && !base64Content.isEmpty()) {
                    try {
                        // Remove prefix like data:application/pdf;base64, if present
                        int commaIdx = base64Content.indexOf(",");
                        if (commaIdx >= 0) {
                            base64Content = base64Content.substring(commaIdx + 1);
                        }
                        fileBytes = Base64.getDecoder().decode(base64Content.trim());
                    } catch (Exception e) {
                        System.err.println("Base64 decode failed: " + e.getMessage());
                    }
                } else if (data.get("content") != null) {
                    fileBytes = data.get("content").getBytes(StandardCharsets.UTF_8);
                }

                boolean ok = resourceService.createResource(r, fileBytes);
                if (ok) {
                    sendJsonResponse(exchange, 201, "{\"success\":true,\"resource\":" + JsonUtil.resourceToJson(r) + "}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Failed to save resource. Ensure required fields are filled.\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    // -------------------------------------------------------------------------
    // DOWNLOAD & PREVIEW HANDLERS
    // -------------------------------------------------------------------------

    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }

            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String idStr = query.get("id");
            if (idStr == null) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Resource ID required\"}");
                return;
            }

            try {
                int id = Integer.parseInt(idStr);
                User user = getAuthenticatedUser(exchange);
                int userId = user != null ? user.getUserId() : 0;
                Resource r = resourceService.getResourceById(id, userId);
                if (r == null) {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Resource not found\"}");
                    return;
                }

                // Increment download counter
                String ip = exchange.getRemoteAddress() != null ? exchange.getRemoteAddress().getAddress().getHostAddress() : "127.0.0.1";
                resourceService.recordDownload(id, userId, ip);

                byte[] bytes = resourceService.getFileContent(r);
                String fileName = r.getFileName() != null ? r.getFileName() : ("resource_" + id + "." + r.getFileExtension());

                exchange.getResponseHeaders().set("Content-Type", getMimeType(r.getFileExtension()));
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName.replaceAll("\"", "") + "\"");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (NumberFormatException e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Invalid ID\"}");
            }
        }
    }

    private class PreviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }

            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String idStr = query.get("id");
            if (idStr == null) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Resource ID required\"}");
                return;
            }

            try {
                int id = Integer.parseInt(idStr);
                User user = getAuthenticatedUser(exchange);
                int userId = user != null ? user.getUserId() : 0;
                Resource r = resourceService.getResourceById(id, userId);
                if (r == null) {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Resource not found\"}");
                    return;
                }

                byte[] bytes = resourceService.getFileContent(r);
                String textContent = new String(bytes, StandardCharsets.UTF_8);

                String json = "{" +
                        "\"resource\":" + JsonUtil.resourceToJson(r) + "," +
                        "\"content\":\"" + JsonUtil.escape(textContent) + "\"," +
                        "\"sizeBytes\":" + bytes.length +
                        "}";
                sendJsonResponse(exchange, 200, json);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Error reading file: " + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    // -------------------------------------------------------------------------
    // BOOKMARKS HANDLER
    // -------------------------------------------------------------------------

    private class BookmarksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCorsPreflight(exchange);
                return;
            }

            User user = getAuthenticatedUser(exchange);
            if (user == null) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Please log in to manage bookmarks\"}");
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> data = JsonUtil.parseJson(body);
                int resourceId = 0;
                try {
                    resourceId = Integer.parseInt(data.get("resourceId"));
                } catch (Exception ignored) {}

                if (resourceId <= 0) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Valid resourceId is required\"}");
                    return;
                }

                boolean isBookmarked = resourceService.toggleBookmark(user.getUserId(), resourceId);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"bookmarked\":" + isBookmarked + "}");
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    // -------------------------------------------------------------------------
    // UPLOADS & STATIC FILE HANDLERS
    // -------------------------------------------------------------------------

    private class UploadsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            // path is like /uploads/filename.ext
            String fileName = path.substring("/uploads/".length());
            File file = new File("uploads", fileName);
            if (!file.exists()) {
                file = new File("AcademicResourceVault/uploads", fileName);
            }
            if (!file.exists() || file.isDirectory()) {
                sendJsonResponse(exchange, 404, "{\"error\":\"File not found\"}");
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            String ext = "";
            int idx = fileName.lastIndexOf('.');
            if (idx > 0) ext = fileName.substring(idx + 1);

            exchange.getResponseHeaders().set("Content-Type", getMimeType(ext));
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File("resources/web" + path);
            if (!file.exists()) {
                file = new File("AcademicResourceVault/resources/web" + path);
            }

            if (!file.exists() || file.isDirectory()) {
                // If requesting route, fallback to index.html for SPA
                file = new File("resources/web/index.html");
                if (!file.exists()) {
                    file = new File("AcademicResourceVault/resources/web/index.html");
                }
            }

            if (!file.exists()) {
                String notFound = "<h1>404 Not Found - Academic Resource Vault</h1>";
                exchange.sendResponseHeaders(404, notFound.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            String fileName = file.getName();
            String ext = "";
            int idx = fileName.lastIndexOf('.');
            if (idx > 0) ext = fileName.substring(idx + 1);

            exchange.getResponseHeaders().set("Content-Type", getMimeType(ext));
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String getMimeType(String ext) {
        if (ext == null) return "application/octet-stream";
        switch (ext.toLowerCase()) {
            case "html": case "htm": return "text/html; charset=UTF-8";
            case "css": return "text/css; charset=UTF-8";
            case "js": return "application/javascript; charset=UTF-8";
            case "json": return "application/json; charset=UTF-8";
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "svg": return "image/svg+xml";
            case "pdf": return "application/pdf";
            case "txt": case "md": return "text/plain; charset=UTF-8";
            case "zip": return "application/zip";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default: return "application/octet-stream";
        }
    }
}

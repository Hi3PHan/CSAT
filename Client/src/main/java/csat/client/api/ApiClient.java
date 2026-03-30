package csat.client.api;

import csat.client.crypto.AESCipher;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * ApiClient — Kết nối HTTP tới Spring Boot Backend.
 *
 * Mọi request đều:
 *   1. Đóng gói JSON payload
 *   2. Mã hóa AES-128 thành Hex (dùng AESCipher tự viết)
 *   3. Gửi lên Server với header X-Encrypted: true
 *
 * Mọi response đều:
 *   1. Nhận chuỗi Hex từ Server
 *   2. Giải mã AES-128 thành JSON
 *   3. Parse JSON -> trả về JsonObject/JsonArray
 */
public class ApiClient {

    private static final String BASE_URL    = "http://localhost:8081";
    private static final String AES_KEY     = "TransportKey1234"; // 16 bytes — phải giống Backend
    private static final String ENC_HEADER  = "X-Encrypted";

    private final HttpClient  http;
    private final AESCipher   cipher;
    private final Gson        gson;

    private String accessToken;  // Lưu token sau khi đăng nhập

    public ApiClient() {
        this.http   = HttpClient.newHttpClient();
        this.cipher = new AESCipher(AES_KEY.getBytes(StandardCharsets.UTF_8));
        this.gson   = new Gson();
    }

    // ─────────────────────────────────────────────────────────────
    //  Login
    // ─────────────────────────────────────────────────────────────

    /**
     * Đăng nhập. Trả về JsonObject chứa {token, level, username}.
     */
    public JsonObject login(String username, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        System.out.println("[CLIENT] Plain payload: " + body);
        String hexPayload = cipher.encrypt(body.toString());
        System.out.println("[CLIENT] Encrypted -> " + hexPayload.substring(0, Math.min(40, hexPayload.length())) + "...");

        String raw = post("/api/auth/login", hexPayload);
        String json = cipher.decrypt(raw);
        System.out.println("[CLIENT] Decrypted response: " + json);

        return gson.fromJson(json, JsonObject.class);
    }

    // ─────────────────────────────────────────────────────────────
    //  Get all users
    // ─────────────────────────────────────────────────────────────

    public JsonArray getUsers() throws Exception {
        // GET request — không có body để mã hóa, nhưng response vẫn được mã hóa
        String raw = get("/api/users");
        String json = cipher.decrypt(raw);
        System.out.println("[CLIENT] Users decrypted JSON (first 100): " + json.substring(0, Math.min(100, json.length())));
        return gson.fromJson(json, JsonArray.class);
    }

    public JsonObject addEmployee(String fullName, String cccd, String phone,
                                  String email, String bankAccount, long salary) throws Exception {
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("Missing access token. Please login again.");
        }

        JsonObject body = new JsonObject();
        body.addProperty("fullName", fullName);
        // Compatibility keys for backends that use different DTO naming conventions.
        body.addProperty("fullname", fullName);
        body.addProperty("full_name", fullName);
        body.addProperty("name", fullName);
        body.addProperty("cccd", cccd);
        body.addProperty("phone", phone);
        body.addProperty("email", email);
        body.addProperty("bankAccount", bankAccount);
        body.addProperty("salary", salary);
        body.addProperty("algo", "AES");

        String jsonPayload = body.toString();
        System.out.println("[CLIENT] POST /api/users payload: " + jsonPayload);

        String raw;
        try {
            raw = postJson("/api/users", jsonPayload);
            System.out.println("[CLIENT] addEmployee mode: JSON");
        } catch (Exception ex) {
            System.out.println("[CLIENT] JSON mode failed, retry encrypted mode: " + ex.getMessage());
            String hexPayload = cipher.encrypt(jsonPayload);
            raw = post("/api/users", hexPayload);
            System.out.println("[CLIENT] addEmployee mode: ENCRYPTED");
        }
        String decoded = decodeResponse(raw);
        if (decoded == null || decoded.isBlank()) {
            JsonObject ok = new JsonObject();
            ok.addProperty("message", "Created");
            return ok;
        }
        try {
            return gson.fromJson(decoded, JsonObject.class);
        } catch (Exception ex) {
            JsonObject fallback = new JsonObject();
            fallback.addProperty("message", decoded);
            return fallback;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HTTP Helpers
    // ─────────────────────────────────────────────────────────────

    private String post(String path, String hexBody) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header(ENC_HEADER, "true")
                .POST(HttpRequest.BodyPublishers.ofString(hexBody, StandardCharsets.UTF_8));

        if (accessToken != null)
            builder.header("Authorization", "Bearer " + accessToken);

        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400)
            throw new RuntimeException("Server error " + response.statusCode() + ": " + response.body());
        return response.body();
    }

    private String postJson(String path, String jsonBody) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        if (accessToken != null)
            builder.header("Authorization", "Bearer " + accessToken);

        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400)
            throw new RuntimeException("Server error " + response.statusCode() + ": " + response.body());
        return response.body();
    }

    private String get(String path) throws Exception {
        // GET request — payload trống nhưng Server trả về Response mã hóa
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header(ENC_HEADER, "true")
                .GET();

        if (accessToken != null)
            builder.header("Authorization", "Bearer " + accessToken);

        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400)
            throw new RuntimeException("Server error " + response.statusCode() + ": " + response.body());

        // Nếu Server trả về Hex (header X-Encrypted: true)
        String encHeader = response.headers().firstValue(ENC_HEADER).orElse("false");
        if ("true".equalsIgnoreCase(encHeader)) return response.body();
        return response.body(); // fallback: trả plain
    }

    private String decodeResponse(String rawBody) {
        try {
            return cipher.decrypt(rawBody);
        } catch (Exception ignored) {
            return rawBody;
        }
    }

    public void setAccessToken(String token) { this.accessToken = token; }
    public String getAccessToken()           { return accessToken; }
}

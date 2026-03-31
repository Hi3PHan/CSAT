package csat.client.api;

import csat.client.crypto.AESCipher;
import csat.client.crypto.DiffieHellman;
import csat.client.crypto.SHA256;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;

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
    private static final String ENC_HEADER  = "X-Encrypted";
    private static final String SESSION_HEADER = "X-DH-Session-Id";

    private final HttpClient  http;
    private AESCipher   cipher;
    private final Gson        gson;

    private String accessToken;  // Lưu token sau khi đăng nhập
    private String sessionId;    // Lưu session ID từ DH handshake
    private String sessionKeyHex; // Lưu khóa phiên (hex) để hiển thị

    public ApiClient() {
        this.http   = HttpClient.newHttpClient();
        this.gson   = new Gson();
    }

    // ─────────────────────────────────────────────────────────────
    //  Handshake (Diffie-Hellman)
    // ─────────────────────────────────────────────────────────────

    public void handshake() throws Exception {
        DiffieHellman dh = new DiffieHellman();
        JsonObject payload = new JsonObject();
        payload.addProperty("publicKey", dh.getPublicKey().toString());

        System.out.println("[CLIENT] Sending DH Public Key to server...");
        String rawResp = postJson("/api/auth/handshake", payload.toString());
        JsonObject resJson = gson.fromJson(rawResp, JsonObject.class);
        
        // Store session ID from response
        this.sessionId = resJson.get("sessionId").getAsString();
        
        // Backend trả publicKey ở dạng hex (toString(16)), nên phải parse base 16 để tránh NumberFormatException
        BigInteger serverPublicKey = new BigInteger(resJson.get("serverPublicKey").getAsString(), 16);
        byte[] rawSecret = dh.getSharedSecretBytes(serverPublicKey);
        // Derive AES-128 key the same way as the backend: SHA-256 -> take first 16 bytes from hex string
        String hashedHex = SHA256.hashHex(rawSecret);
        this.sessionKeyHex = hashedHex.substring(0, 32);
        byte[] aesKey = AESCipher.hexToBytes(sessionKeyHex);

        this.cipher = new AESCipher(aesKey);
        System.out.println("[CLIENT] Handshake completed. Session ID: " + sessionId +
            " | Session AES-128 key (hex): " + sessionKeyHex);
    }

    // ─────────────────────────────────────────────────────────────
    //  Login
    // ─────────────────────────────────────────────────────────────

    /**
     * Đăng nhập. Trả về JsonObject chứa {token, level, username}.
     */
    public JsonObject login(String username, String password) throws Exception {
        if (this.cipher == null) {
            handshake(); // Đảm bảo đã thiết lập khóa trước khi gọi các API mã hóa
        }
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
        if (this.cipher == null) {
            handshake();
        }
        // GET request — không có body để mã hóa, nhưng response vẫn được mã hóa
        String raw = get("/api/users");
        System.out.println("[CLIENT] Users raw encrypted (len=" + raw.length() + "): " + sample(raw));
        String json = cipher.decrypt(raw);
        System.out.println("[CLIENT] Users decrypted JSON (len=" + json.length() + "): " + sample(json));
        return gson.fromJson(json, JsonArray.class);
    }

    public JsonObject addEmployee(String fullName, String cccd, String phone,
                                  String email, String bankAccount, long salary) throws Exception {
        if (this.cipher == null) {
            handshake();
        }
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
        System.out.println("[CLIENT] addEmployee raw response (len=" + raw.length() + "): " + sample(raw));
        String decoded = decodeResponse(raw);
        System.out.println("[CLIENT] addEmployee decoded response (len=" + (decoded == null ? 0 : decoded.length()) + "): " + sample(decoded));
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

        if (sessionId != null && !sessionId.isEmpty())
            builder.header(SESSION_HEADER, sessionId);

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

        if (sessionId != null && !sessionId.isEmpty())
            builder.header(SESSION_HEADER, sessionId);

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

        if (sessionId != null && !sessionId.isEmpty())
            builder.header(SESSION_HEADER, sessionId);

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

    public String getSessionKeyHex() throws Exception {
        ensureCipher();
        return sessionKeyHex;
    }

    /** Local helper to encrypt arbitrary text with the established session key. */
    public String encryptLocal(String plaintext) throws Exception {
        ensureCipher();
        return cipher.encrypt(plaintext);
    }

    /** Local helper to decrypt hex with the established session key. */
    public String decryptLocal(String hexCipher) throws Exception {
        ensureCipher();
        String normalized = hexCipher == null ? "" : hexCipher.replaceAll("\\s+", "");
        if (normalized.length() == 0) {
            throw new IllegalArgumentException("Hex input is empty.");
        }
        if ((normalized.length() % 32) != 0) { // 32 hex chars = 16 bytes block
            throw new IllegalArgumentException("Hex length must be a multiple of 32 characters (16 bytes blocks).");
        }
        return cipher.decrypt(normalized);
    }

    private void ensureCipher() throws Exception {
        if (this.cipher == null) {
            handshake();
        }
    }

    private String sample(String text) {
        if (text == null) return "null";
        int max = Math.min(120, text.length());
        return text.substring(0, max);
    }

    public void setAccessToken(String token) { this.accessToken = token; }
    public String getAccessToken()           { return accessToken; }
}

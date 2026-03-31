package group2.CSAT.BMTT.crypto;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * CryptoFilter €” HTTP Transport Layer Encryption.
 *
 * Bắt tất cả request/response trên các endpoint nhạy cảm (/api/**).
 *
 * REQUEST flow:
 *   FE gửi body = chuỗi Hex (JSON đã mã hóa AES)
 *   Filter giải mã Hex -> JSON gốc -> chuyển vào Controller
 *
 * RESPONSE flow:
 *   Controller trả JSON gốc
 *   Filter mã hóa JSON -> chuỗi Hex -> gửi về Client
 *
 * Áp dụng trên path: /api/** (trừ /api/auth/challenge)
 */
@Component
public class CryptoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CryptoFilter.class);

    private final DHService dhService;

    /** Header bo hi‡u request/response c ‘c m ha khng */
    public static final String ENCRYPTED_HEADER = "X-Encrypted";
    public static final String SESSION_HEADER = "X-DH-Session-Id";

    public CryptoFilter(DHService dhService) {
        this.dhService = dhService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Bỏ qua filter với các path không phải API
        String path = request.getRequestURI();

        // Pass through DH handshake to prevent infinite loops of encryption
        if (path.contains("/api/auth/handshake")) {
            System.out.println("[CryptoFilter] Bypassing DH handshake: " + path);
            return true;
        }

        return !path.contains("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Ch‰ x l nu client bo hi‡u gi encrypted payload
        String encryptedHeader = request.getHeader(ENCRYPTED_HEADER);
        boolean isEncrypted = "true".equalsIgnoreCase(encryptedHeader);

        if (isEncrypted) {
            String sessionId = request.getHeader(SESSION_HEADER);
            if (sessionId == null || sessionId.isEmpty()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing DH session");
                return;
            }
            byte[] rawSessionKey = dhService.getSessionKey(sessionId);
            if (rawSessionKey == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired DH session");
                return;
            }
            AESCipher activeCipher = new AESCipher(rawSessionKey);

            // --- DECRYPT REQUEST ---
            byte[] rawBody = request.getInputStream().readAllBytes();
            String hexPayload = new String(rawBody, StandardCharsets.UTF_8).trim();
            log.info("[CryptoFilter] Incoming encrypted payload: session={} path={} len={} hexSample={}", sessionId, request.getRequestURI(), hexPayload.length(), sample(hexPayload));

            String jsonBody   = activeCipher.decrypt(hexPayload);
            log.info("[CryptoFilter] Decrypted request JSON: session={} path={} len={} jsonSample={}", sessionId, request.getRequestURI(), jsonBody.length(), sample(jsonBody));

            // Wrap li request v›i JSON ‘ gii m
            byte[] decryptedBodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            HttpServletRequest wrappedRequest = new BodyReplacedRequest(request, decryptedBodyBytes);

            // --- CAPTURE & ENCRYPT RESPONSE ---
            ResponseCaptureWrapper capturedResponse = new ResponseCaptureWrapper(response);
            filterChain.doFilter(wrappedRequest, capturedResponse);

            byte[] originalBody  = capturedResponse.getCapturedBody();
            String originalJson  = new String(originalBody, StandardCharsets.UTF_8);
            log.info("[CryptoFilter] Controller response JSON: session={} path={} len={} jsonSample={}", sessionId, request.getRequestURI(), originalJson.length(), sample(originalJson));

            String encryptedJson = activeCipher.encrypt(originalJson);
            log.info("[CryptoFilter] Outgoing encrypted payload: session={} path={} len={} hexSample={}", sessionId, request.getRequestURI(), encryptedJson.length(), sample(encryptedJson));

            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader(ENCRYPTED_HEADER, "true");
            // Also echo back the session header so client knows returning payload uses the same key
            if (sessionId != null) {
                response.setHeader(SESSION_HEADER, sessionId);
            }
            response.getOutputStream().write(encryptedJson.getBytes(StandardCharsets.UTF_8));
        } else {
            // Yêu cầu mọi request API phải mã hóa bằng khóa phiên
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Encrypted header required");
        }
    }

    private String sample(String content) {
//        if (content == null) return "null";
//        int max = Math.min(content.length(), 200);
//        return content.substring(0, max);
        return content;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner Helper: Thay thế body của HttpServletRequest
    // ─────────────────────────────────────────────────────────────────────────

    private static class BodyReplacedRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final byte[] body;

        public BodyReplacedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public String getContentType() {
            return "application/json;charset=UTF-8";
        }

        @Override
        public String getHeader(String name) {
            if ("content-type".equalsIgnoreCase(name)) return "application/json;charset=UTF-8";
            return super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            if ("content-type".equalsIgnoreCase(name))
                return java.util.Collections.enumeration(java.util.Collections.singletonList("application/json;charset=UTF-8"));
            return super.getHeaders(name);
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            InputStream is = new ByteArrayInputStream(body);
            return new jakarta.servlet.ServletInputStream() {
                @Override public boolean isFinished() { 
                    try { return is.available() == 0; } catch (IOException e) { return true; } 
                }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(jakarta.servlet.ReadListener l) {}
                @Override public int read() throws IOException { return is.read(); }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(
                new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner Helper: Capture response body trước khi ghi ra client
    // ─────────────────────────────────────────────────────────────────────────

    private static class ResponseCaptureWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        private jakarta.servlet.ServletOutputStream  outputStream;
        private java.io.PrintWriter                  writer;

        public ResponseCaptureWrapper(HttpServletResponse response) { super(response); }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() {
            if (outputStream == null) {
                outputStream = new jakarta.servlet.ServletOutputStream() {
                    @Override public boolean isReady() { return true; }
                    @Override public void setWriteListener(jakarta.servlet.WriteListener l) {}
                    @Override public void write(int b) { baos.write(b); }
                    @Override public void write(byte[] b, int off, int len) { baos.write(b, off, len); }
                };
            }
            return outputStream;
        }

        @Override
        public java.io.PrintWriter getWriter() {
            if (writer == null) {
                writer = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(baos, StandardCharsets.UTF_8));
            }
            return writer;
        }

        public byte[] getCapturedBody() {
            if (writer != null) writer.flush();
            return baos.toByteArray();
        }
    }
}

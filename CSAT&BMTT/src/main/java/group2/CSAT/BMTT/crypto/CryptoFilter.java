package group2.CSAT.BMTT.crypto;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * CryptoFilter — HTTP Transport Layer Encryption.
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

    private final AESCipher cipher;

    /** Header báo hiệu request/response có được mã hóa không */
    public static final String ENCRYPTED_HEADER = "X-Encrypted";

    public CryptoFilter(@Value("${app.aes.transport-key}") String transportKey) {
        this.cipher = new AESCipher(transportKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Bỏ qua filter với các path không phải API
        String path = request.getServletPath();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Chỉ xử lý nếu client báo hiệu gửi encrypted payload
        String encryptedHeader = request.getHeader(ENCRYPTED_HEADER);
        boolean isEncrypted = "true".equalsIgnoreCase(encryptedHeader);

        if (isEncrypted) {
            // --- DECRYPT REQUEST ---
            byte[] rawBody = request.getInputStream().readAllBytes();
            String hexPayload = new String(rawBody, StandardCharsets.UTF_8).trim();
            String jsonBody   = cipher.decrypt(hexPayload);

            // Wrap lại request với JSON đã giải mã
            byte[] decryptedBodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            HttpServletRequest wrappedRequest = new BodyReplacedRequest(request, decryptedBodyBytes);

            // --- CAPTURE & ENCRYPT RESPONSE ---
            ResponseCaptureWrapper capturedResponse = new ResponseCaptureWrapper(response);
            filterChain.doFilter(wrappedRequest, capturedResponse);

            byte[] originalBody  = capturedResponse.getCapturedBody();
            String originalJson  = new String(originalBody, StandardCharsets.UTF_8);
            String encryptedJson = cipher.encrypt(originalJson);

            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader(ENCRYPTED_HEADER, "true");
            response.getOutputStream().write(encryptedJson.getBytes(StandardCharsets.UTF_8));
        } else {
            // Không mã hóa: đi thẳng vào controller (dùng khi test bằng Postman)
            filterChain.doFilter(request, response);
        }
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

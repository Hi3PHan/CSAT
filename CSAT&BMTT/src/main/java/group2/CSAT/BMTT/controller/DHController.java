package group2.CSAT.BMTT.controller;

import group2.CSAT.BMTT.crypto.DHService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth") // Modify to match /api/auth/handshake
public class DHController {

    @Autowired
    private DHService dhService;

    @PostMapping("/handshake")
    public ResponseEntity<DHService.DHResponse> handshake(@RequestBody Map<String, Object> payload) {
        System.out.println("[DHController] Received handshake request. Payload: " + payload);
        
        // Cố gắng lấy key hỗ trợ cả các trường hợp tên biến (key) khác nhau từ FE
        String clientPublicKey = null;
        if (payload.containsKey("clientPublicKey")) {
            clientPublicKey = String.valueOf(payload.get("clientPublicKey"));
        } else if (payload.containsKey("publicKey")) {
            clientPublicKey = String.valueOf(payload.get("publicKey"));
        } else if (payload.containsKey("client_public_key")) {
            clientPublicKey = String.valueOf(payload.get("client_public_key"));
        }
        
        if (clientPublicKey == null || clientPublicKey.isEmpty() || "null".equals(clientPublicKey)) {
            System.out.println("[DHController] Client public key is missing or invalid");
            return ResponseEntity.badRequest().build();
        }

        System.out.println("[DHController] Processing handshake");
        DHService.DHResponse response = dhService.handshake(clientPublicKey);
        
        System.out.println("[DHController] Handshake complete, returning response");
        return ResponseEntity.ok(response);
    }
}

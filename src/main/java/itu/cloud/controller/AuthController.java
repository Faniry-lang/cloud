package itu.cloud.controller;

import itu.cloud.dto.LoginRequest;
import itu.cloud.dto.LoginResponse;
import itu.cloud.service.FirebaseAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final FirebaseAuthService firebaseAuthService;

    public AuthController(FirebaseAuthService firebaseAuthService) {
        this.firebaseAuthService = firebaseAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = firebaseAuthService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }
}


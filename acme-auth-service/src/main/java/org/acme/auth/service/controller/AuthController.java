package org.acme.auth.service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.auth.service.dto.UserInfoResponse;
import org.acme.auth.service.entity.UserRole;
import org.acme.auth.service.repository.UserRepository;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping("/users/{dn}")
    public ResponseEntity<UserInfoResponse> getUserByDn(@PathVariable("dn") String dn) {
        log.debug("Looking up user by DN: {}", dn);

        return userRepository.findByDn(dn)
                .map(user -> {
                    List<String> roles = user.getRoles().stream()
                            .map(UserRole::getRoleName)
                            .toList();

                    log.debug("Found user: dn={}, givenName={}, surname={}, roles={}",
                            user.getDn(), user.getGivenName(), user.getSurname(), roles);

                    return ResponseEntity.ok(UserInfoResponse.builder()
                            .dn(user.getDn())
                            .givenName(user.getGivenName())
                            .surname(user.getSurname())
                            .roles(roles)
                            .build());
                })
                .orElseGet(() -> {
                    log.debug("User not found: {}", dn);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }
}

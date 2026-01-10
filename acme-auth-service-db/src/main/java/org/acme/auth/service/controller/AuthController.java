package org.acme.auth.service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.auth.service.dto.UserInfoResponse;
import org.acme.auth.service.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/users/{dn}")
    public ResponseEntity<UserInfoResponse> getUserByDn(@PathVariable("dn") String dn) {
        log.debug("Looking up user by DN: {}", dn);

        UserInfoResponse user = userService.findByDn(dn);
        log.debug("Found user: dn={}, givenName={}, surname={}, roles={}",
                user.getDn(), user.getGivenName(), user.getSurname(), user.getRoles());

        return ResponseEntity.ok(user);
    }
}

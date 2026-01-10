package org.acme.auth.service.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.auth.service.dto.UserInfoResponse;
import org.acme.auth.service.entity.User;
import org.acme.auth.service.entity.UserRole;
import org.acme.auth.service.exception.UserNotFoundException;
import org.acme.auth.service.repository.UserRepository;
import org.acme.auth.service.service.UserService;

/**
 * Implementation of {@link UserService} for querying user information from
 * database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbUserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserInfoResponse findByDn(String dn) {
        if (!StringUtils.hasText(dn)) {
            throw new UserNotFoundException(dn);
        }

        User user = userRepository.findByDnIgnoreCase(dn)
                .orElseThrow(() -> {
                    log.debug("User not found in database: {}", dn);
                    return new UserNotFoundException(dn);
                });

        log.debug("Found user in database: {}", dn);

        // Extract roles from UserRole entities
        List<String> roles = user.getRoles().stream()
                .map(UserRole::getRoleName)
                .collect(Collectors.toList());

        return UserInfoResponse.builder()
                .dn(user.getDn())
                .givenName(user.getGivenName())
                .surname(user.getSurname())
                .roles(roles)
                .build();
    }
}

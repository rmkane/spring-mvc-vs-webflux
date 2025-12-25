package org.acme.auth.client;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfo implements UserDetails {

    private final String dn;
    private final String givenName;
    private final String surname;
    private final List<String> roles;

    public UserInfo(String dn, String givenName, String surname, List<String> roles) {
        this.dn = dn;
        this.givenName = givenName;
        this.surname = surname;
        this.roles = roles != null ? roles : List.of("ROLE_USER");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return null; // Not used for header-based authentication
    }

    @Override
    public String getUsername() {
        return dn; // Spring Security expects getUsername(), return DN
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

package com.hospital.security;

import com.hospital.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter that wraps the domain {@link User} entity into a Spring Security
 * {@link UserDetails} object consumed by the security framework.
 *
 * <h3>Design note — "username" vs email</h3>
 * <p>Spring Security's {@link UserDetails} contract requires a {@code getUsername()}
 * method. Because the {@link User} entity has no {@code username} column (only
 * {@code email}), we use <b>email as the Spring Security username</b>.
 * The JWT subject is also the email address.</p>
 *
 * <h3>Role mapping</h3>
 * <p>The single {@link com.hospital.model.Role} enum value (e.g.,
 * {@code ROLE_DOCTOR}) is mapped directly to a {@link SimpleGrantedAuthority}.
 * Spring's {@code hasRole("DOCTOR")} strips the "ROLE_" prefix automatically.</p>
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long   id;
    private final String email;    // used as the Spring Security "username"
    private final String name;     // User.name — for display / controller use
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long id, String email, String name,
                          String password,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id          = id;
        this.email       = email;
        this.name        = name;
        this.password    = password;
        this.authorities = authorities;
    }

    /**
     * Factory method — builds a {@link UserPrincipal} from a domain {@link User}.
     *
     * @param user the persisted user entity
     * @return a ready-to-use Spring Security principal
     */
    public static UserPrincipal build(User user) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().name())
        );
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),    // email is the "username" identifier
                user.getName(),
                user.getPassword(),
                authorities
        );
    }

    // ── UserDetails contract ──────────────────────────────────────────────────

    /**
     * Returns the email address as the Spring Security username.
     * The JWT token subject is also set to this value.
     */
    @Override
    public String getUsername() {
        return email;   // email serves as the unique login identifier
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}

package com.hospital.security;

import com.hospital.model.User;
import com.hospital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security {@link UserDetailsService} that loads a {@link User}
 * <b>by email address</b>.
 *
 * <p>Because the {@link User} entity has no {@code username} column,
 * we treat the <b>email</b> as the unique login credential.
 * Spring Security passes whatever string the user submitted in the login
 * form to {@link #loadUserByUsername(String)} — we expect that string to
 * be an email address.</p>
 *
 * <p>Called by:</p>
 * <ul>
 *   <li>{@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}
 *       during login.</li>
 *   <li>{@link JwtAuthenticationFilter} when reconstructing the
 *       {@link org.springframework.security.core.context.SecurityContext}
 *       from a JWT on each subsequent request.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their <b>email address</b>.
     *
     * <p>Despite the parameter name {@code username} (required by the
     * {@link UserDetailsService} contract), we treat it as an email.</p>
     *
     * @param email the email address submitted at login
     * @return a populated {@link UserPrincipal}
     * @throws UsernameNotFoundException if no user with that email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("No user found with email: {}", email);
                    return new UsernameNotFoundException(
                            "User not found with email: " + email);
                });
        log.debug("Loaded user: id={}, role={}", user.getId(), user.getRole());
        return UserPrincipal.build(user);
    }

    /**
     * Loads a user by their database primary key.
     * Used by the JWT filter when the token carries a user-ID claim
     * rather than the email subject.
     *
     * @param id the user's primary key
     * @return a populated {@link UserPrincipal}
     * @throws UsernameNotFoundException if no user with that ID exists
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with id: " + id));
        return UserPrincipal.build(user);
    }
}

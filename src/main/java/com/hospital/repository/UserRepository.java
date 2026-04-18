package com.hospital.repository;

import com.hospital.model.Role;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} entity.
 *
 * <p>Inherits full CRUD from {@link JpaRepository}:</p>
 * <ul>
 *   <li>{@code save(User)}           — INSERT or UPDATE</li>
 *   <li>{@code findById(Long)}        — SELECT by primary key</li>
 *   <li>{@code findAll()}             — SELECT all rows</li>
 *   <li>{@code deleteById(Long)}      — DELETE by primary key</li>
 *   <li>{@code existsById(Long)}      — existence check by PK</li>
 *   <li>{@code count()}               — total row count</li>
 * </ul>
 *
 * <p>Custom derived-query methods are listed below.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ── Lookup ───────────────────────────────────────────────────────────────

    /**
     * Find a user by their unique email address.
     * Used during login and duplicate-email validation.
     *
     * <pre>SELECT * FROM users WHERE email = ?</pre>
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their display name (exact match, case-sensitive).
     * Prefer {@link #findByEmail} for authentication; use this for admin search.
     *
     * <pre>SELECT * FROM users WHERE name = ?</pre>
     */
    Optional<User> findByName(String name);

    // ── Existence Checks ─────────────────────────────────────────────────────

    /**
     * Returns {@code true} if an account with this email already exists.
     * Use before registration to give a friendly duplicate-email error.
     *
     * <pre>SELECT COUNT(*) > 0 FROM users WHERE email = ?</pre>
     */
    boolean existsByEmail(String email);

    // ── Filtered Queries ─────────────────────────────────────────────────────

    /**
     * Retrieve all users that have a specific role.
     * Useful for admin dashboards (e.g., list all doctors or all patients).
     *
     * <pre>SELECT * FROM users WHERE role = ?</pre>
     */
    List<User> findByRole(Role role);

    /**
     * Search users whose name contains the given keyword (case-insensitive).
     * Uses JPQL LOWER + LIKE for cross-database compatibility.
     *
     * <pre>SELECT * FROM users WHERE LOWER(name) LIKE LOWER('%keyword%')</pre>
     *
     * @param keyword partial name to search for
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByName(@Param("keyword") String keyword);
}

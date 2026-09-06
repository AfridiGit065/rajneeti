package com.rajneeti.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Placeholder {@link UserDetailsService} implementation.
 *
 * <p><strong>Module 01 stub only.</strong> This will be replaced in Module 02
 * when the {@code User} entity, repository, and authentication service are implemented.
 * It currently throws {@link UsernameNotFoundException} for every lookup so that
 * the JWT filter gracefully denies access without any null-pointer errors.
 *
 * <p>The bean is required now because {@link SecurityConfig} and
 * {@link JwtAuthenticationFilter} both depend on {@link UserDetailsService}.
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO (Module 02): Query UserRepository and return a real UserDetails
        log.warn("UserDetailsService.loadUserByUsername called for '{}' – stub implementation. " +
                 "Replace in Module 02.", username);
        throw new UsernameNotFoundException(
                "User '%s' not found. Full user management will be available in Module 02."
                        .formatted(username));
    }
}

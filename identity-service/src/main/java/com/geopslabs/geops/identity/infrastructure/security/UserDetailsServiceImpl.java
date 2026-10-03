package com.geopslabs.geops.identity.infrastructure.security;

import com.geopslabs.geops.identity.infrastructure.persistence.UserJpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for providing the user details to the Spring Security framework.
 * It implements the UserDetailsService interface.
 */
@Service(value = "defaultUserDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserJpaRepository userRepository;

    public UserDetailsServiceImpl(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * This method is responsible for loading the user details from the database.
     * @param username The username (email).
     * @return The UserDetails object.
     * @throws UsernameNotFoundException If the user is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        return UserDetailsImpl.build(user);
    }
}

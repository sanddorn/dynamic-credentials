package de.bermuda.hero.backend;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
public class BackendSecurityConfiguration {

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository,
                                                 @Value("${rest.username}") String username,
                                                 @Value("${rest.password}") String password) {

        var userEntry = new UserEntry();
        userEntry.setUsername(username);
        userEntry.setPassword(password);
        userEntry.setRoles(Set.of("USER"));
        userRepository.save(userEntry);
        return new MongoUserDetailsService(userRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString();
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword != null && rawPassword.toString().equals(encodedPassword);
            }
        };
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz ->
                                               authz.requestMatchers("/actuator/**").permitAll()
                                                    .requestMatchers("/**").authenticated()
                                                    .requestMatchers("/hero/**").authenticated()
                ).httpBasic().and().csrf().disable();
        return http.build();
    }

    class MongoUserDetailsService implements UserDetailsService {
        private final Logger LOGGER = LoggerFactory.getLogger(MongoUserDetailsService.class);
        private final UserRepository userRepository;

        public MongoUserDetailsService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        /**
         * Locates the user based on the username. In the actual implementation, the search
         * may possibly be case sensitive, or case insensitive depending on how the
         * implementation instance is configured. In this case, the <code>UserDetails</code>
         * object that comes back may have a username that is of a different case than what
         * was actually requested..
         *
         * @param username the username identifying the user whose data is required.
         * @return a fully populated user record (never <code>null</code>)
         * @throws UsernameNotFoundException if the user could not be found or the user has no
         *                                   GrantedAuthority
         */
        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            LOGGER.info("Checking User '{}'", username);
            var userEntryOptional = userRepository.findById(username);
            if (userEntryOptional.isPresent()) {
                var userEntry = userEntryOptional.get();
                var roles = userEntry.getRoles().stream()
                                     .map(SimpleGrantedAuthority::new)
                                     .toList();
                return new User(userEntry.getUsername(), userEntry.getPassword(), true, true, true, true, roles);
            }
            LOGGER.warn("User not found: {}", username);
            throw new UsernameNotFoundException("No such user!");
        }
    }
}

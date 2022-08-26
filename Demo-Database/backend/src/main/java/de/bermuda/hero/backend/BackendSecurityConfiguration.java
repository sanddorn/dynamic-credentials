package de.bermuda.hero.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
public class BackendSecurityConfiguration {
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
    public UserDetailsManager userDetailsService(@Value("${rest.username}") String username,
                                                 @Value("${rest.password}") String password) {

        UserDetails user1 = User.builder()
                                .username(username)
                                .password(password)
                                .roles("USER")
                                .build();
        return new InMemoryUserDetailsManager(user1);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) -> authz
                        .antMatchers("/actuator").permitAll()
                        .antMatchers("/*").authenticated()
                        .antMatchers("/hero/**").authenticated()
                ).httpBasic().and().csrf().disable();
        return http.build();
    }
}

package com.jden.espylah.webapi.app.api.signup;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class SignupServiceImpl implements ISignupService {

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsManager userDetailsManager;

    public SignupServiceImpl(PasswordEncoder passwordEncoder, UserDetailsManager userDetailsManager) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsManager = userDetailsManager;
    }

    @Override
    public void signup(UserSignUpController.SignupRequest signupRequest) {

        log.info("Creating new user : {}", signupRequest.email());
        UserDetails roleUser = User.builder()
                .passwordEncoder(passwordEncoder::encode)
                .username(signupRequest.email())
                .password(signupRequest.password())
                .authorities("ROLE_USER")
                .build();

        userDetailsManager.createUser(roleUser);

    }
}

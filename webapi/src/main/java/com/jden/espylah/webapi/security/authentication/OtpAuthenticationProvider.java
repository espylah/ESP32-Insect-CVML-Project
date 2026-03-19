package com.jden.espylah.webapi.security.authentication;

import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.models.LoginOneTimeToken;
import com.jden.espylah.webapi.db.repos.LoginOneTimeTokenRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;

@Log4j2
public class OtpAuthenticationProvider implements AuthenticationProvider {

    private final LoginOneTimeTokenRepo tokenRepo;
    private final UserDetailsManager userDetailsManager;
    private final TimeUtil timeUtil;

    public OtpAuthenticationProvider(LoginOneTimeTokenRepo tokenRepo, UserDetailsManager userDetailsManager, TimeUtil timeUtil) {
        this.tokenRepo = tokenRepo;
        this.userDetailsManager = userDetailsManager;
        this.timeUtil = timeUtil;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String otpToken = (String) authentication.getPrincipal();

        LoginOneTimeToken lott = tokenRepo.findById(otpToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid one-time token"));

        if (Boolean.TRUE.equals(lott.getUsed())) {
            throw new BadCredentialsException("One-time token already used");
        }

        if (lott.getExpiresAt().isBefore(timeUtil.now())) {
            throw new BadCredentialsException("One-time token expired");
        }

        lott.setUsed(true);
        tokenRepo.save(lott);

        UserDetails userDetails = userDetailsManager.loadUserByUsername(lott.getUser().getUsername());
        log.info("OTP authentication successful for user: {}", userDetails.getUsername());
        return new OtpAuthenticationToken(userDetails, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OtpAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
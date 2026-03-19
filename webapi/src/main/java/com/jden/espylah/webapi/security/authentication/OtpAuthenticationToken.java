package com.jden.espylah.webapi.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;

public class OtpAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    public OtpAuthenticationToken(String otpToken) {
        super(new ArrayList<>());
        this.principal = otpToken;
        setAuthenticated(false);
    }

    public OtpAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
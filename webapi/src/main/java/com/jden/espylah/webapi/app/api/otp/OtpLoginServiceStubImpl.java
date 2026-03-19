package com.jden.espylah.webapi.app.api.otp;

import com.jden.espylah.webapi.app.utils.RandomUtil;
import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.models.LoginOneTimeToken;
import com.jden.espylah.webapi.db.repos.LoginOneTimeTokenRepo;
import com.jden.espylah.webapi.db.repos.UserRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Log4j2
@ConditionalOnProperty(name = "app.otp.stub", havingValue = "true", matchIfMissing = false)
public class OtpLoginServiceStubImpl implements OtpLoginService {

    private final LoginOneTimeTokenRepo tokenRepo;
    private final UserRepo userRepo;
    private final RandomUtil randomUtil;
    private final TimeUtil timeUtil;

    public OtpLoginServiceStubImpl(LoginOneTimeTokenRepo tokenRepo, UserRepo userRepo, RandomUtil randomUtil, TimeUtil timeUtil) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.randomUtil = randomUtil;
        this.timeUtil = timeUtil;
    }

    @Override
    public OtpRequestResponse requestToken(String email) {
        var user = userRepo.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found for that email"));

        var lott = new LoginOneTimeToken();
        lott.setToken(randomUtil.generateRandomAlphaNumericString(8));
        lott.setExpiresAt(timeUtil.now().plus(Duration.ofMinutes(10)));
        lott.setUsed(false);
        lott.setUser(user);
        tokenRepo.save(lott);

        log.info("OTP stub: token created for user: {}", email);
        return new OtpRequestResponse(lott.getToken(), lott.getExpiresAt().getEpochSecond());
    }
}

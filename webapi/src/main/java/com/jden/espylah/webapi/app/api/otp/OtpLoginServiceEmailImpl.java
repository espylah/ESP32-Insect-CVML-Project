package com.jden.espylah.webapi.app.api.otp;

import com.jden.espylah.webapi.app.utils.RandomUtil;
import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.models.LoginOneTimeToken;
import com.jden.espylah.webapi.db.repos.LoginOneTimeTokenRepo;
import com.jden.espylah.webapi.db.repos.UserRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Log4j2
@ConditionalOnProperty(name = "app.otp.stub", havingValue = "false", matchIfMissing = true)
public class OtpLoginServiceEmailImpl implements OtpLoginService {

    private final LoginOneTimeTokenRepo tokenRepo;
    private final UserRepo userRepo;
    private final RandomUtil randomUtil;
    private final TimeUtil timeUtil;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public OtpLoginServiceEmailImpl(LoginOneTimeTokenRepo tokenRepo, UserRepo userRepo, RandomUtil randomUtil, TimeUtil timeUtil, JavaMailSender mailSender) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.randomUtil = randomUtil;
        this.timeUtil = timeUtil;
        this.mailSender = mailSender;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Your ESP-YLAH one-time login code");
        message.setText("Your login code is: " + lott.getToken() + "\n\nThis code expires in 10 minutes.");
        mailSender.send(message);

        log.info("OTP emailed to user: {}", email);
        return new OtpRequestResponse(null, lott.getExpiresAt().getEpochSecond());
    }
}

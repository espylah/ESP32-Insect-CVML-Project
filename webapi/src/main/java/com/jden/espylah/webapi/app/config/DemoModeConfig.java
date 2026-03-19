package com.jden.espylah.webapi.app.config;

import com.jden.espylah.webapi.db.models.User;
import com.jden.espylah.webapi.db.repos.SpeciesRepo;
import com.jden.espylah.webapi.db.repos.UserRepo;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

@Log4j2
@Configuration
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoModeConfig {

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;
    private final UserRepo userRepo;
    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;
    private final SpeciesRepo speciesRepo;

    public DemoModeConfig(DataSource dataSource, ResourceLoader resourceLoader, UserRepo userRepo, UserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder, SpeciesRepo speciesRepo) {
        this.dataSource = dataSource;
        this.resourceLoader = resourceLoader;
        this.userRepo = userRepo;
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.speciesRepo = speciesRepo;
    }

    @PostConstruct
    public void init() throws SQLException {
        log.info("Initializing DemoModeConfig");

        userRepo.deleteAll();

        ScriptUtils.executeSqlScript(dataSource.getConnection(), resourceLoader.getResource("classpath:/demo/h2-init-demo.sql"));

        User user = new User();
        user.setUsername("alice@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setUserAuthorities(Collections.singleton("ROLE_USER"));
        user.setEnabled(true);
        userRepo.save(user);


        for (User user2 : userRepo.findAll()) {
            log.info("User: {}->{}", user2.getUsername(), user2.getPassword());
        }

        speciesRepo.findAll().forEach(log::info);

    }

}

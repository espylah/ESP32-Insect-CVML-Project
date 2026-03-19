package com.jden.espylah.webapi.db.repos;

import com.jden.espylah.webapi.db.models.LoginOneTimeToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginOneTimeTokenRepo extends JpaRepository<LoginOneTimeToken, String> {
}
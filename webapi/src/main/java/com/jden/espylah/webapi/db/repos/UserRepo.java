package com.jden.espylah.webapi.db.repos;

import com.jden.espylah.webapi.db.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, String> {
}

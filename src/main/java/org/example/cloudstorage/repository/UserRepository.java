package org.example.cloudstorage.repository;

import java.util.Optional;
import org.example.cloudstorage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);
}

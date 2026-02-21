package com.example.construction.reposirtories;

import com.example.construction.Enums.Role;
import com.example.construction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    List<User> findAllByRole(Role role);

}

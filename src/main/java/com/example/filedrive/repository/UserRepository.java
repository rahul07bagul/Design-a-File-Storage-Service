package com.example.filedrive.repository;

import com.example.filedrive.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, String> {
    @Query("SELECT DISTINCT u FROM User u WHERE u.email LIKE %:email% OR u.name LIKE %:name%")
    List<User> findDistinctByEmailContainingOrNameContaining(
            String email, String name);
}

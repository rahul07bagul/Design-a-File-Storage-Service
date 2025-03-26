package com.example.filedrive.service;

import com.example.filedrive.dto.SearchUser;
import com.example.filedrive.model.User;
import com.example.filedrive.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Validated
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

//    public Integer createUser(@Valid UserRequest userRequest) {
//        try {
//            if (userRequest == null) {
//                throw new IllegalArgumentException("User request cannot be null");
//            }
//
//            if (userRequest.getUsername() == null || userRequest.getUsername().trim().isEmpty()) {
//                throw new IllegalArgumentException("Username cannot be empty");
//            }
//
//            if (userRequest.getEmail() == null || userRequest.getEmail().trim().isEmpty()) {
//                throw new IllegalArgumentException("Email cannot be empty");
//            }
//
//            User newUser = new User();
//            newUser.setUsername(userRequest.getUsername());
//            newUser.setPassword(userRequest.getPassword());
//            newUser.setEmail(userRequest.getEmail());
//            newUser.setFirstName(userRequest.getFirstName());
//            newUser.setLastName(userRequest.getLastName());
//
//            try {
//                User savedUser = userRepository.save(newUser);
//                return savedUser.getId();
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to save user to database: " + e.getMessage(), e);
//            }
//        } catch (IllegalArgumentException e) {
//            throw e;
//        } catch (Exception e) {
//            System.err.println("Error creating user: " + e.getMessage());
//            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
//        }
//    }

    public void saveOrUpdateUser(String uid, String email, String name){
        try {
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setId(uid);
            userRepository.save(user);
        }catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    public User getUserById(String id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }

            try {
                return userRepository.findById(id).orElse(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve user from database: " + e.getMessage(), e);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
            return null;
        }
    }

    public List<SearchUser> searchUsers(String searchKeyword) {
        try {
            if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
                throw new IllegalArgumentException("Search keyword cannot be empty");
            }

            List<User> userList;
            try {
                userList = userRepository.findDistinctByEmailContainingOrNameContaining(
                        searchKeyword, searchKeyword);

                if (userList == null) {
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to search users in database: " + e.getMessage(), e);
            }

            List<SearchUser> searchUserList = new ArrayList<>();
            for (User user : userList) {
                try {
                    SearchUser searchUser = new SearchUser();
                    searchUser.setUserId(user.getId());
                    searchUser.setEmail(user.getEmail());
                    searchUser.setName(user.getName());
                    searchUserList.add(searchUser);
                } catch (Exception e) {
                    System.err.println("Error mapping user to SearchUser: " + e.getMessage());
                    // Continue with the next user
                }
            }

            return searchUserList;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error searching users: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
package com.example.filedrive.controller;

import com.example.filedrive.dto.SearchUser;
import com.example.filedrive.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drive")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

//    @PostMapping("/user/create")
//    public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
//        logger.info("API Call: POST /user/create");
//        try {
//            if (request == null || request.getUsername() == null || request.getEmail() == null) {
//                logger.warn("Invalid user data: Username and email are required");
//                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user data. Username and email are required."));
//            }
//
//            Integer userId = userService.createUser(request);
//            if (userId != null) {
//                logger.info("User created successfully with ID: {}", userId);
//                return ResponseEntity.ok().body(Map.of("userId", userId, "message", "User created successfully"));
//            } else {
//                logger.warn("User creation failed");
//                return ResponseEntity.badRequest().body(Map.of("error", "User creation failed."));
//            }
//        } catch (IllegalArgumentException e) {
//            logger.warn("Bad request during user creation: {}", e.getMessage());
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        } catch (Exception e) {
//            logger.error("Error creating user: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Internal server error during user creation: " + e.getMessage()));
//        }
//    }

    @GetMapping("/users/search/{searchKeyword}")
    public ResponseEntity<List<SearchUser>> searchFiles(@PathVariable String searchKeyword) {
        logger.info("API Call: GET /users/search/{}", searchKeyword);
        try {
            if (searchKeyword == null || searchKeyword.isBlank()) {
                logger.warn("Invalid search keyword: Cannot be null or empty");
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<SearchUser> users = userService.searchUsers(searchKeyword);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error searching users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
}
package com.siva.services.withdrawservice.controller;

import com.siva.services.withdrawservice.dao.impl.UserDatabaseService;
import com.siva.services.withdrawservice.dto.UserRequest;
import com.siva.services.withdrawservice.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/v1/internal/user")
@Log4j2
public class UserController {

    private final UserDatabaseService userDatabaseService;

    @Autowired
    public UserController(UserDatabaseService userDatabaseService) {
        this.userDatabaseService = userDatabaseService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody UserRequest userRequest) {

        String id = null;
        try {
            id = userDatabaseService.insert(User.builder().name(userRequest.getName())
                    .balance(userRequest.getBalance()).build());
        } catch(Exception e){
            log.error("Error inserting user ", e);
        }
        return ResponseEntity.ok(Map.of("id", id));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{userId}", produces = "application/json")
    public ResponseEntity<User> getUser(@PathVariable("userId") String userId) {
        User user = null;
        user = userDatabaseService.get(userId)
                    .orElseThrow(() -> new RuntimeException("Could not find user with id=" + userId));
        return ResponseEntity.ok(user);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list/all", produces = "application/json")
    public ResponseEntity<List<User>> userList() {
        List<User> results = null;
        results = userDatabaseService.listAll();
        return ResponseEntity.ok(results);
    }

}

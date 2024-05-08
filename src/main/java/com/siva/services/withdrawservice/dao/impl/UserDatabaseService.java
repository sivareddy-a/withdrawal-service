package com.siva.services.withdrawservice.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.services.withdrawservice.dao.DatabaseService;
import com.siva.services.withdrawservice.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class UserDatabaseService extends DatabaseService<User> {

    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;

    public UserDatabaseService(NamedParameterJdbcTemplate template, PlatformTransactionManager transactionManager, ObjectMapper objectMapper) {
        this.template = template;
        this.transactionManager = transactionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public String insert(User u) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("balance",u.getBalance());
        params.addValue("name",u.getName());
        String insertUser = "INSERT INTO users (name, balance) values (:name, :balance)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String[] keyColumns = {"id"};
        UUID userId = null;
        try {
            int updatedRows = template.update(insertUser, params, keyHolder, keyColumns);
            if (updatedRows <= 0) {
                throw new SQLException("Failed to insert user");
            }
            userId = (UUID) keyHolder.getKeys().get("id");
        } catch (Exception e) {
            log.error("Error inserting user", e);
            throw new RuntimeException("could not insert user");
        }
        assert userId != null;
        return userId.toString();
    }

    @Override
    public Boolean update(User u) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name",u.getName());
        params.addValue("balance",u.getBalance());
        params.addValue("id",u.getId());
        String updateUser = "UPDATE users SET name =:name, balance =:balance, updated_at = now() WHERE id = :id";
        try {
            int updatedRows = template.update(updateUser, params);
            if (updatedRows <= 0) {
                throw new SQLException("Failed to update uset");
            }
        } catch (Exception e) {
            log.error("Error updating user", e);
            throw new RuntimeException("could not update user");
        }
        return true;
    }

    public synchronized Boolean updateBalance(UUID userId, Double balance) {
        Optional<User> userOpt = get(userId.toString());
        if(userOpt.isPresent()){
            User user = userOpt.get();
            return update(user.toBuilder().balance(balance).build());
        }
        return false;
    }


    @Override
    public synchronized Optional<User> get(String id) {
        String getUser = "SELECT * FROM users where id =:id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id",UUID.fromString(id));
        List<User> users = template.query(getUser, params, (rs,rowNum) -> {
            return User.builder()
                    .id((UUID) rs.getObject("id"))
                    .name(rs.getString("name"))
                    .balance(rs.getDouble("balance"))
                    .createdAt((rs.getTimestamp("created_at")).toInstant())
                    .updatedAt((rs.getTimestamp("updated_at")).toInstant())
                    .build();
        });
        if(!users.isEmpty())
            return Optional.of(users.get(0));
        return Optional.empty();
    }

    @Override
    public List<User> listAll() {
        String getUser = "SELECT * FROM users";
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<User> users = template.query(getUser, params, (rs,rowNum) -> {
            return User.builder()
                    .id((UUID) rs.getObject("id"))
                    .name(rs.getString("name"))
                    .name(rs.getString("name"))
                    .balance(rs.getDouble("balance"))
                    .createdAt((rs.getTimestamp("created_at")).toInstant())
                    .updatedAt((rs.getTimestamp("updated_at")).toInstant())
                    .build();
        });
        return users;
    }

    @Override
    public Boolean delete(String id) {
        String deleteUser = "DELETE FROM users WHERE id =:id";
        return template.update(deleteUser, Map.of("id", UUID.fromString(id))) > 0;
    }
}

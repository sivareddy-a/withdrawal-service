package com.siva.services.withdrawservice.dao.impl;

import com.siva.services.withdrawservice.dao.DatabaseService;
import com.siva.services.withdrawservice.model.TransactionStatus;
import com.siva.services.withdrawservice.model.Withdrawl;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Log4j2
public class WithdrawlDatabaseService extends DatabaseService<Withdrawl> {

    private final NamedParameterJdbcTemplate template;

    public WithdrawlDatabaseService(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public String insert(Withdrawl w) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        populateParams(w, params);
        String insertWithdrawl = "INSERT INTO withdrawls (id, sender_id, address, amount, status, failure) values (:id, :sender_id, :address, :amount, :status, :failure)";
        try {
            int updatedRows = template.update(insertWithdrawl, params);
            if (updatedRows <= 0) {
                throw new SQLException("Failed to insert withdrawl");
            }
        } catch (Exception e) {
            log.error("Error inserting withdrawl", e);
            throw new RuntimeException("could not insert withdrawl");
        }
        return w.getId().toString();
    }

    private void populateParams(Withdrawl w, MapSqlParameterSource params) {
        params.addValue("id",w.getId());
        params.addValue("sender_id",w.getSenderId());
        params.addValue("address",w.getAddress());
        params.addValue("amount",w.getAmount());
        params.addValue("status",w.getStatus().getStatus());
        params.addValue("failure",w.getFailure());
    }

    @Override
    public Boolean update(Withdrawl t) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        populateParams(t, params);
        String updateWithdrawl = "UPDATE withdrawls SET sender_id =:sender_id, address =:address, amount =:amount, status =:status, failure =:failure, updated_at = now() WHERE id = :id";
        try {
            int updatedRows = template.update(updateWithdrawl, params);
            if (updatedRows <= 0) {
                throw new SQLException("Failed to update withdrawl");
            }
        } catch (Exception e) {
            log.error("Error updating withdrawl", e);
            throw new RuntimeException("could not update withdrawl");
        }
        return true;
    }

    public Boolean updateStatus(UUID withdrawlId, TransactionStatus status) {
        return updateStatus(withdrawlId, status, null);
    }

    public Boolean updateStatus(UUID withdrawlId, TransactionStatus status, String failure) {
        Withdrawl withdrawl = get(withdrawlId.toString()).orElseThrow();
        return update(withdrawl.toBuilder().status(status).failure(failure).build());
    }

    @Override
    public Optional<Withdrawl> get(String id) {
        String getWithdrawl = "SELECT * FROM withdrawls where id =:id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id",UUID.fromString(id));
        List<Withdrawl> withdrawls = template.query(getWithdrawl, params, withdrawlRowMapper);
        if(!withdrawls.isEmpty())
            return Optional.of(withdrawls.get(0));
        return Optional.empty();
    }

    @Override
    public List<Withdrawl> listAll() {
        String getWithdrawl = "SELECT * FROM withdrawls";
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<Withdrawl> withdrawls = template.query(getWithdrawl, params, withdrawlRowMapper);
        return withdrawls;
    }

    @Override
    public Boolean delete(String id) {
        String deleteWithdrawl = "DELETE FROM withdrawls WHERE id =:id";
        return template.update(deleteWithdrawl, Map.of("id", UUID.fromString(id))) > 0;
    }

    RowMapper<Withdrawl> withdrawlRowMapper =  (rs, rowNum) -> {
        return Withdrawl.builder()
                .id((UUID) rs.getObject("id"))
                .senderId((UUID)rs.getObject("sender_id"))
                .address(rs.getString("address"))
                .amount(rs.getDouble("amount"))
                .status(TransactionStatus.fromString(rs.getString("status")))
                .failure(rs.getString("failure"))
                .createdAt((rs.getTimestamp("created_at")).toInstant())
                .updatedAt((rs.getTimestamp("updated_at")).toInstant())
                .build();
    };
}

package com.siva.services.withdrawservice.dao.impl;

import com.siva.services.withdrawservice.dao.DatabaseService;
import com.siva.services.withdrawservice.model.TransactionStatus;
import com.siva.services.withdrawservice.model.Transfer;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Log4j2
public class TransferDatabaseService extends DatabaseService<Transfer> {

    private final NamedParameterJdbcTemplate template;

    public TransferDatabaseService(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public String insert(Transfer t) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        populateParams(t, params);
        String insertTransfer = "INSERT INTO transfers (sender_id, receiver_id, amount, status) values (:sender_id, :receiver_id, :amount, :status)";
        UUID transferId = null;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String[] keyColumns = {"id"};
        try {
            int updatedRows = template.update(insertTransfer, params, keyHolder, keyColumns);
            if (updatedRows <= 0) {
                throw new SQLException("Failed to insert transfer");
            }
            transferId = (UUID) keyHolder.getKeys().get("id");
        } catch (Exception e) {
            log.error("Error inserting transfer", e);
            throw new RuntimeException("could not insert transfer");
        }
        assert transferId != null;
        return transferId.toString();
    }

    private void populateParams(Transfer t, MapSqlParameterSource params) {
        if(t.getId() != null){
            params.addValue("id",t.getId());
        }
        params.addValue("sender_id",t.getSenderId());
        params.addValue("receiver_id",t.getReceiverId());
        params.addValue("amount",t.getAmount());
        params.addValue("status",t.getStatus().toString());
        params.addValue("failure", t.getFailure());
    }

    @Override
    public Boolean update(Transfer t) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        populateParams(t, params);
        String updateTransfer = "UPDATE transfers SET sender_id =:sender_id, receiver_id =:receiver_id, amount =:amount, status =:status, failure =:failure, updated_at = now() WHERE id = :id";
        try {
            int updatedRows = template.update(updateTransfer, params);
            if (updatedRows <= 0) {
                throw new SQLException("Failed to update transfer");
            }
        } catch (Exception e) {
            log.error("Error updating transfer", e);
            throw new RuntimeException("could not update transfer");
        }
        return true;
    }

    public Boolean updateStatus(UUID transferId, TransactionStatus status) {
        return updateStatus(transferId, status, null);
    }

    public Boolean updateStatus(UUID transferId, TransactionStatus status, String failure) {
        Transfer transfer = get(transferId.toString()).orElseThrow();
        return update(transfer.toBuilder().status(status).failure(failure).build());
    }

    @Override
    public Optional<Transfer> get(String id) {
        String getTransfer = "SELECT * FROM transfers where id =:id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id",UUID.fromString(id));
        List<Transfer> transfers = template.query(getTransfer, params, transferRowMapper);
        if(!transfers.isEmpty())
            return Optional.of(transfers.get(0));
        return Optional.empty();
    }

    @Override
    public List<Transfer> listAll() {
        String getTransfer = "SELECT * FROM transfers";
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<Transfer> transfers = template.query(getTransfer, params, transferRowMapper);
        return transfers;
    }

    @Override
    public Boolean delete(String id) {
        String deleteTransfer = "DELETE FROM transfers WHERE id =:id";
        return template.update(deleteTransfer, Map.of("id", UUID.fromString(id))) > 0;
    }

    RowMapper<Transfer> transferRowMapper = (rs, rowNum) -> {
        return Transfer.builder()
                .id((UUID) rs.getObject("id"))
                .senderId((UUID)rs.getObject("sender_id"))
                .receiverId((UUID)rs.getObject("receiver_id"))
                .amount(rs.getDouble("amount"))
                .status(TransactionStatus.fromString(rs.getString("status")))
                .failure(rs.getString("failure"))
                .createdAt((rs.getTimestamp("created_at")).toInstant())
                .updatedAt((rs.getTimestamp("updated_at")).toInstant())
                .build();
    };
}

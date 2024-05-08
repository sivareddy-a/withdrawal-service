package com.siva.services.withdrawservice.controller;
import com.siva.services.withdrawservice.dto.ErrorResponse;
import com.siva.services.withdrawservice.dto.TransferRequest;
import com.siva.services.withdrawservice.dto.WithdrawalRequest;
import com.siva.services.withdrawservice.model.Transfer;
import com.siva.services.withdrawservice.model.TransactionStatus;
import com.siva.services.withdrawservice.model.Withdrawl;
import com.siva.services.withdrawservice.service.impl.TransactionService;
import lombok.extern.log4j.Log4j2;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/transactions")
@Log4j2
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/transfer", produces = "application/json")
    public ResponseEntity<?> makeTransfer(@RequestBody TransferRequest request) {
        try {
            Transfer transfer = transactionService.makeTransfer(request);
            return ResponseEntity.ok(transfer);
        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{transferId}/status", produces = "application/json")
    public ResponseEntity<TransactionStatus> getTransferStatus(@PathVariable("transferId") String transferId) {
            return ResponseEntity.ok(transactionService.getTransferState(UUID.fromString(transferId)));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/withdrawal", produces = "application/json")
    public ResponseEntity<?> requestWithdrawal(@RequestBody WithdrawalRequest request) {
        try {
            Withdrawl withdrawal = transactionService.makeWithdrawal(request);
            return ResponseEntity.ok(withdrawal);
        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{withdrawalId}/status", produces = "application/json")
    public ResponseEntity<TransactionStatus> getWithdrawalStatus(@PathVariable("withdrawalId") String withdrawalId) {
        return ResponseEntity.ok(transactionService.getWithdrawalState(UUID.fromString(withdrawalId)));
    }

}

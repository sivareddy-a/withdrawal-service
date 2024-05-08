package com.siva.services.withdrawservice.service.impl;

import com.siva.services.withdrawservice.dao.impl.TransferDatabaseService;
import com.siva.services.withdrawservice.dao.impl.UserDatabaseService;
import com.siva.services.withdrawservice.dao.impl.WithdrawlDatabaseService;
import com.siva.services.withdrawservice.dto.TransferRequest;
import com.siva.services.withdrawservice.dto.WithdrawalRequest;
import com.siva.services.withdrawservice.model.Transfer;
import com.siva.services.withdrawservice.model.User;
import com.siva.services.withdrawservice.model.TransactionStatus;
import com.siva.services.withdrawservice.model.Withdrawl;
import com.siva.services.withdrawservice.service.WithdrawalService;
import lombok.extern.log4j.Log4j2;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Log4j2
public class TransactionService {

    private final PlatformTransactionManager transactionManager;

    private final TransferDatabaseService transferDatabaseService;

    private final UserDatabaseService userDatabaseService;

    private final WithdrawalService withdrawalService;

    private final WithdrawlDatabaseService withdrawalDatabaseService;

    public TransactionService(PlatformTransactionManager transactionManager, TransferDatabaseService transferDatabaseService, UserDatabaseService userDatabaseService, WithdrawalService withdrawalService, WithdrawlDatabaseService withdrawalDatabaseService) {
        this.transactionManager = transactionManager;
        this.transferDatabaseService = transferDatabaseService;
        this.userDatabaseService = userDatabaseService;
        this.withdrawalService = withdrawalService;
        this.withdrawalDatabaseService = withdrawalDatabaseService;
    }

    public Transfer makeTransfer(TransferRequest request) throws BadRequestException {
        validateRequest(request);
        String transferId = makeTransfer(request.getSenderId(), request.getReceiverId(), request.getAmount());
        return transferDatabaseService.get(transferId).orElse(null);
    }
    private void validateRequest(TransferRequest request) throws BadRequestException {
        if(request.getSenderId() == null){
            throw new BadRequestException("sender_id not provided");
        }
        if(request.getReceiverId() == null) {
            throw new BadRequestException("receiver_id not provided");
        }
        if(request.getAmount() == null){
            throw new BadRequestException("amount not provided");
        }
    }

    private synchronized String makeTransfer(UUID senderId, UUID receiverId, Double amount) {
        //insert new transfer transaction
        String transferId = transferDatabaseService.insert(Transfer.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .amount(amount)
                    .status(TransactionStatus.PROCESSING)
                    .build());

        UUID firstUserId = senderId.compareTo(receiverId) < 0 ? senderId : receiverId;
        UUID secondUserId = senderId.compareTo(receiverId) < 0 ? receiverId : senderId;

        // Acquire locks in a consistent order
        synchronized (firstUserId) {
            synchronized (secondUserId) {
                //validate sender's account balance
                User sender = userDatabaseService.get(senderId.toString()).orElseThrow(() -> new IllegalArgumentException("Sender not found"));
                if (sender.getBalance() < amount) {
                    transferDatabaseService.updateStatus(UUID.fromString(transferId), TransactionStatus.FAILED, "Insufficient funds!");
                } else {
                    User receiver = userDatabaseService.get(receiverId.toString()).orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
                    DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
                    txDef.setTimeout(10);
                    org.springframework.transaction.TransactionStatus txStatus = transactionManager.getTransaction(txDef);
                    try{
                        AtomicReference<Double> senderBalance = new AtomicReference<>(sender.getBalance());
                        AtomicReference<Double> receiverBalance = new AtomicReference<>(receiver.getBalance());
                        senderBalance.getAndUpdate(x -> x - amount);
                        userDatabaseService.updateBalance(senderId, senderBalance.get());
                        receiverBalance.getAndUpdate(x -> x + amount);
                        userDatabaseService.updateBalance(receiverId, receiverBalance.get());
                        transferDatabaseService.updateStatus(UUID.fromString(transferId), TransactionStatus.COMPLETED);
                        transactionManager.commit(txStatus);
                    } catch(TransactionTimedOutException e){
                        transactionManager.rollback(txStatus);
                        transferDatabaseService.updateStatus(UUID.fromString(transferId), TransactionStatus.FAILED, "Transaction timed out!!");
                        throw e;
                    }  catch(Exception e){
                        transactionManager.rollback(txStatus);
                        transferDatabaseService.updateStatus(UUID.fromString(transferId), TransactionStatus.FAILED, "Transaction failed!!");
                        throw  e;
                    }
                }
                return transferId;
            }
        }
    }

    public TransactionStatus getTransferState(UUID transferId) {
        Optional<Transfer> transfer = transferDatabaseService.get(transferId.toString());
        if(transfer.isPresent()){
            return transfer.get().getStatus();
        } else
            throw new RuntimeException("transfer does not exist");
    }

    public Withdrawl makeWithdrawal(WithdrawalRequest request) throws BadRequestException {
        validateRequest(request);
        UUID withdrawalId = UUID.randomUUID();
        makeWithdrawal(withdrawalId, request.getSenderId(), request.getAddress(), request.getAmount());
        Withdrawl withdrawl = withdrawalDatabaseService.get(withdrawalId.toString()).get();
        return withdrawl;
    }

    private synchronized void makeWithdrawal(UUID withdrawalId, UUID senderId, String address,  Double amount) { // throws InsufficientFundsException, SQLException
        //could've handled errors in controller, but didn't want to change method signature as per requirement
        //insert a withdrawal transaction with a random ID
        withdrawalDatabaseService.insert(Withdrawl.builder()
                .id(withdrawalId)
                .senderId(senderId)
                .address(address)
                .amount(amount)
                .status(TransactionStatus.PROCESSING)
                .build());
        //validate sender's account balance
        User sender = userDatabaseService.get(senderId.toString()).orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        synchronized(sender){
            if (sender.getBalance() < amount) {
                withdrawalDatabaseService.updateStatus(withdrawalId, TransactionStatus.FAILED, "Insufficient funds!");
            } else {
                WithdrawalService.WithdrawalId withdrawalIdObj = new WithdrawalService.WithdrawalId(withdrawalId);
                try {
                    withdrawalService.requestWithdrawal(withdrawalIdObj, new WithdrawalService.Address(address), amount);
                } catch (Exception e) {
                    log.error("Error from withdrawal servcie API ", e);
                    throw new RuntimeException("Error from withdrawal servcie API");
                }
                WithdrawalService.WithdrawalState state = fetchWithdrawalStateFromStub(withdrawalIdObj);
                // Handle withdrawal status
                switch (state) {
                    case COMPLETED:
                        // Proceed with payment
                        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
                        org.springframework.transaction.TransactionStatus txStatus = transactionManager.getTransaction(txDef);
                        try {
                            userDatabaseService.updateBalance(senderId, sender.getBalance() - amount);
                            withdrawalDatabaseService.updateStatus(withdrawalId, TransactionStatus.COMPLETED);
                            transactionManager.commit(txStatus);
                        } catch (Exception e) {
                            transactionManager.rollback(txStatus);
                            withdrawalDatabaseService.updateStatus(withdrawalId, TransactionStatus.FAILED, "Transaction failed!!");
                            throw e;
                        }
                        System.out.println("Payment processed successfully.");
                        break;
                    case FAILED:
                        // Update failure
                        withdrawalDatabaseService.updateStatus(withdrawalId, TransactionStatus.FAILED, "Third-party service failed transaction");
                        break;
                }
            }
        }
    }

    private WithdrawalService.WithdrawalState fetchWithdrawalStateFromStub(WithdrawalService.WithdrawalId withdrawalIdObj) {
        //Ideally, if withdrawalService provided callback mechanism, we could have used it to post results when they are ready
        WithdrawalService.WithdrawalState state = null;
        while (state == null || state == WithdrawalService.WithdrawalState.PROCESSING) {
            try {
                Thread.sleep(1000); // Poll every 1 second
                state = withdrawalService.getRequestState(withdrawalIdObj);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for withdrawal status");
            }
        }
        return state;
    }

    private void validateRequest(WithdrawalRequest request) throws BadRequestException {
        if(request.getSenderId() == null){
            throw new BadRequestException("sender_id not provided");
        }
        if(request.getAddress() == null || request.getAddress().isEmpty()) {
            throw new BadRequestException("address not provided");
        }
        if(request.getAmount() == null){
            throw new BadRequestException("amount not provided");
        }
    }

    public TransactionStatus getWithdrawalState(UUID withdrawalId) {
        Optional<Withdrawl> withdrawl = withdrawalDatabaseService.get(withdrawalId.toString());
        if(withdrawl.isPresent()){
            return withdrawl.get().getStatus();
        } else
            throw new RuntimeException("withdrawl does not exist");
    }

}

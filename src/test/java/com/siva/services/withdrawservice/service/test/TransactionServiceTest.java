package com.siva.services.withdrawservice.service.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.services.withdrawservice.dao.impl.TransferDatabaseService;
import com.siva.services.withdrawservice.dao.impl.UserDatabaseService;
import com.siva.services.withdrawservice.dao.impl.WithdrawlDatabaseService;
import com.siva.services.withdrawservice.dto.TransferRequest;
import com.siva.services.withdrawservice.dto.WithdrawalRequest;
import com.siva.services.withdrawservice.model.TransactionStatus;
import com.siva.services.withdrawservice.model.Transfer;
import com.siva.services.withdrawservice.model.User;
import com.siva.services.withdrawservice.model.Withdrawl;
import com.siva.services.withdrawservice.service.WithdrawalService;
import com.siva.services.withdrawservice.service.impl.TransactionService;
import com.siva.services.withdrawservice.service.impl.WithdrawalServiceStub;
import lombok.extern.log4j.Log4j2;
import org.apache.coyote.BadRequestException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
public class TransactionServiceTest {
    private static DataSource dataSource;
    private static NamedParameterJdbcTemplate template;
    private static PlatformTransactionManager transactionManager;
    private static ObjectMapper objectMapper;
    private static WithdrawlDatabaseService withdrawlDatabaseService;
    private static UserDatabaseService userDatabaseService;
    private static WithdrawalService withdrawlService;
    private static  TransferDatabaseService transferDatabaseService;
    private static  TransactionService transactionService;

    private static String userId1;

    private static String userId2;

    @BeforeClass
    public static void setUp() {
        dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();
        template = new NamedParameterJdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
        objectMapper = new ObjectMapper();
        userDatabaseService = new UserDatabaseService(template, transactionManager, objectMapper);
        transferDatabaseService = new TransferDatabaseService(template);
        withdrawlDatabaseService = new WithdrawlDatabaseService(template);
        withdrawlService = new WithdrawalServiceStub();
        transactionService = new TransactionService(transactionManager, transferDatabaseService, userDatabaseService, withdrawlService, withdrawlDatabaseService);
        userId1 = userDatabaseService.insert(User.builder().name("user1").balance(1000.0).build());
        userId2 = userDatabaseService.insert(User.builder().name("user2").balance(1000.0).build());
    }

    @Test
    public void testWithdrawalsSimple() throws BadRequestException {
        //success scenario
        WithdrawalRequest request = WithdrawalRequest.builder()
                .senderId(UUID.fromString(userId1))
                .address("address 1")
                .amount(100.0)
                .build();

        Withdrawl withdrawl1 = transactionService.makeWithdrawal(request);

        User user1 = userDatabaseService.get(userId1).get();
        Withdrawl withdrawl1Actual = withdrawlDatabaseService.get(withdrawl1.getId().toString()).get();
        assertEquals(user1.getBalance(), 900.0);
        assertEquals(withdrawl1Actual.getStatus(), TransactionStatus.COMPLETED);

        //check for insufficient funds
        Withdrawl withdrawl2 = transactionService.makeWithdrawal(request.toBuilder().amount(950.0).build());
        assertEquals(TransactionStatus.FAILED, withdrawl2.getStatus());
        assertEquals("Insufficient funds!", withdrawl2.getFailure());
    }

    @Test
    public void testWithdrawalsConcurrent() throws InterruptedException {
        int numberOfThreads = 3; // Number of concurrent threads
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        WithdrawalRequest request = WithdrawalRequest.builder()
                .senderId(UUID.fromString(userId1))
                .address("address 1")
                .amount(100.0)
                .build();

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try{
                    transactionService.makeWithdrawal(request);
                } catch (BadRequestException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown(); // Decrease the count of the latch when thread completes
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        //User balance should be 0
        User user1 = userDatabaseService.get(userId1).get();
        List<Withdrawl> withdrawls = withdrawlDatabaseService.listAll();
        long successfulWithdrawals = withdrawls.stream().filter(w -> w.getStatus() == TransactionStatus.COMPLETED).count();
        assertEquals(1000.0-(successfulWithdrawals*100.0),user1.getBalance());
        assertEquals(numberOfThreads, withdrawls.size());
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    @Test
    public void testTransfersSimple() throws BadRequestException {
        //success scenario
        TransferRequest request = TransferRequest.builder()
                .senderId(UUID.fromString(userId1))
                .receiverId(UUID.fromString(userId2))
                .amount(100.0)
                .build();

        Transfer transfer1 = transactionService.makeTransfer(request);

        User user1 = userDatabaseService.get(userId1).get();
        User user2 = userDatabaseService.get(userId2).get();
        Transfer transfer1Actual = transferDatabaseService.get(transfer1.getId().toString()).get();
        assertEquals(user1.getBalance(), 900.0);
        assertEquals(user2.getBalance(), 1100.0);
        assertEquals(transfer1Actual.getStatus(), TransactionStatus.COMPLETED);

        //check for insufficient funds
        Transfer transfer2 = transactionService.makeTransfer(request.toBuilder().amount(950.0).build());
        assertEquals(TransactionStatus.FAILED, transfer2.getStatus());
        assertEquals("Insufficient funds!", transfer2.getFailure());
    }

    @Test
    public void testTransfersConcurrent() throws InterruptedException {
        int numberOfThreads = 5; // Number of concurrent threads
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        TransferRequest request = TransferRequest.builder()
                .senderId(UUID.fromString(userId1))
                .receiverId(UUID.fromString(userId2))
                .amount(500.0)
                .build();

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try{
                    transactionService.makeTransfer(request);
                } catch (BadRequestException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown(); // Decrease the count of the latch when thread completes
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);

        User user1 = userDatabaseService.get(userId1).get();
        List<Transfer> transfers = transferDatabaseService.listAll();
        long successfulTransfers = transfers.stream().filter(w -> w.getStatus() == TransactionStatus.COMPLETED).count();
        assertEquals(1000.0-(successfulTransfers*500.0),user1.getBalance());
        assertEquals(numberOfThreads, transfers.size());
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    @Test
    public void testTransferssAndWithdrawalsConcurrent() throws InterruptedException {
        int numberOfThreads = 6; // Number of concurrent threads
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderId(UUID.fromString(userId1))
                .receiverId(UUID.fromString(userId2))
                .amount(100.0)
                .build();

        WithdrawalRequest withdrawalRequest = WithdrawalRequest.builder()
                .senderId(UUID.fromString(userId1))
                .address("address 1")
                .amount(100.0)
                .build();

        for (int i = 0; i < numberOfThreads; i++) {
            int finalI = i;
            executor.submit(() -> {
                try{
                    switch(finalI %2){
                        case 0:
                            transactionService.makeTransfer(transferRequest);
                            break;
                        case 1:
                            transactionService.makeWithdrawal(withdrawalRequest);
                    }
                } catch (BadRequestException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown(); // Decrease the count of the latch when thread completes
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);

        User user1 = userDatabaseService.get(userId1).get();
        List<Withdrawl> withdrawls = withdrawlDatabaseService.listAll();
        long successfulWithdrawals = withdrawls.stream().filter(w -> w.getStatus() == TransactionStatus.COMPLETED).count();
        List<Transfer> transfers = transferDatabaseService.listAll();
        long successfulTransfers = transfers.stream().filter(w -> w.getStatus() == TransactionStatus.COMPLETED).count();
        assertEquals(1000.0-((successfulTransfers+successfulWithdrawals)*100.0),user1.getBalance());
        assertEquals(numberOfThreads, transfers.size() + withdrawls.size());
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }


}

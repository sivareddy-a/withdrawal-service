package com.siva.services.withdrawservice.dao.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.services.withdrawservice.dao.impl.TransferDatabaseService;
import com.siva.services.withdrawservice.dao.impl.UserDatabaseService;
import com.siva.services.withdrawservice.model.TransactionStatus;
import com.siva.services.withdrawservice.model.Transfer;
import com.siva.services.withdrawservice.model.User;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TransferDatabaseServiceTest {

    private static DataSource dataSource;
    private static NamedParameterJdbcTemplate template;
    private static PlatformTransactionManager transactionManager;
    private static ObjectMapper objectMapper;
    private static TransferDatabaseService transferDatabaseService;
    private static UserDatabaseService userDatabaseService;

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
    }

    @Test
    public void test() throws SQLException {
        String userId1 = userDatabaseService.insert(User.builder().name("user1").balance(100.0).build());
        String userId2 = userDatabaseService.insert(User.builder().name("user2").balance(100.0).build());

        //testInsert
        Transfer transfer1 = Transfer.builder().senderId(UUID.fromString(userId1)).receiverId(UUID.fromString(userId2)).amount(50.0).status(TransactionStatus.PROCESSING).build();
        String transferId1 = transferDatabaseService.insert(transfer1);

        //testGet
        Transfer transfer1Actual = transferDatabaseService.get(transferId1).get();
        transfer1 = transfer1.toBuilder().id(UUID.fromString(transferId1)).build();
        assertThat(transfer1Actual).isEqualToIgnoringGivenFields(transfer1,"createdAt","updatedAt");

        //testUpdateStatus
        transferDatabaseService.updateStatus(UUID.fromString(transferId1), TransactionStatus.COMPLETED);
        transfer1Actual = transferDatabaseService.get(transferId1).get();
        transfer1 = transfer1.toBuilder().status(TransactionStatus.COMPLETED).build();
        assertThat(transfer1Actual).isEqualToIgnoringGivenFields(transfer1,"createdAt","updatedAt");

        //testListAll
        List<Transfer> list = transferDatabaseService.listAll();
        assertEquals(1, list.size());
    }
}

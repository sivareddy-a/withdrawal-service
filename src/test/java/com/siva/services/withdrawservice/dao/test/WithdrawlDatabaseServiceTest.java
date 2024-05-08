package com.siva.services.withdrawservice.dao.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.services.withdrawservice.dao.impl.UserDatabaseService;
import com.siva.services.withdrawservice.dao.impl.WithdrawlDatabaseService;
import com.siva.services.withdrawservice.model.TransactionStatus;
import com.siva.services.withdrawservice.model.User;
import com.siva.services.withdrawservice.model.Withdrawl;
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


public class WithdrawlDatabaseServiceTest {

    private static DataSource dataSource;
    private static NamedParameterJdbcTemplate template;
    private static PlatformTransactionManager transactionManager;
    private static ObjectMapper objectMapper;
    private static WithdrawlDatabaseService withdrawlDatabaseService;

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
        withdrawlDatabaseService = new WithdrawlDatabaseService(template);
    }

    @Test
    public void test() throws SQLException {
       /* String userId1 = userDatabaseService.insert(User.builder().name("user1").balance(100.0).build());


        //testInsert
        UUID withdrawlIdGen = UUID.randomUUID();
        Withdrawl withdrawl1 = Withdrawl.builder().id(withdrawlIdGen).senderId(UUID.fromString(userId1)).receiverId(UUID.fromString(addressId1)).amount(50.0).status(TransactionStatus.PROCESSING).build();
        String withdrawlId1 = withdrawlDatabaseService.insert(withdrawl1);

        //testGet
        Withdrawl withdrawl1Actual = withdrawlDatabaseService.get(withdrawlId1).get();
        withdrawl1 = withdrawl1.toBuilder().id(UUID.fromString(withdrawlId1)).build();
        assertThat(withdrawl1Actual).isEqualToIgnoringGivenFields(withdrawl1,"createdAt","updatedAt");

        //testUpdateStatus
        withdrawlDatabaseService.updateStatus(UUID.fromString(withdrawlId1), TransactionStatus.COMPLETED);
        withdrawl1Actual = withdrawlDatabaseService.get(withdrawlId1).get();
        withdrawl1 = withdrawl1.toBuilder().status(TransactionStatus.COMPLETED).build();
        assertThat(withdrawl1Actual).isEqualToIgnoringGivenFields(withdrawl1,"createdAt","updatedAt");

        //testListAll
        List<Withdrawl> list = withdrawlDatabaseService.listAll();
        assertEquals(1, list.size());*/
    }
}

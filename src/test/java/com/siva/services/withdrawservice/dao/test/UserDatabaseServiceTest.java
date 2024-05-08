package com.siva.services.withdrawservice.dao.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.services.withdrawservice.dao.impl.UserDatabaseService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;


public class UserDatabaseServiceTest {

    private static DataSource dataSource;
    private static NamedParameterJdbcTemplate template;
    private static PlatformTransactionManager transactionManager;
    private static ObjectMapper objectMapper;
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
    }

    @Test
    public void test() throws SQLException {
        // Arrange
        User user = User.builder().name("user1").balance(100.0).build();
        String id1 = userDatabaseService.insert(user);
        User actual = userDatabaseService.get(id1).get();
        assertEquals(id1, actual.getId().toString());
        String id2 = userDatabaseService.insert(user.toBuilder().name("user2").balance(200.0).build());
        List<User> actualtList = userDatabaseService.listAll();
        assertEquals(2, actualtList.size());
    }

}

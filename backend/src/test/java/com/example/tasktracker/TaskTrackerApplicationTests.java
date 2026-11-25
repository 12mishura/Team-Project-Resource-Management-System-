package com.example.tasktracker;

import com.example.tasktracker.security.AuthTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTrackerApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private FilterRegistrationBean<AuthTokenFilter> authTokenFilterRegistration;

    @Test
    void contextLoads() {
    }

    @Test
    void authTokenFilterIsNotRegisteredAsServletFilter() {
        assertThat(authTokenFilterRegistration.isEnabled()).isFalse();
    }

    @Test
    void migrationsAreApplied() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(applied).isEqualTo(11);
    }
}

package org.example.authentication_service.integration;

import org.example.authentication_service.init.AdminInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {
    @MockitoBean
    protected AdminInitializer adminInitializer;
}

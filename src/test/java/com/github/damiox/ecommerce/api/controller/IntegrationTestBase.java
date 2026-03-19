package com.github.damiox.ecommerce.api.controller;

import com.github.damiox.ecommerce.api.controller.objects.CredentialsDto;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {
    @LocalServerPort
    private int port;

    protected RestTemplate restTemplate;

    @Before
    public void setUp() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            @Override
            public boolean hasError(HttpStatus statusCode) {
                return false;
            }
        });
    }

    @After
    public void cleanDB() {
        // TODO: clean up db
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected String loginUrl() {
        return url("/login");
    }

    protected String dbUrl() {
        return url("/admin/db/reset");
    }

    protected String productsUrl() {
        return url("/products");
    }

    protected String loginAdmin() {
        CredentialsDto credentials = new CredentialsDto("admin", "admin");
        return login(credentials);
    }

    protected String loginUser() {
        CredentialsDto credentials = new CredentialsDto("user1", "user1");
        return login(credentials);
    }

    protected String login(CredentialsDto credentials) {
        Map<String, String> response = restTemplate.postForObject(loginUrl(), credentials, Map.class);
        return response.get("token").replace("Bearer ", "");
    }
}

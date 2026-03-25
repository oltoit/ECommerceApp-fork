package com.github.damiox.ecommerce.api.controller.security;

import com.github.damiox.ecommerce.api.controller.IntegrationTestBase;
import com.github.damiox.ecommerce.api.controller.objects.CategoryDto;
import com.github.damiox.ecommerce.api.controller.utils.CategoryUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CategoryControllerSecurityTest extends IntegrationTestBase {
    private String baseUrl;

    @Autowired
    private CategoryUtils categoryUtils;

    @Before
    public void init() {
        baseUrl = categoriesUrl();
    }

    // Options
    @Test
    public void categoriesOptions() {
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.OPTIONS, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void categoryOptions() {
        long id = categoryUtils.createCategory("test-category");

        ResponseEntity<String> response = restTemplate.exchange(categoryUrl((int) id), HttpMethod.OPTIONS, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Get all
    @Test
    public void getAllCategoriesNotLoggedIn() {
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.GET, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Get specific
    @Test
    public void getCategoryNotLoggedIn() {
        long id = categoryUtils.createCategory("test-category");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(categoryUrl((int) id), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Create category
    @Test
    public void createCategoryNotLoggedIn() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = createCategory(headers, "new-category");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void createCategoryAsNormalUser() {
        ResponseEntity<Map> response = createCategory(loginWithHeaders(user1), "new-category");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void createCategoryAsAdmin() {
        ResponseEntity<Map> response = createCategory(loginWithHeaders(admin), "new-category");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        long id = categoryUtils.getId(response);
        Map<String, Object> fromDb = categoryUtils.getCategoryAsMap(id);
        assertThat(fromDb.get("name")).isEqualTo("new-category");
    }

    // Update category
    @Test
    public void updateCategoryNotLoggedIn() {
        long id = categoryUtils.createCategory("original-category");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = updateCategory(headers, id, "updated-category");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> fromDb = categoryUtils.getCategoryAsMap(id);
        assertThat(fromDb.get("name")).isEqualTo("original-category");
    }

    @Test
    public void updateCategoryAsNormalUser() {
        long id = categoryUtils.createCategory("original-category");

        ResponseEntity<Map> response = updateCategory(loginWithHeaders(user1), id, "updated-category");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> fromDb = categoryUtils.getCategoryAsMap(id);
        assertThat(fromDb.get("name")).isEqualTo("original-category");
    }

    @Test
    public void updateCategoryAsAdmin() {
        long id = categoryUtils.createCategory("original-category");

        ResponseEntity<Map> response = updateCategory(loginWithHeaders(admin), id, "updated-category");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> fromDb = categoryUtils.getCategoryAsMap(id);
        assertThat(fromDb.get("name")).isEqualTo("updated-category");
    }

    // Delete category
    @Test
    public void deleteCategoryNotLoggedIn() {
        long id = categoryUtils.createCategory("test-category");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = deleteCategory(headers, id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> fromDb = categoryUtils.getCategoryAsMap(id);
        assertThat(fromDb.get("name")).isEqualTo("test-category");
    }

    @Test
    public void deleteCategoryAsNormalUser() {
        long id = categoryUtils.createCategory("test-category");

        ResponseEntity<Map> response = deleteCategory(loginWithHeaders(user1), id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> fromDb = categoryUtils.getCategoryAsMap(id);
        assertThat(fromDb.get("name")).isEqualTo("test-category");
    }

    @Test
    public void deleteCategoryAsAdmin() {
        long id = categoryUtils.createCategory("test-category");

        ResponseEntity<Map> response = deleteCategory(loginWithHeaders(admin), id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThatThrownBy(() -> categoryUtils.getCategoryAsMap(id))
                .isInstanceOf(EmptyResultDataAccessException.class);
    }

    // Private helper functions
    private ResponseEntity<Map> createCategory(HttpHeaders headers, String name) {
        return restTemplate.exchange(baseUrl, HttpMethod.POST, new HttpEntity<>(new CategoryDto(name), headers), Map.class);
    }

    private ResponseEntity<Map> updateCategory(HttpHeaders headers, long id, String name) {
        return restTemplate.exchange(categoryUrl((int) id), HttpMethod.PUT, new HttpEntity<>(new CategoryDto(name), headers), Map.class);
    }

    private ResponseEntity<Map> deleteCategory(HttpHeaders headers, long id) {
        return restTemplate.exchange(categoryUrl((int) id), HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
    }
}

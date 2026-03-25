package com.github.damiox.ecommerce.api.controller.security;

import com.github.damiox.ecommerce.api.controller.IntegrationTestBase;
import com.github.damiox.ecommerce.api.controller.utils.CategoryUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CategorySubcategoriesControllerSecurityTest extends IntegrationTestBase {

    @Autowired
    private CategoryUtils categoryUtils;

    // Options
    @Test
    public void subcategoriesOptions() {
        long parentId = categoryUtils.createCategory("parent");
        ResponseEntity<String> response = restTemplate.exchange(subcategoriesUrl(parentId), HttpMethod.OPTIONS, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Get all
    @Test
    public void getSubcategoriesNotLoggedIn() {
        long parentId = categoryUtils.createCategory("parent");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(subcategoriesUrl(parentId), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Add subcategory
    @Test
    public void addSubcategoryNotLoggedIn() {
        long parentId = categoryUtils.createCategory("parent");
        long childId = categoryUtils.createCategory("child");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = addSubcategory(headers, parentId, childId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // verify in db that parentId is still null
        Map<String, Object> childFromDb = categoryUtils.getCategoryAsMap(childId);
        assertThat(childFromDb.get("parentid")).isNull();
    }

    @Test
    public void addSubcategoryAsNormalUser() {
        long parentId = categoryUtils.createCategory("parent");
        long childId = categoryUtils.createCategory("child");

        ResponseEntity<Map> response = addSubcategory(loginWithHeaders(user1), parentId, childId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // verify in db that parentId is still null
        Map<String, Object> childFromDb = categoryUtils.getCategoryAsMap(childId);
        assertThat(childFromDb.get("parentid")).isNull();
    }

    @Test
    public void addSubcategoryAsAdmin() {
        long parentId = categoryUtils.createCategory("parent");
        long childId = categoryUtils.createCategory("child");

        ResponseEntity<Map> response = addSubcategory(loginWithHeaders(admin), parentId, childId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // get child category
        Map<String, Object> childCategory = categoryUtils.getCategoryAsMap(childId);
        assertThat(childCategory.get("parentid")).isEqualTo(parentId);
    }

    // Remove subcategory
    @Test
    public void removeSubcategoryNotLoggedIn() {
        long parentId = categoryUtils.createCategory("parent");
        long childId = categoryUtils.createSubcategory("child", parentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = removeSubcategory(headers, parentId, childId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // verify in db that parentid is still set
        Map<String, Object> childFromDb = categoryUtils.getCategoryAsMap(childId);
        assertThat(childFromDb.get("parentid")).isEqualTo(parentId);
    }

    @Test
    public void removeSubcategoryAsNormalUser() {
        long parentId = categoryUtils.createCategory("parent");
        long childId = categoryUtils.createSubcategory("child", parentId);

        ResponseEntity<Map> response = removeSubcategory(loginWithHeaders(user1), parentId, childId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // verify in db that parentid is still set
        Map<String, Object> childFromDb = categoryUtils.getCategoryAsMap(childId);
        assertThat(childFromDb.get("parentid")).isEqualTo(parentId);
    }

    @Test
    public void removeSubcategoryAsAdmin() {
        long parentId = categoryUtils.createCategory("parent");
        long childId = categoryUtils.createSubcategory("child", parentId);

        ResponseEntity<Map> response = removeSubcategory(loginWithHeaders(admin), parentId, childId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // verify in db that parentid is now null
        Map<String, Object> childFromDb = categoryUtils.getCategoryAsMap(childId);
        assertThat(childFromDb.get("parentid")).isNull();
    }

    // Private helper functions
    private ResponseEntity<Map> addSubcategory(HttpHeaders headers, long parentId, long childId) {
        return restTemplate.exchange(
                subcategoryUrl(parentId, childId),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class
        );
    }

    private ResponseEntity<Map> removeSubcategory(HttpHeaders headers, long parentId, long childId) {
        return restTemplate.exchange(
                subcategoryUrl(parentId, childId),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Map.class
        );
    }
}

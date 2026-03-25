package com.github.damiox.ecommerce.api.controller.security;

import com.github.damiox.ecommerce.api.controller.IntegrationTestBase;
import com.github.damiox.ecommerce.api.controller.utils.CategoryUtils;
import com.github.damiox.ecommerce.api.controller.utils.ProductCategoryUtils;
import com.github.damiox.ecommerce.api.controller.utils.ProductUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryProductsControllerSecurityTest extends IntegrationTestBase {
    private String baseUrl;
    private long categoryId;

    @Autowired
    private CategoryUtils categoryUtils;
    @Autowired
    private ProductUtils productUtils;
    @Autowired
    private ProductCategoryUtils productCategoryUtils;

    @Before
    public void init() {
        categoryId = categoryUtils.createCategory("test-category");
        baseUrl = categoryProductsUrl(categoryId);
    }

    // Options
    @Test
    public void productsOptions() {
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.OPTIONS, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Get all
    @Test
    public void getProductsNotLoggedIn() {
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.GET, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Add product
    @Test
    public void addProductNotLoggedIn() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = addProduct(headers, categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(productCategoryUtils.getCategoryIds(productId)).doesNotContain(categoryId);
    }

    @Test
    public void addProductWrongUser() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = addProduct(loginWithHeaders(user2), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(productCategoryUtils.getCategoryIds(productId)).doesNotContain(categoryId);
    }

    @Test
    public void addProductRightUser() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = addProduct(loginWithHeaders(user1), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(productCategoryUtils.getCategoryIds(productId)).contains(categoryId);
    }

    @Test
    public void addProductAsAdmin() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = addProduct(loginWithHeaders(admin), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(productCategoryUtils.getCategoryIds(productId)).contains(categoryId);
    }

    // Remove product
    @Test
    public void removeProductAsAdmin() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = removeProduct(loginWithHeaders(admin), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(productCategoryUtils.getCategoryIds(productId)).doesNotContain(categoryId);
    }

    @Test
    public void removeProductWrongUser() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = removeProduct(loginWithHeaders(user2), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(productCategoryUtils.getCategoryIds(productId)).contains(categoryId);
    }

    @Test
    public void removeProductRightUser() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = removeProduct(loginWithHeaders(user1), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(productCategoryUtils.getCategoryIds(productId)).doesNotContain(categoryId);
    }

    @Test
    public void removeProductNotLoggedIn() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = removeProduct(headers, categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(productCategoryUtils.getCategoryIds(productId)).contains(categoryId);
    }

    // Private helper functions
    private ResponseEntity<Map> addProduct(HttpHeaders headers, long categoryId, long productId) {
        return restTemplate.exchange(
                categoryProductUrl(categoryId, productId),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class
        );
    }

    private ResponseEntity<Map> removeProduct(HttpHeaders headers, long categoryId, long productId) {
        return restTemplate.exchange(
                categoryProductUrl(categoryId, productId),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Map.class
        );
    }
}

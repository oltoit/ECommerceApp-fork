package com.github.damiox.ecommerce.api.controller.functionality;

import com.github.damiox.ecommerce.api.controller.IntegrationTestBase;
import com.github.damiox.ecommerce.api.controller.utils.CategoryUtils;
import com.github.damiox.ecommerce.api.controller.utils.ProductCategoryUtils;
import com.github.damiox.ecommerce.api.controller.utils.ProductUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CategoryProductsControllerFunctionalityTest extends IntegrationTestBase {

    private String baseUrl;
    private long categoryId;
    private long notCategoryId;

    @Autowired
    private CategoryUtils categoryUtils;
    @Autowired
    private ProductUtils productUtils;
    @Autowired
    private ProductCategoryUtils productCategoryUtils;

    @Before
    public void init() {
        categoryId = categoryUtils.createCategory("test-category");
        notCategoryId = categoryId + 1;
        baseUrl = categoryProductsUrl(categoryId);
    }

    // Get all
    @Test
    public void getProducts() {
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getProductsCategoryNotFound() {
        ResponseEntity<String> response = restTemplate.exchange(categoryProductsUrl(notCategoryId), HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Add product
    @Test
    public void addProduct() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = addProduct(loginWithHeaders(user1), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(productCategoryUtils.getCategoryIds(productId)).contains(categoryId);
    }

    @Test
    public void addProductCategoryNotFound() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = addProduct(loginWithHeaders(user1), notCategoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void addProductNotFound() {
        ResponseEntity<Map> response = addProduct(loginWithHeaders(user1), categoryId, 1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void addProductAlreadyAssociated() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = addProduct(loginWithHeaders(user1), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void addCategoryToProductAlreadyCategorized() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        long categoryId2 = categoryUtils.createCategory("category-2");
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = addProduct(loginWithHeaders(user1), categoryId2, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(productCategoryUtils.getCategoryIds(productId)).contains(categoryId, categoryId2);
    }

    // Remove product
    @Test
    public void removeProduct() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = removeProduct(loginWithHeaders(user1), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(productCategoryUtils.getCategoryIds(productId)).doesNotContain(categoryId);
    }

    @Test
    public void removeProductCategoryNotFound() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = removeProduct(loginWithHeaders(user1), notCategoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void removeProductNotFound() {
        ResponseEntity<Map> response = removeProduct(loginWithHeaders(user1), categoryId, 1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void removeProductNotAssociated() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = removeProduct(loginWithHeaders(user1), categoryId, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
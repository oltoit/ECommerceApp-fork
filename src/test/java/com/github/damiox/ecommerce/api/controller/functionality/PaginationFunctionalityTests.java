package com.github.damiox.ecommerce.api.controller.functionality;

import com.github.damiox.ecommerce.api.controller.IntegrationTestBase;
import com.github.damiox.ecommerce.api.controller.objects.ProductDto;
import com.github.damiox.ecommerce.api.controller.utils.CategoryUtils;
import com.github.damiox.ecommerce.api.controller.utils.ProductCategoryUtils;
import com.github.damiox.ecommerce.api.controller.utils.ProductUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/*
Tests for testing that pagination works correctly.
They have been separated from controller-specific tests so they are shorter and more readable.
*/

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PaginationFunctionalityTests extends IntegrationTestBase {

    private String productBaseUrl;
    private String categoryProductsBaseUrl;
    private long categoryId;

    @Autowired
    private CategoryUtils categoryUtils;
    @Autowired
    private ProductUtils productUtils;
    @Autowired
    private ProductCategoryUtils productCategoryUtils;

    @Before
    public void init() {
        productBaseUrl = productsUrl();
        categoryId = categoryUtils.createCategory("test-category");
        categoryProductsBaseUrl = categoryProductsUrl(categoryId);
    }

    // ProductController Pagination Tests
    @Test
    public void getProductsPagination() {
        for (int i = 0; i < 3; i++) {
            String name = "product-"  + i;
            productUtils.createProduct(new ProductDto(name, "EUR", 10.00), user1.id);
        }

        ResponseEntity<Map> response = restTemplate.exchange(productBaseUrl + "?page=0&size=2", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("size")).isEqualTo(2);
        assertThat(page.get("totalElements")).isEqualTo(3);
        assertThat(page.get("totalPages")).isEqualTo(2);
        assertThat(page.get("number")).isEqualTo(0);
    }

    @Test
    public void getProductsPaginationPage2() {
        for (int i = 0; i < 3; i++) {
            String name = "product-"  + i;
            productUtils.createProduct(new ProductDto(name, "EUR", 10.00), user1.id);
        }

        ResponseEntity<Map> response = restTemplate.exchange(productBaseUrl + "?page=1&size=2", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("number")).isEqualTo(1);
        assertThat(page.get("totalPages")).isEqualTo(2);

        // page 2 should only have 1 element
        Map embedded = (Map) response.getBody().get("_embedded");
        List<Map> products = (List<Map>) embedded.get("productResourceList");
        assertThat(products).hasSize(1);
    }

    @Test
    public void getProductsPaginationPageOutOfBounds() {
        productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = restTemplate.exchange(productBaseUrl + "?page=999&size=20", HttpMethod.GET,new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("number")).isEqualTo(999);
        assertThat(page.get("totalElements")).isEqualTo(1);
        assertThat(page.get("size")).isEqualTo(20);
    }

    @Test
    public void getProductsPaginationNegativePageIndex() {
        productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = restTemplate.exchange(productBaseUrl + "?page=-1&size=20", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("totalElements")).isEqualTo(1);
        assertThat(page.get("size")).isEqualTo(20);
    }

    @Test
    public void getProductsPaginationNegativePageSize() {
        productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = restTemplate.exchange(productBaseUrl + "?page=0&size=-1", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("totalElements")).isEqualTo(1);
        assertThat(page.get("size")).isEqualTo(20);
    }

    @Test
    public void getProductsPaginationExceedMaxPageSize() {
        ResponseEntity<Map> response = restTemplate.exchange(productBaseUrl + "?page=0&size=5000", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Spring caps the size at 2000 instead of returning an error
        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("size")).isEqualTo(2000);
    }


    // CategoryProductsController Pagination Tests
    @Test
    public void getCategoryProductsPagination() {
        for (int i = 0; i < 3; i++) {
            long productId = productUtils.createProduct(new ProductDto("product-" + i, "EUR", 10.00), user1.id);
            productCategoryUtils.addProductToCategory(productId, categoryId);
        }

        ResponseEntity<Map> response = restTemplate.exchange(categoryProductsBaseUrl + "?page=0&size=2", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("size")).isEqualTo(2);
        assertThat(page.get("totalElements")).isEqualTo(3);
        assertThat(page.get("totalPages")).isEqualTo(2);
        assertThat(page.get("number")).isEqualTo(0);
    }

    @Test
    public void getCategoryProductsPaginationPage2() {
        for (int i = 0; i < 3; i++) {
            long productId = productUtils.createProduct(new ProductDto("product-" + i, "EUR", 10.00), user1.id);
            productCategoryUtils.addProductToCategory(productId, categoryId);
        }

        ResponseEntity<Map> response = restTemplate.exchange(categoryProductsBaseUrl + "?page=1&size=2", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("number")).isEqualTo(1);
        assertThat(page.get("totalPages")).isEqualTo(2);

        Map embedded = (Map) response.getBody().get("_embedded");
        List<Map> products = (List<Map>) embedded.get("productResourceList");
        assertThat(products).hasSize(1);
    }

    @Test
    public void getCategoryProductsPaginationPageOutOfBounds() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = restTemplate.exchange(categoryProductsBaseUrl + "?page=999&size=20", HttpMethod.GET,new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("number")).isEqualTo(999);
        assertThat(page.get("totalElements")).isEqualTo(1);
        assertThat(page.get("size")).isEqualTo(20);
    }

    @Test
    public void getCategoryProductsPaginationNegativePageIndex() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = restTemplate.exchange(categoryProductsBaseUrl + "?page=-1&size=20", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("totalElements")).isEqualTo(1);
        assertThat(page.get("size")).isEqualTo(20);
    }

    @Test
    public void getCategoryProductsPaginationNegativePageSize() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = restTemplate.exchange(categoryProductsBaseUrl + "?page=0&size=-1", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("totalElements")).isEqualTo(1);
        assertThat(page.get("size")).isEqualTo(20);
    }

    @Test
    public void getCategoryProductsPaginationExceedMaxPageSize() {
        ResponseEntity<Map> response = restTemplate.exchange(categoryProductsBaseUrl + "?page=0&size=5000", HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map page = (Map) response.getBody().get("page");
        assertThat(page.get("size")).isEqualTo(2000);
    }
}

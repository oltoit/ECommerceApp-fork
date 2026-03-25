package com.github.damiox.ecommerce.api.controller.security;

import com.github.damiox.ecommerce.api.controller.IntegrationTestBase;
import com.github.damiox.ecommerce.api.controller.objects.ProductDto;
import com.github.damiox.ecommerce.api.controller.utils.ProductUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;

import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductControllerSecurityTest extends IntegrationTestBase {
    private String baseUrl;

    @Autowired
    private ProductUtils productUtils;

    @Before
    public void init() {
        baseUrl = productsUrl();
    }


    // options
    @Test
    public void productsOptions() {
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.OPTIONS, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void productOptions() {
        // create object
        long id = productUtils.createProduct(defaultProduct, user1.id);
        String productUrl = productUrl((int) id);

        // get object
        ResponseEntity<String> response = restTemplate.exchange(productUrl, HttpMethod.OPTIONS, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Get all
    @Test
    public void getProductsNotLoggedIn() {
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.GET, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }


    // Get specific
    @Test
    public void getProductNotLoggedIn() {
        // create object
        long id = productUtils.createProduct(defaultProduct, user1.id);
        String productUrl = productUrl((int) id);

        // get object
        ResponseEntity<String> response = restTemplate.exchange(productUrl, HttpMethod.GET, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Create product
    @Test
    public void createProductNotLoggedIn() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> productEntity = createDefaultProduct();
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Update product
    @Test
    public void updateProductNotLoggedIn() throws SQLException{
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // update second product on first products path
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String productUrl = productUrl((int) id);
        ResponseEntity<Map> productEntity = updateDefaultProduct(headers, productUrl);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // get product
        ProductDto productFromDb = productUtils.getProduct(id);
        assertThat(defaultProduct).isEqualTo(productFromDb);
    }

    @Test
    public void updateProductWrongUser() throws SQLException {
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // update second product on first products path
        HttpHeaders headers = loginWithHeaders(user2);
        String productUrl = productUrl((int) id);
        ResponseEntity<Map> productEntity = updateDefaultProduct(headers, productUrl);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // get product
        ProductDto productFromDb = productUtils.getProduct(id);
        assertThat(defaultProduct).isEqualTo(productFromDb);
    }

    @Test
    public void updateProductAsAdmin() throws SQLException{
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // update second product on first products path
        HttpHeaders headers = loginWithHeaders(admin);
        String productUrl = productUrl((int) id);
        ResponseEntity<Map> productEntity = updateDefaultProduct(headers, productUrl);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // get product
        ProductDto productFromDb = productUtils.getProduct(id);
        assertThat(defaultProduct).isNotEqualTo(productFromDb);
    }

    // Delete product
    @Test
    public void deleteProductNotLoggedIn() throws SQLException{
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // create second product on first products path
        String productUrl = productUrl((int) id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> productEntity = deleteProduct(headers, productUrl);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // get product
        ProductDto product = productUtils.getProduct(id);
        assertThat(product).isEqualTo(defaultProduct);
    }

    @Test
    public void deleteProductWrongUser() throws SQLException{
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // create second product on first products path
        String productUrl = productUrl((int) id);
        HttpHeaders headers = loginWithHeaders(user2);
        ResponseEntity<Map> productEntity = deleteProduct(headers, productUrl);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // get product
        ProductDto product = productUtils.getProduct(id);
        assertThat(product).isEqualTo(defaultProduct);
    }

    @Test
    public void deleteProductAsAdmin() {
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // create second product on first products path
        String productUrl = productUrl((int) id);
        HttpHeaders headers = loginWithHeaders(admin);
        ResponseEntity<Map> product2Entity = deleteProduct(headers, productUrl);
        assertThat(product2Entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // assert that no object is found
        assertThatThrownBy(() -> productUtils.getProduct(id)).isInstanceOf(EmptyResultDataAccessException.class);
    }

    // Private helper functions
    private ResponseEntity<Map> createDefaultProduct() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(baseUrl, HttpMethod.POST, new HttpEntity<>(productsUrl(), headers), Map.class);
    }

    private ResponseEntity<Map> updateDefaultProduct(HttpHeaders headers, String url) {
        ProductDto product = new ProductDto("updated", "EUR", 12.00);
        HttpEntity httpEntity = new HttpEntity(product, headers);
        return restTemplate.exchange(url, HttpMethod.PUT, httpEntity, Map.class);
    }

    private ResponseEntity<Map> deleteProduct(HttpHeaders headers, String url) {
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
    }
}

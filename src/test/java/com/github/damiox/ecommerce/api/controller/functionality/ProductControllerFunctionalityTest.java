package com.github.damiox.ecommerce.api.controller.functionality;

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
public class ProductControllerFunctionalityTest extends IntegrationTestBase {
    private String baseUrl;

    @Autowired
    private ProductUtils productUtils;

    @Before
    public void init() {
        baseUrl = productsUrl();
    }

    // Get all
    @Test
    public void getProducts() {
        HttpHeaders headers = loginWithHeaders(user1);
        HttpEntity entity = new HttpEntity(headers);

        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Get specific
    @Test
    public void getProductNotFound() {
        HttpHeaders headers = loginWithHeaders(user1);
        HttpEntity entity = new HttpEntity(headers);

        String product1Url = productUrl(1);

        ResponseEntity<String> response = restTemplate.exchange(product1Url, HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getProduct() {
        // create object
        long id = productUtils.createProduct(defaultProduct, user1.id);
        String productUrl = baseUrl + "/" + id;

        // get object
        ResponseEntity<String> response = restTemplate.exchange(productUrl, HttpMethod.GET, new HttpEntity<>(loginWithHeaders(user1)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Create product
    @Test
    public void createProduct() throws SQLException {
        // create object
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl, HttpMethod.POST,new HttpEntity<>(defaultProduct, loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = productUtils.getId(response);

        // check that object exists
        ProductDto product = productUtils.getProduct(id);
        assertThat(product).isNotNull();
        assertThat(product).isEqualTo(defaultProduct);
    }

    @Test
    public void createProductInUsd() throws SQLException{
        ProductDto product = new ProductDto("usd-product", "USD", 10.00);

        // create product
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl, HttpMethod.POST, new HttpEntity<>(product, loginWithHeaders(user1)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // get product
        ProductDto productFromDb = productUtils.getProduct(productUtils.getId(response));

        product.setPrice(Math.round(product.getPrice() * (1.0 / 1.146427) * 100.0) / 100.0);
        product.setCurrency("EUR");

        assertThat(product).isEqualTo(productFromDb);
    }

    @Test
    public void createProductWrongCurrency() {
        HttpHeaders headers = loginWithHeaders(user1);
        ProductDto product = new ProductDto("wrong-currency-product", "BITCOIN", 10000.0);

        ResponseEntity<Map> productEntity = createProduct(headers, product);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void createProductAlreadyAvailable() {
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // create second product on first products path
        ResponseEntity<Map> productEntity = createProduct(productUrl((int) id), loginWithHeaders(user1), defaultProduct);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Update product
    @Test
    public void updateProduct() throws SQLException {
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // update second product on first products path
        HttpHeaders headers = loginWithHeaders(user1);
        String productUrl = productUrl((int) id);
        ResponseEntity<Map> productEntity = updateDefaultProduct(headers, productUrl);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // get product
        ProductDto productFromDb = productUtils.getProduct(id);
        assertThat(defaultProduct).isNotEqualTo(productFromDb);
    }

    @Test
    public void updateProductDifferentCurrency() throws SQLException {
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // update second product on first products path
        HttpHeaders headers = loginWithHeaders(user1);
        String productUrl = productUrl((int) id);
        ProductDto productDifferentCurrency = new ProductDto("different-currency", "USD", 10000.0);
        ResponseEntity<Map> productEntity = updateProduct(headers, productDifferentCurrency, productUrl);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // get product
        ProductDto productFromDb = productUtils.getProduct(id);
        assertThat(defaultProduct).isNotEqualTo(productFromDb);
    }

    @Test
    public void updateProductNotFound() throws SQLException{
        // create second product on first products path
        long id = 1;
        HttpHeaders headers = loginWithHeaders(user1);
        ResponseEntity<Map> productEntity = updateDefaultProduct(headers, productUrl((int) 1));
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // assert that no object is found
        assertThatThrownBy(() -> productUtils.getProduct(id)).isInstanceOf(EmptyResultDataAccessException.class);
    }

    @Test
    public void updateProductWrongCurrency() throws SQLException {
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // create second product on first products path
        String productUrl = productUrl((int) id);
        HttpHeaders headers = loginWithHeaders(user1);
        ProductDto productWrongCurrency = new ProductDto("wrong-currency-product", "BITCOIN", 10000.0);
        ResponseEntity<Map> productEntity = updateProduct(headers, productWrongCurrency, productUrl);
        assertThat(productEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // get product
        ProductDto product = productUtils.getProduct((int) id);
        assertThat(product).isEqualTo(defaultProduct);
    }

    @Test
    public void deleteProduct() {
        // create first product
        long id = productUtils.createProduct(defaultProduct, user1.id);

        // create second product on first products path
        String productUrl = productUrl((int) id);
        HttpHeaders headers = loginWithHeaders(user1);
        ResponseEntity<Map> product2Entity = deleteProduct(headers, productUrl);
        assertThat(product2Entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // assert that no object is found
        assertThatThrownBy(() -> productUtils.getProduct(id)).isInstanceOf(EmptyResultDataAccessException.class);
    }

    @Test
    public void deleteProductNonExistent() {
        long id = 1;

        HttpHeaders headers = loginWithHeaders(user1);
        String productUrl = productUrl((int) id);
        ResponseEntity<Map> product2Entity = deleteProduct(headers, productUrl);
        assertThat(product2Entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // assert that no object is found
        assertThatThrownBy(() -> productUtils.getProduct(id)).isInstanceOf(EmptyResultDataAccessException.class);
    }


    // Private helper functions
    private ResponseEntity<Map> createProduct(HttpHeaders headers, ProductDto product) {
        return createProduct(baseUrl, headers, product);
    }

    private ResponseEntity<Map> createProduct(String url, HttpHeaders headers, ProductDto product) {
        HttpEntity httpEntity = new HttpEntity(product, headers);
        return restTemplate.exchange(url, HttpMethod.POST, httpEntity, Map.class);
    }

    private ResponseEntity<Map> updateDefaultProduct(HttpHeaders headers, String url) {
        ProductDto product = new ProductDto("updated", "EUR", 12.00);
        return updateProduct(headers, product, url);
    }

    private ResponseEntity<Map> updateProduct(HttpHeaders headers, ProductDto product, String url) {
        HttpEntity httpEntity = new HttpEntity(product, headers);
        return restTemplate.exchange(url, HttpMethod.PUT, httpEntity, Map.class);
    }

    private ResponseEntity<Map> deleteProduct(HttpHeaders headers, String url) {
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
    }
}

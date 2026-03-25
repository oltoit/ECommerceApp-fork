package com.github.damiox.ecommerce.api.controller.functionality;

import com.github.damiox.ecommerce.api.controller.IntegrationTestBase;
import com.github.damiox.ecommerce.api.controller.objects.CategoryDto;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/*
Tests for the purposes of testing whether every endpoint sends a correct hateoas response.
Only Endpoints are tested that actually return a body.
*/

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HateoasFunctionalityTest extends IntegrationTestBase {

    private String productsBaseUrl;
    private String categoriesBaseUrl;

    @Autowired
    private ProductUtils productUtils;
    @Autowired
    private CategoryUtils categoryUtils;
    @Autowired
    private ProductCategoryUtils productCategoryUtils;

    @Before
    public void init() {
        productsBaseUrl = productsUrl();
        categoriesBaseUrl = categoriesUrl();
    }

    // ProductController Tests
    @Test
    public void getProducts() {
        long id = productUtils.createProduct(defaultProduct, user1.id);
        String url = productUrl((int) id);

        ResponseEntity<Map> response = restTemplate.exchange(
                productsBaseUrl, HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("_links", "_embedded");
        assertThat((Map) response.getBody().get("_links")).containsKey("self");
    }

    @Test
    public void getProduct() {
        long id = productUtils.createProduct(defaultProduct, user1.id);
        String url = productUrl((int) id);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map links = (Map) response.getBody().get("_links");
        assertThat((String) ((Map) links.get("self")).get("href")).isEqualTo(url);
    }

    @Test
    public void createProduct() {
        ResponseEntity<Map> response = restTemplate.exchange(
                productsBaseUrl, HttpMethod.POST,
                new HttpEntity<>(defaultProduct, loginWithHeaders(user1)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map links = (Map) response.getBody().get("_links");
        assertThat(links).containsKey("self");

        String selfHref = (String) ((Map) links.get("self")).get("href");
        assertThat(selfHref).contains("/products/");

        // Test that href is valid
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                selfHref, HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), Map.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void updateProduct() {
        long id = productUtils.createProduct(defaultProduct, user1.id);

        ResponseEntity<Map> response = restTemplate.exchange(
                productsBaseUrl + "/" + id, HttpMethod.PUT,
                new HttpEntity<>(new ProductDto("updated", "EUR", 12.00), loginWithHeaders(user1)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map links = (Map) response.getBody().get("_links");
        String selfHref = (String) ((Map) links.get("self")).get("href");
        assertThat(links).containsKey("self");

        // Test that href is valid
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                selfHref, HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), Map.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    // CategoryController Tests
    @Test
    public void getCategories() {
        categoryUtils.createCategory("test-category");

        // Using List here turns _link into link for some reason
        ResponseEntity<List> response = restTemplate.exchange(
                categoriesBaseUrl, HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), List.class
        );

        Map<String, Object> map = (Map) response.getBody().get(0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(map).containsKeys("links");
        assertThat((List) map.get("links")).isNotEmpty();
    }

    @Test
    public void getCategory() {
        long id = categoryUtils.createCategory("test-category");
        String url = categoryUrl((int) id);

        ResponseEntity<Map> response = restTemplate.exchange(
                url , HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map links = (Map) response.getBody().get("_links");
        assertThat((String) ((Map) links.get("self")).get("href")).isEqualTo(url);

    }

    @Test
    public void createCategory() {
        ResponseEntity<Map> response = restTemplate.exchange(
                categoriesBaseUrl, HttpMethod.POST,
                new HttpEntity<>(new CategoryDto("new-category"), loginWithHeaders(admin)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map links = (Map) response.getBody().get("_links");
        assertThat(links).containsKey("self");

        String selfHref = (String) ((Map) links.get("self")).get("href");
        assertThat(selfHref).contains("/categories/");

        // Test that href is valid
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                selfHref, HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), Map.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void updateCategory() {
        long id = categoryUtils.createCategory("original-category");
        String url = categoryUrl((int) id);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.PUT,
                new HttpEntity<>(new CategoryDto("updated-category"), loginWithHeaders(admin)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map links = (Map) response.getBody().get("_links");
        String selfHref = (String) ((Map) links.get("self")).get("href");
        assertThat(selfHref).isEqualTo(url);
    }


    // CategorySubcategoriesController Tests
    @Test
    public void getSubcategories() {
        long parentId = categoryUtils.createCategory("parent");
        long childId = categoryUtils.createSubcategory("child", parentId);

        ResponseEntity<List> response = restTemplate.exchange(
                subcategoriesUrl(parentId), HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();

        Map firstItem = (Map) response.getBody().get(0);
        assertThat(firstItem).containsKey("links");
        assertThat((List) firstItem.get("links")).isNotEmpty();

        // check that href is valid
        String subcategoryHref = ((List) firstItem.get("links")).stream().filter(e -> ((Map) e).get("rel").equals("self")).map(e -> ((Map) e).get("href")).findFirst().get().toString();
        assertThat(subcategoryHref).isEqualTo(categoryUrl((int) childId));
    }

    @Test
    public void addSubcategory() {
        long parentId = categoryUtils.createCategory("parent");
        long childId = categoryUtils.createCategory("child");

        ResponseEntity<Map> response = restTemplate.exchange(
                subcategoryUrl(parentId, childId), HttpMethod.POST,
                new HttpEntity<>(loginWithHeaders(admin)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LinkedHashMap links = (LinkedHashMap) response.getBody().get("_links");
        assertThat(links).isNotEmpty();

        assertThat(links.size()).isEqualTo(2);
        assertThat(links.get("self")).isNotNull();
        assertThat(links.get("subcategories")).isNotNull();

        // test that both hrefs are valid
        String selfLink = ((Map) links.get("self")).get("href").toString();
        String subcategoryLink = ((Map) links.get("subcategories")).get("href").toString();

        assertThat(selfLink).isEqualTo(categoryUrl((int) parentId));
        assertThat(subcategoryLink).isEqualTo(subcategoriesUrl(parentId));
    }


    // CategoryProductsController Tests
    @Test
    public void getCategoryProducts() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        long categoryId = categoryUtils.createCategory("test-category");
        productCategoryUtils.addProductToCategory(productId, categoryId);

        ResponseEntity<Map> response = restTemplate.exchange(
                categoryProductsUrl(categoryId), HttpMethod.GET,
                new HttpEntity<>(loginWithHeaders(user1)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map firstItem = response.getBody();
        assertThat(firstItem).containsKey("_links");
        assertThat(firstItem.get("_links")).isNotNull();
    }

    @Test
    public void addCategoryProduct() {
        long productId = productUtils.createProduct(defaultProduct, user1.id);
        long categoryId = categoryUtils.createCategory("test-category");
        String url = productUrl((int) productId);

        ResponseEntity<Map> response = restTemplate.exchange(
                categoryProductUrl(categoryId, productId), HttpMethod.POST,
                new HttpEntity<>(loginWithHeaders(user1)), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LinkedHashMap links = (LinkedHashMap) response.getBody().get("_links");
        assertThat(links).isNotEmpty();
        assertThat((String) ((Map) links.get("self")).get("href")).isEqualTo(url);
    }
}

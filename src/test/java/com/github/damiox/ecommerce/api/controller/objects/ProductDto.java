package com.github.damiox.ecommerce.api.controller.objects;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ProductDto {
    @NotNull(message = "name is required")
    @Size(message = "name must be equal to or lower than 300", min = 1, max = 300)
    private String name;
    @NotNull
    @Size(message = "Currency must be in ISO 4217 format", min = 3, max = 3)
    private String currency;
    @NotNull(message = "name is required")
    @Min(0)
    private Double price;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public ProductDto(String name, String currency, double price) {
        this.name = name;
        this.currency = currency;
        this.price = price;
    }
}

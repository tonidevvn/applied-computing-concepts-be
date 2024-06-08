package com.scraper.api.controller;

import com.scraper.api.model.ProductData;
import com.scraper.api.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping(path = "/api")
public class ScraperController {
    @Autowired
    private ProductService productService;

    @GetMapping
    @ResponseBody
    public String index() {
        return "<!DOCTYPE html><html lang=\"en\">"
                + "<head><title>Selenium / Spring Boot demo</title></head>"
                + "<body><h1 id=\"h1-hello\">Hello world!</h1></body></html>";
    }

    @GetMapping(path = "/products/search")
    public Set<ProductData> getProductsByKeyword(@RequestParam("q") String searchKeyword) {
        return productService.getProductsByKeyword(searchKeyword);
    }

    @GetMapping(path = "/products/")
    public Set<ProductData> getProducts() {
        return productService.getProducts();
    }
}

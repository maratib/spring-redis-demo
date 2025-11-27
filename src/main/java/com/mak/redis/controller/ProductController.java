package com.mak.redis.controller;

import org.springframework.web.bind.annotation.*;
import com.mak.redis.service.ProductService;
import com.mak.redis.model.Product;
import java.util.*;

@RestController
@RequestMapping("/products")
public class ProductController {
  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @GetMapping("/{id}")
  public Product get(@PathVariable Long id) {
    return service.getProduct(id);
  }

  @PostMapping
  public Product create(@RequestBody Product p) {
    return service.createProduct(p);
  }

  @PutMapping("/{id}")
  public Product update(@PathVariable Long id, @RequestBody Product p) {
    p.setId(id);
    return service.updateProduct(p);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.deleteProduct(id);
  }
}
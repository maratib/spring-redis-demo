package com.mak.redis.service;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.*;
import com.mak.redis.model.Product;
import java.util.*;

@Service
public class ProductService {

  private final Map<Long, Product> db = new HashMap<>();

  @Cacheable(value = "product", key = "#id")
  public Product getProduct(Long id) {
    return db.get(id);
  }

  public Product createProduct(Product p) {
    db.put(p.getId(), p);
    return p;
  }

  @CachePut(value = "product", key = "#p.id")
  public Product updateProduct(Product p) {
    db.put(p.getId(), p);
    return p;
  }

  @CacheEvict(value = "product", key = "#id")
  public void deleteProduct(Long id) {
    db.remove(id);
  }
}
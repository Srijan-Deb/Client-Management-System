package com.cms.billing.service;

import com.cms.billing.domain.entity.Product;
import com.cms.billing.repository.ProductRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products")
    public List<Product> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }
}

package com.cms.client;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

public class MapperScanTest {
    @Test
    public void test() throws Exception {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        for (org.springframework.beans.factory.config.BeanDefinition bd : provider.findCandidateComponents("com.cms.client.mapper")) {
            System.out.println("FOUND BEAN: " + bd.getBeanClassName());
        }
    }
}

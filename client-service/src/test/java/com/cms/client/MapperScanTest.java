package com.cms.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperScanTest {

    @Test
    void test() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        Set<BeanDefinition> candidates = provider.findCandidateComponents("com.cms.client.mapper");

        for (BeanDefinition bd : candidates) {
            System.out.println("FOUND BEAN: " + bd.getBeanClassName());
        }

        // Assert that the mapper package is not empty
        assertFalse(candidates.isEmpty(), "No mapper beans found in com.cms.client.mapper package");

        // Assert that exactly 2 mappers are registered (ClientMapper and
        // SupportTicketMapper)
        assertEquals(2, candidates.size(),
                "Expected 2 mapper beans (ClientMapper, SupportTicketMapper) but found: " + candidates.size());
    }
}

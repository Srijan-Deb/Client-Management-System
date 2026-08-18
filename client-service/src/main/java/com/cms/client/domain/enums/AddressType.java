package com.cms.client.domain.enums;

/**
 * Classification of an address record attached to a client.
 * Stored as a MySQL ENUM('BILLING','SHIPPING','PRIMARY').
 */
public enum AddressType {
    BILLING,
    SHIPPING,
    PRIMARY
}

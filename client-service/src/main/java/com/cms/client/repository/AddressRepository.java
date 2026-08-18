package com.cms.client.repository;

import com.cms.client.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByClientClientId(Long clientId);

    boolean existsByAddressIdAndClientClientId(Long addressId, Long clientId);
}

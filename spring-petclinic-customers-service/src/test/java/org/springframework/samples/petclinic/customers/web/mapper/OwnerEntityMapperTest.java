package org.springframework.samples.petclinic.customers.web.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.web.OwnerRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OwnerEntityMapperTest {

    private final OwnerEntityMapper mapper = new OwnerEntityMapper();

    @Test
    void testMap() {
        // Arrange
        Owner owner = new Owner();
        OwnerRequest request = new OwnerRequest("First", "Last", "Addr", "City", "1234567890");

        // Act
        Owner result = mapper.map(owner, request);

        // Assert
        assertEquals("First", result.getFirstName());
        assertEquals("Last", result.getLastName());
        assertEquals("Addr", result.getAddress());
        assertEquals("City", result.getCity());
        assertEquals("1234567890", result.getTelephone());
    }
}
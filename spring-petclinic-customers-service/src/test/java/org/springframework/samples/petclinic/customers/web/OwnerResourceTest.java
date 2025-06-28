package org.springframework.samples.petclinic.customers.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.web.mapper.OwnerEntityMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OwnerResource.class)
class OwnerResourceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OwnerRepository ownerRepository;

    @MockBean
    private OwnerEntityMapper ownerEntityMapper;

    @Test
    void testCreateOwner() throws Exception {
        // Arrange
        OwnerRequest request = new OwnerRequest("Jane", "Doe", "123 Street", "City", "1234567890");
        Owner owner = new Owner();
        owner.setFirstName("Jane");
        owner.setLastName("Doe");
        owner.setAddress("123 Street");
        owner.setCity("City");
        owner.setTelephone("1234567890");
        ReflectionTestUtils.setField(owner, "id", 10);

        given(ownerEntityMapper.map(any(Owner.class), any(OwnerRequest.class))).willReturn(owner);
        given(ownerRepository.save(any(Owner.class))).willReturn(owner);

        // Act & Assert
        mvc.perform(post("/owners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.firstName").value("Jane"))
            .andExpect(jsonPath("$.address").value("123 Street"));
    }

    @Test
    void testFindOwner() throws Exception {
        // Arrange
        Owner owner = new Owner();
        ReflectionTestUtils.setField(owner, "id", 5);
        owner.setFirstName("Alice");
        owner.setLastName("Smith");
        owner.setAddress("456 Avenue");
        owner.setCity("Town");
        owner.setTelephone("0987654321");

        given(ownerRepository.findById(5)).willReturn(Optional.of(owner));

        // Act & Assert
        mvc.perform(get("/owners/5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void testFindAllOwners() throws Exception {
        // Arrange
        Owner owner1 = new Owner();
        ReflectionTestUtils.setField(owner1, "id", 1);
        owner1.setFirstName("A");
        owner1.setLastName("B");
        
        Owner owner2 = new Owner();
        ReflectionTestUtils.setField(owner2, "id", 2);
        owner2.setFirstName("X");
        owner2.setLastName("Y");

        given(ownerRepository.findAll()).willReturn(Arrays.asList(owner1, owner2));

        // Act & Assert
        mvc.perform(get("/owners")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void testUpdateOwnerSuccessfully() throws Exception {
        // Arrange
        Owner existingOwner = new Owner();
        ReflectionTestUtils.setField(existingOwner, "id", 7);
        existingOwner.setFirstName("Old");
        existingOwner.setLastName("Name");
        existingOwner.setAddress("Old Address");
        existingOwner.setCity("Old City");
        existingOwner.setTelephone("000");

        OwnerRequest request = new OwnerRequest("New", "Name", "New Address", "New City", "1112223333");

        given(ownerRepository.findById(7)).willReturn(Optional.of(existingOwner));
        given(ownerEntityMapper.map(existingOwner, request)).willReturn(existingOwner);
        given(ownerRepository.save(existingOwner)).willReturn(existingOwner);

        // Act & Assert
        mvc.perform(put("/owners/7")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());
    }

    @Test
    void testUpdateOwnerNotFound() throws Exception {
        // Arrange
        OwnerRequest request = new OwnerRequest("New", "Name", "New Address", "New City", "1112223333");
        given(ownerRepository.findById(999)).willReturn(Optional.empty());

        // Act & Assert
        mvc.perform(put("/owners/999")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }
}
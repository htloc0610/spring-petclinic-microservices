package org.springframework.samples.petclinic.customers.web;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.model.Pet;
import org.springframework.samples.petclinic.customers.model.PetRepository;
import org.springframework.samples.petclinic.customers.model.PetType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Maciej Szarlinski
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(PetResource.class)
@ActiveProfiles("test")
class PetResourceTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PetRepository petRepository;

    @MockBean
    OwnerRepository ownerRepository;

    @Test
    void shouldGetAPetInJSonFormat() throws Exception {

        Pet pet = setupPet();

        given(petRepository.findById(2)).willReturn(Optional.of(pet));


        mvc.perform(get("/owners/2/pets/2").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.name").value("Basil"))
            .andExpect(jsonPath("$.type.id").value(6));
    }

    @Test
    void shouldReturnNotFoundForNonExistingPet() throws Exception {
        given(petRepository.findById(999)).willReturn(Optional.empty());

        mvc.perform(get("/owners/1/pets/999").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllPetTypes() throws Exception {
        List<PetType> petTypes = Arrays.asList(
            createPetType(1, "cat"),
            createPetType(2, "dog"),
            createPetType(3, "lizard")
        );

        given(petRepository.findPetTypes()).willReturn(petTypes);

        mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("cat"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("dog"))
            .andExpect(jsonPath("$[2].id").value(3))
            .andExpect(jsonPath("$[2].name").value("lizard"));
    }

    @Test
    void shouldReturnNotFoundWhenCreatingPetWithUnknownOwner() throws Exception {
        given(ownerRepository.findById(999)).willReturn(Optional.empty());

        mvc.perform(post("/owners/999/pets")
            .content("{\"name\": \"Fluffy\", \"birthDate\": \"2021-01-01\", \"typeId\": 2}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreatePetSuccessfully() throws Exception {
        // Arrange
        int ownerId = 1;
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");

        PetType petType = createPetType(1, "dog");

        // When finding the owner
        given(ownerRepository.findById(ownerId)).willReturn(Optional.of(owner));
        // When looking up pet type
        given(petRepository.findPetTypeById(1)).willReturn(Optional.of(petType));
        // When saving pet, simulate assigned id = 100
        willAnswer(invocation -> {
            Pet petToSave = invocation.getArgument(0);
            petToSave.setId(100);
            return petToSave;
        }).given(petRepository).save(any(Pet.class));

        // Act & Assert
        mvc.perform(post("/owners/1/pets")
            .content("{\"name\": \"Buddy\", \"birthDate\": \"2022-04-01\", \"typeId\": 1}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.name").value("Buddy"));
    }

    @Test
    void shouldUpdatePetSuccessfully() throws Exception {
        // Arrange
        int petId = 2;
        Pet pet = new Pet();
        pet.setId(petId);
        pet.setName("OldName");

        PetType newType = createPetType(3, "lizard");

        // Simulate pet found for update
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));
        // Simulate pet type found for update
        given(petRepository.findPetTypeById(3)).willReturn(Optional.of(newType));
        // Simulate saving pet (no changes to id)
        willAnswer(invocation -> invocation.getArgument(0))
            .given(petRepository).save(any(Pet.class));

        // Act & Assert
        mvc.perform(put("/owners/1/pets/2")
            .content("{\"id\": 2, \"name\": \"UpdatedBuddy\", \"birthDate\": \"2020-03-15\", \"typeId\": 3}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Optionally, verify that the pet fields were updated
        // (if you add logging or further testing in your controller/service)
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistingPet() throws Exception {
        given(petRepository.findById(999)).willReturn(Optional.empty());

        mvc.perform(put("/owners/1/pets/999")
            .content("{\"id\": 999, \"name\": \"NewName\", \"birthDate\": \"2020-03-15\", \"typeId\": 3}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    private PetType createPetType(int id, String name) {
        PetType petType = new PetType();
        petType.setId(id);
        petType.setName(name);
        return petType;
    }

    private Pet setupPet() {
        Owner owner = new Owner();
        owner.setFirstName("George");
        owner.setLastName("Bush");

        Pet pet = new Pet();

        pet.setName("Basil");
        pet.setId(2);

        PetType petType = new PetType();
        petType.setId(6);
        pet.setType(petType);

        owner.addPet(pet);
        return pet;
    }
}

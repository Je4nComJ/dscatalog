package com.devsuperior.dscatalog.resources;

import com.devsuperior.dscatalog.dtos.ProductDTO;
import com.devsuperior.dscatalog.services.ProductService;
import com.devsuperior.dscatalog.services.exceptions.DatabaseException;
import com.devsuperior.dscatalog.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dscatalog.tests.Factory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductResource.class)
public class ProductResourceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService service;

    private ProductDTO productDTO;
    private PageImpl<ProductDTO> page;
    private Long existingId;
    private Long nonExistingId;
    private Long dependentId;

    @BeforeEach
    void setUp() {
        productDTO = Factory.createProductDTO();
        page = new PageImpl<>(List.of(productDTO));
        existingId = 1L;
        nonExistingId = 2L;
        dependentId = 3L;

        Mockito.when(service.findAllPaged(ArgumentMatchers.any())).thenReturn(page);

        Mockito.when(service.findById(existingId)).thenReturn(productDTO);
        Mockito.when(service.findById(nonExistingId)).thenThrow(ResourceNotFoundException.class);

        Mockito.when(service.update(eq(existingId), ArgumentMatchers.any())).thenReturn(productDTO);
        Mockito.when(service.update(eq(nonExistingId), ArgumentMatchers.any())).thenThrow(ResourceNotFoundException.class);

        Mockito.doNothing().when(service).delete(existingId);
        Mockito.doThrow(ResourceNotFoundException.class).when(service).delete(nonExistingId);
        Mockito.doThrow(DatabaseException.class).when(service).delete(dependentId);

        Mockito.when(service.insert(ArgumentMatchers.any())).thenReturn(productDTO);
    }

    @Test
    public void findAllShouldReturnPage() throws Exception{
        mockMvc.perform(get("/products").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void findByIdShouldReturnProductWhenIdExists() throws Exception {
        mockMvc.perform(get("/products/{id}",existingId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    public void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception{
        mockMvc.perform(get("/products/{id}",nonExistingId)).andExpect(status().isNotFound());
    }

    @Test
    public void updateShouldReturnProductDTOWhenIdExists() throws Exception{
        String jsonBody = objectMapper.writeValueAsString(productDTO);
        mockMvc.perform(put("/products/{id}", existingId)
                .content(jsonBody).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    public void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception{
        String jsonBody = objectMapper.writeValueAsString(productDTO);
        mockMvc.perform(put("/products/{id}", nonExistingId)
                        .content(jsonBody).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void insertShouldReturnCreatedAndProductDTO() throws Exception {
        String jsonBody = objectMapper.writeValueAsString(productDTO);
        mockMvc.perform(post("/products")
                .content(jsonBody).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    public void deleteShouldReturnNoContentWhenIdExists() throws Exception{
        mockMvc.perform(delete("/products/{id}", existingId))
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception{
        mockMvc.perform(delete("/products/{id}", nonExistingId))
                .andExpect(status().isNotFound());
    }
}

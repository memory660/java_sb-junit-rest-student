package com.khanivorous.studentservice.controllertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanivorous.studentservice.student.NoSuchIdException;
import com.khanivorous.studentservice.student.controllers.StudentController;
import com.khanivorous.studentservice.student.mapper.StudentMapper;
import com.khanivorous.studentservice.student.model.StudentCreationDTO;
import com.khanivorous.studentservice.student.model.StudentDTO;
import com.khanivorous.studentservice.student.services.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
public class StudentControllerWithServiceMockTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentMapper studentMapper;

    @MockBean
    private StudentService studentService;

    @Test
    public void testGetAllUsers() throws Exception {

        StudentDTO student1 = new StudentDTO(1, "Ben", 28);

        List<StudentDTO> studentList = new ArrayList<>();
        studentList.add(student1);

        when(studentService.getAllStudents()).thenReturn(studentList);

        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Ben")))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].age", is(28)));
    }

    @Test
    public void testGetUserById() throws Exception {

        StudentDTO student1 = new StudentDTO(1, "Ben", 28);

        when(studentService.getStudentById(1)).thenReturn(student1);

        mockMvc.perform(get("/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Ben")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.age", is(28)));
    }

    @Test
    public void testUnknownIdReturnsError() throws Exception {
        when(studentService.getStudentById(2)).thenThrow(new NoSuchIdException(2));
        mockMvc.perform(get("/students/2"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Could not find student with id 2"));
    }

    @Test
    public void testAddNewStudent() throws Exception {

        StudentDTO student = new StudentDTO(1, "Andy", 22);

        when(studentService.addNewStudent(anyString(), anyInt())).thenReturn(student);

        ObjectMapper mapper = new ObjectMapper();
        StudentCreationDTO studentDTO = new StudentCreationDTO("Andy", 22);
        String requestBody = mapper.writeValueAsString(studentDTO);

        mockMvc.perform(post("/students")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Andy")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.age", is(22)));
    }

    @Test
    public void validateEmptyNameInRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        StudentCreationDTO student = new StudentCreationDTO("", 16);
        String requestBody = mapper.writeValueAsString(student);

        mockMvc.perform(post("/students")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name", is("name must not be empty")));
    }

    @Test
    public void validateMinimumAgeInRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        StudentCreationDTO student = new StudentCreationDTO("Andrew", 16);
        String requestBody = mapper.writeValueAsString(student);

        mockMvc.perform(post("/students")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.age", is("age cannot be less than 17 years old")));
    }

    @Test
    public void validateNullAge() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        StudentCreationDTO student = new StudentCreationDTO("Jason", null);
        String requestBody = mapper.writeValueAsString(student);

        mockMvc.perform(post("/students")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.age", is("age must not be null")));
    }


    @Test
    public void testDeleteStudentById() throws Exception {

        mockMvc.perform(delete("/students/1"))
                .andExpect(status().isNoContent());
        verify(studentService, times(1)).deleteStudentById(1);
    }

    @Test
    public void testDeleteNonExistentStudentThrowsError() throws Exception {

        doThrow(new NoSuchIdException(1)).when(studentService).deleteStudentById(1);
        mockMvc.perform(delete("/students/1"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NoSuchIdException));
    }

}

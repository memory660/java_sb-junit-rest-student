package com.khanivorous.studentservice.servicetests;

import com.khanivorous.studentservice.student.NoSuchIdException;
import com.khanivorous.studentservice.student.entities.Student;
import com.khanivorous.studentservice.student.mapper.StudentMapper;
import com.khanivorous.studentservice.student.model.StudentDTO;
import com.khanivorous.studentservice.student.repository.StudentRepository;
import com.khanivorous.studentservice.student.services.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Spy
    private StudentRepository studentRepository;

    private StudentServiceImpl serviceUnderTest;

    @Spy
    private StudentMapper studentMapper;

    @BeforeEach
    public void setUp() {
        this.serviceUnderTest = new StudentServiceImpl(studentRepository,studentMapper);
    }

    @Test
    public void testGetStudentById() {
        Student student1 = new Student();
        student1.setId(1);
        student1.setName("Ben");
        student1.setAge(28);
        when(studentRepository.findById(1)).thenReturn(Optional.of(student1));

        StudentDTO response = serviceUnderTest.getStudentById(1);

        assertEquals(student1.getId(), response.id());
        assertEquals(student1.getName(), response.name());
        assertEquals(student1.getAge(), response.age());

        verify(studentMapper, times(1)).toDTO(any(Student.class));
    }

    @Test
    public void testGetUnknownIdReturnsError() {

        Exception exception = assertThrows(NoSuchIdException.class, () -> serviceUnderTest.getStudentById(1));

        String expectedMessage = "Could not find student with id 1";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testGetAllStudents() {
        Student student1 = new Student();
        student1.setId(1);
        student1.setName("Ben");
        student1.setAge(28);

        ArrayList<Student> studentList = new ArrayList<>();
        studentList.add(student1);

        when(studentRepository.findAll()).thenReturn(studentList);

        List<StudentDTO> response = serviceUnderTest.getAllStudents();
        assertEquals(studentList.get(0).getId(), response.get(0).id());
        assertEquals(studentList.get(0).getName(), response.get(0).name());
        assertEquals(studentList.get(0).getAge(), response.get(0).age());

        verify(studentMapper, times(1)).toDTOList(anyIterable());
    }

    @Test
    public void testAddNewStudent() {

        Student student = new Student();
        student.setId(1);
        student.setName("Andy");
        student.setAge(22);

        when(studentRepository.save(any(Student.class))).thenReturn(student);

        serviceUnderTest.addNewStudent("john", 23);
        verify(studentMapper, times(1)).toDTO(any(Student.class));
    }


    @Test
    public void testDeleteById() {
        when(studentRepository.existsById(1)).thenReturn(true);
        serviceUnderTest.deleteStudentById(1);
        verify(studentRepository, times(1)).deleteById(1);
    }

    @Test
    public void testDeleteByNonExistentIdThrowsError() {
        when(studentRepository.existsById(1)).thenReturn(false);
        Exception exception = assertThrows(NoSuchIdException.class, () -> serviceUnderTest.deleteStudentById(1));
        String expectedMessage = "Could not find student with id 1";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
        verify(studentRepository, never()).delete(any());
    }

}

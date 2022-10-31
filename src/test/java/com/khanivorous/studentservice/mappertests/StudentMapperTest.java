package com.khanivorous.studentservice.mappertests;

import com.khanivorous.studentservice.student.entities.Student;
import com.khanivorous.studentservice.student.mapper.StudentMapper;
import com.khanivorous.studentservice.student.model.StudentDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StudentMapperTest {

    private final StudentMapper mapperUnderTest = new StudentMapper();

    @Test
    void toDto() {

        Student entityToMap = new Student();
        entityToMap.setId(1);
        entityToMap.setName("Tom");
        entityToMap.setAge(22);

        StudentDTO dto = mapperUnderTest.toDTO(entityToMap);

        assertEquals(entityToMap.getId(), dto.id());
        assertEquals(entityToMap.getName(), dto.name());
        assertEquals(entityToMap.getAge(), dto.age());
    }

    @Test
    void toDTOList() {

        Student entityToMap = new Student();
        entityToMap.setId(1);
        entityToMap.setName("Tom");
        entityToMap.setAge(22);

        List<Student> studentList = new ArrayList<>();
        studentList.add(entityToMap);

        Iterable<Student> studentIterable = studentList;

        List<StudentDTO> dtoList = mapperUnderTest.toDTOList(studentIterable);

        assertEquals(studentList.get(0).getId(), dtoList.get(0).id());
        assertEquals(studentList.get(0).getName(), dtoList.get(0).name());
        assertEquals(studentList.get(0).getAge(), dtoList.get(0).age());
    }

    @Test
    void toEntity() {
        StudentDTO dto = new StudentDTO(1, "Bob", 21);
        Student entity = mapperUnderTest.toEntity(dto);
        assertEquals(dto.id(), entity.getId());
        assertEquals(dto.name(), entity.getName());
        assertEquals(dto.age(), entity.getAge());
    }

}

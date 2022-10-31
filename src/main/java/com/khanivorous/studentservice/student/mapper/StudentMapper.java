package com.khanivorous.studentservice.student.mapper;

import com.khanivorous.studentservice.student.entities.Student;
import com.khanivorous.studentservice.student.model.StudentDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StudentMapper {


    public StudentDTO toDTO(Student entity) {
        return new StudentDTO(entity.getId(), entity.getName(), entity.getAge());
    }

    public List<StudentDTO> toDTOList(Iterable<Student> students) {
        List<Student> studentList = new ArrayList<>();
        students.forEach(studentList::add);
        return studentList
                .stream()
                .map(student -> new StudentDTO(student.getId(),student.getName(),student.getAge()))
                .collect(Collectors.toList());
    }

    public Student toEntity(StudentDTO dto) {
        Student entity = new Student();
        entity.setId(dto.id());
        entity.setName(dto.name());
        entity.setAge(dto.age());
        return entity;
    }



}

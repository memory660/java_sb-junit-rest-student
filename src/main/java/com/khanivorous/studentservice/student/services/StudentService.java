package com.khanivorous.studentservice.student.services;

import com.khanivorous.studentservice.student.entities.Student;
import com.khanivorous.studentservice.student.model.StudentDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StudentService {

    StudentDTO getStudentById(int id);

    List<StudentDTO> getAllStudents();

    StudentDTO addNewStudent(String name, int age);

    void deleteStudentById(int id);

}

package com.khanivorous.studentservice.student.services;

import com.khanivorous.studentservice.student.NoSuchIdException;
import com.khanivorous.studentservice.student.entities.Student;
import com.khanivorous.studentservice.student.mapper.StudentMapper;
import com.khanivorous.studentservice.student.model.StudentDTO;
import com.khanivorous.studentservice.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private StudentRepository studentRepository;

    private StudentMapper studentMapper;

    public StudentServiceImpl(StudentRepository studentRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    public StudentDTO getStudentById(int id) {
        Student student = studentRepository.findById(id).orElseThrow(() -> new NoSuchIdException(id));
        return studentMapper.toDTO(student);
    }

    public List<StudentDTO> getAllStudents() {
        return studentMapper.toDTOList(studentRepository.findAll());
    }

    public StudentDTO addNewStudent(String name, int age) {
        Student newStudent = new Student();
        newStudent.setName(name);
        newStudent.setAge(age);
        return  studentMapper.toDTO(studentRepository.save(newStudent));
    }

    public void deleteStudentById(int id) {
        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
        } else {
            throw new NoSuchIdException(id);
        }
    }
}

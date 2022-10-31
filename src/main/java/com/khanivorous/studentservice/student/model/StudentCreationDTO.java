package com.khanivorous.studentservice.student.model;


import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public record StudentCreationDTO(
        @NotEmpty(message = "name must not be empty")
        String name,
        @NotNull(message = "age must not be null")
        @Min(value = 17, message = "age cannot be less than 17 years old")
        Integer age) {
}

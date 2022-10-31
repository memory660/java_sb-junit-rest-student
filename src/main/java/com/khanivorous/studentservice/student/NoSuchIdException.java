package com.khanivorous.studentservice.student;


public class NoSuchIdException extends RuntimeException {

    public NoSuchIdException(long id) {
        super("Could not find student with id " + id);
    }

}

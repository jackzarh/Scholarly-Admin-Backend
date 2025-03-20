package org.niit_project.backend.dto;

import org.springframework.http.HttpStatus;

public class ApiException extends Exception{
    private HttpStatus httpStatus;

    public ApiException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getStatusCode() {
        return httpStatus == null? HttpStatus.BAD_REQUEST: httpStatus;
    }

    public void setStatusCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}

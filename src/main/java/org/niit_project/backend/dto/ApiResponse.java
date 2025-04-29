package org.niit_project.backend.dto;


/**
 * This is a DTO (Data Transfer Object) that will be used
 * throughout the project when returning responses.
 *
 * <p> The {@link  #message} field passes response messages
 * And the {@link #data} field passes the data of the response
 *
 * @author Teninlanimi Taiwo
 */
public class ApiResponse<T> {
    private String message;
    private T data;

    // Empty Constructor
    public ApiResponse(){}

    public ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

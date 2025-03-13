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
public class ApiResponse {
    private String message;
    private Object data;

    // Empty Constructor
    public ApiResponse(){}

    public ApiResponse(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

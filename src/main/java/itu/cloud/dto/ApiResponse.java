package itu.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reponse generique pour les APIs CRUD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private String error;
    private T data;
    private String mode; // ONLINE ou OFFLINE

    public static <T> ApiResponse<T> success(T data, String mode) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .mode(mode)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message, String mode) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .mode(mode)
                .build();
    }

    public static <T> ApiResponse<T> error(String error, String mode) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .mode(mode)
                .build();
    }
}


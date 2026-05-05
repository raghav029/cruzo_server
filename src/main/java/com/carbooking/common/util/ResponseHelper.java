package com.carbooking.common.util;

import com.carbooking.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Controller-layer helper. Eliminates the ResponseEntity + ApiResponse boilerplate
 * from every controller method.
 *
 * Usage:
 * <pre>
 *   return ResponseHelper.ok(service.get(id));
 *   return ResponseHelper.created(service.create(request));
 *   return ResponseHelper.ok("Driver assigned", service.assign(id, req));
 *   return ResponseHelper.noContent();   // for DELETE endpoints
 * </pre>
 */
public final class ResponseHelper {

    private ResponseHelper() {}

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data));
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}

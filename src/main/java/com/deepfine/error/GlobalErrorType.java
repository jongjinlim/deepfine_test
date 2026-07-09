package com.deepfine.error;

import org.springframework.http.HttpStatus;

public interface GlobalErrorType {
    HttpStatus getStatus();

    String getMessage();

}

package com.deepfine.error.response;


/**
 * API 호출을 통한 모든 응답갑의 형식을 정의
 * @param <D>
 */
public record GlobalResponse<D>(D data, GlobalErrorResponse error) {

    public static GlobalResponse<?> success() {
        return new GlobalResponse<>(null, null);
    }

    public static <S> GlobalResponse<S> success(S data) {
        return new GlobalResponse<>(data, null);
    }

    public static GlobalResponse<?> error(String code, String message) {
        return new GlobalResponse<>(null, new GlobalErrorResponse(code, message));
    }

}

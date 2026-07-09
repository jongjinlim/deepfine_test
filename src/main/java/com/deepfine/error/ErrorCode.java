package com.deepfine.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements GlobalErrorType {

	BAD_REQUEST_BODY(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
	INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
	INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "수량은 0보다 커야 합니다."),
	LOCK_ACQUISITION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "다른 요청을 처리 중입니다. 잠시 후 다시 시도해 주세요."),
	DUPLICATE_PRODUCT_NAME(HttpStatus.CONFLICT, "이미 등록된 상품명입니다."),
	;

	private final HttpStatus status;
	private final String message;
}

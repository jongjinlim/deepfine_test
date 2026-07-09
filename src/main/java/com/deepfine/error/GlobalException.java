package com.deepfine.error;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

	private final ErrorCode errorCode;
	private final String identity;

	public GlobalException(ErrorCode errorCode, String identity) {
		this.errorCode = errorCode;
		this.identity = identity;
	}
}

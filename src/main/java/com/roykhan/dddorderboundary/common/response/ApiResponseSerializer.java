package com.roykhan.dddorderboundary.common.response;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * ApiResponse의 data 필드를 성공/실패에 따라 다르게 직렬화한다.
 *
 * <p>성공 응답은 data가 null이면 필드를 아예 생략하고, 실패 응답은 클라이언트가
 * 항상 data 키를 읽을 수 있도록 null이어도 유지한다.
 *
 * <p>등록은 {@code JacksonConfig}의 모듈 빈이 담당한다.
 */
public class ApiResponseSerializer extends ValueSerializer<ApiResponse<?>> {

    @Override
    public Class<?> handledType() {
        return ApiResponse.class;
    }

    @Override
    public void serialize(ApiResponse<?> response, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        gen.writeBooleanProperty("success", response.success());
        gen.writeStringProperty("code", response.code());
        gen.writeStringProperty("message", response.message());
        if (!response.success() || response.data() != null) {
            ctxt.defaultSerializeProperty("data", response.data(), gen);
        }
        gen.writeEndObject();
    }
}

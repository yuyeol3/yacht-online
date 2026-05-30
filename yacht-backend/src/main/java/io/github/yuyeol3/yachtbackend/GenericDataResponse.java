package io.github.yuyeol3.yachtbackend;

public record GenericDataResponse<T>(
        T data
) {
}

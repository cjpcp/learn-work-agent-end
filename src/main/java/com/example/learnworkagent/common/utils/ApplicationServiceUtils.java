package com.example.learnworkagent.common.utils;

import com.example.learnworkagent.common.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public class ApplicationServiceUtils {

    public static final String SORT_FIELD_CREATE_TIME = "createTime";

    private ApplicationServiceUtils() {
    }

    public static Pageable buildPageable(com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        return buildPageable(pageRequest, SORT_FIELD_CREATE_TIME);
    }

    public static Pageable buildPageable(com.example.learnworkagent.common.dto.PageRequest pageRequest, String sortField) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(pageRequest.getOrderDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.getPageNum() - 1,
                pageRequest.getPageSize(),
                Sort.by(direction, sortField)
        );
    }

    public static <T> PageResult<T> buildPageResult(Page<?> page, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        return buildPageResult(page.getContent(), page.getTotalElements(), pageRequest);
    }

    public static <T> PageResult<T> buildPageResult(List<?> content, long totalElements, com.example.learnworkagent.common.dto.PageRequest pageRequest) {
        return new PageResult<>(
                (List<T>) content,
                totalElements,
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );
    }
}
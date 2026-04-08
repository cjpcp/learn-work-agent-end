package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.consultation.dto.HumanTransferConfigRequest;
import com.example.learnworkagent.domain.consultation.dto.HumanTransferConfigResponse;
import com.example.learnworkagent.domain.consultation.service.HumanTransferConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "人工转接配置", description = "人工转接规则配置接口")
@RestController
@RequestMapping("/api/v1/consultation/transfer-config")
@RequiredArgsConstructor
public class HumanTransferConfigController {

    private final HumanTransferConfigService humanTransferConfigService;

    @Operation(summary = "获取人工转接配置列表")
    @GetMapping
    public Result<List<HumanTransferConfigResponse>> list() {
        return Result.success(humanTransferConfigService.findAll());
    }

    @Operation(summary = "创建人工转接配置")
    @PostMapping
    public Result<HumanTransferConfigResponse> create(@Valid @RequestBody HumanTransferConfigRequest request) {
        return Result.success(humanTransferConfigService.create(request));
    }

    @Operation(summary = "更新人工转接配置")
    @PutMapping("/{id}")
    public Result<HumanTransferConfigResponse> update(@PathVariable Long id, @Valid @RequestBody HumanTransferConfigRequest request) {
        return Result.success(humanTransferConfigService.update(id, request));
    }

    @Operation(summary = "删除人工转接配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        humanTransferConfigService.delete(id);
        return Result.success();
    }
}

package com.compliance.module.review.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.common.result.PageResult;
import com.compliance.module.review.service.ReviewTaskService;
import com.compliance.module.review.vo.ReviewSubmitReqVO;
import com.compliance.module.review.vo.ReviewTaskRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "合规审查任务", description = "Compliance review task management")
@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewTaskController {

    private final ReviewTaskService reviewTaskService;

    @Operation(summary = "提交审查内容", description = "Submit content for compliance review")
    @PostMapping("/submit")
    public CommonResult<ReviewTaskRespVO> submit(@Valid @RequestBody ReviewSubmitReqVO reqVO) {
        return CommonResult.success(reviewTaskService.submit(reqVO));
    }

    @Operation(summary = "上传文件审查", description = "Upload a file (image/PDF/Word) for compliance review")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<ReviewTaskRespVO> uploadAndReview(
            @RequestParam("file") MultipartFile file,
            @RequestParam("contentType") String contentType,
            @RequestParam(value = "productType", required = false) String productType,
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "tenantId", defaultValue = "1") Long tenantId,
            @RequestParam(value = "submittedBy", defaultValue = "1") Long submittedBy) {
        return CommonResult.success(reviewTaskService.uploadAndReview(file, contentType, productType, channel, tenantId, submittedBy));
    }

    @Operation(summary = "获取审查任务详情", description = "Get review task detail with results")
    @GetMapping("/{id}")
    public CommonResult<ReviewTaskRespVO> getById(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {
        return CommonResult.success(reviewTaskService.getById(id, tenantId));
    }

    @Operation(summary = "审查任务列表", description = "List review tasks with pagination")
    @GetMapping("/list")
    public CommonResult<PageResult<ReviewTaskRespVO>> list(
            @Parameter(description = "租户ID") @RequestParam Long tenantId,
            @Parameter(description = "审查状态") @RequestParam(required = false) String reviewStatus,
            @Parameter(description = "内容类型") @RequestParam(required = false) String contentType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(reviewTaskService.page(tenantId, reviewStatus, contentType, pageNum, pageSize));
    }
}

package com.saul.user.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saul.common.Result;
import com.saul.user.log.dto.LogQueryDTO;
import com.saul.user.log.service.ISysOperationLogService;
import com.saul.user.log.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志 Controller
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class SysOperationLogController {

    private final ISysOperationLogService logService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/list")
    public Result<Page<OperationLogVO>> list(LogQueryDTO query) {
        Page<OperationLogVO> page = logService.queryPage(query);
        return Result.success(page);
    }
}
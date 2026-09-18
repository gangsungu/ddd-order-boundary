package com.roykhan.dddorderboundary.common.controller;

import com.roykhan.dddorderboundary.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("OK");
    }
}
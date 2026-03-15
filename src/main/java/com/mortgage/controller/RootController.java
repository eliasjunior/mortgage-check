package com.mortgage.controller;

import com.mortgage.model.ApiInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RootController {

    private final String appName;
    private final String appVersion;

    public RootController(
            @Value("${spring.application.name}") String appName,
            @Value("${info.app.version:unknown}") String appVersion
    ) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    @GetMapping("/")
    public ResponseEntity<ApiInfo> root() {
        return ResponseEntity.ok(new ApiInfo(
                appName,
                appVersion,
                List.of(
                        "GET  /api/interest-rates",
                        "POST /api/mortgage-check"
                ),
                "/actuator/health"
        ));
    }
}

package com.project.klare_server.personal.web;

import com.project.klare_server.common.error.UnauthorizedException;
import com.project.klare_server.common.web.ApiResponse;
import com.project.klare_server.personal.dto.RegisterDeviceRequest;
import com.project.klare_server.personal.security.AuthenticatedPersonalUser;
import com.project.klare_server.personal.service.PersonalDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal/devices")
@Tag(name = "Personal Devices", description = "Push notification device registration")
public class PersonalDeviceController {

    private final PersonalDeviceService personalDeviceService;

    public PersonalDeviceController(PersonalDeviceService personalDeviceService) {
        this.personalDeviceService = personalDeviceService;
    }

    @Operation(summary = "Register a device for push", description = "Stores the Expo push token so Klare can send push notifications to this device.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @Valid @RequestBody RegisterDeviceRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        personalDeviceService.register(principal, request.expoPushToken(), request.platform());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "Unregister a device", description = "Removes a push token, typically on sign out.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal AuthenticatedPersonalUser principal,
            @Valid @RequestBody RegisterDeviceRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        personalDeviceService.unregister(request.expoPushToken());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

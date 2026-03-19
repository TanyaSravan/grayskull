package com.flipkart.grayskull.controllers;

import com.flipkart.grayskull.audit.AuditAction;
import com.flipkart.grayskull.audit.UserType;
import com.flipkart.grayskull.configuration.AuditQueryConfiguration;
import com.flipkart.grayskull.models.dto.response.AuditEntriesResponse;
import com.flipkart.grayskull.models.dto.response.ResponseTemplate;
import com.flipkart.grayskull.service.interfaces.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * REST controller for audit-related operations.
 * Provides endpoints to query audit entries for projects.
 */
@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@Validated
public class AuditController {

    private final AuditService auditService;
    private final AuditQueryConfiguration auditConfig;

    /**
     * Retrieves audit entries for a specific project with optional filtering.
     * Enforces maximum query age limit to prevent expensive queries on old data.
     * If no timestamp is provided, defaults to the configured maximum age.
     * 
     * @param projectId Project ID from the path
     * @param resourceName Optional resource name to filter audit entries
     * @param resourceType Optional resource type to filter audit entries (e.g., "SECRET", "PROJECT")
     * @param action Optional action to filter audit entries (e.g., CREATE_SECRET, READ_SECRET)
     * @param userType Optional user type filter (SERVICE or HUMAN)
     * @param afterTimestamp Optional ISO-8601 timestamp (entries after this time)
     * @param offset Pagination offset (default: 0)
     * @param limit Pagination limit (default: 10, max: 100)
     * @return ResponseTemplate containing list of audit entries
     */
    @Operation(summary = "Retrieves audit entries for a project with optional filtering by resource name, type, action, and user type")
    @GetMapping("/projects/{projectId}")
    @PreAuthorize("@grayskullSecurity.hasPermission(#projectId, 'audit.read')")
    public ResponseTemplate<AuditEntriesResponse> getProjectAudits(
            @PathVariable("projectId") @Size(max = 255) String projectId,
            @RequestParam(name = "resourceName", required = false) @Size(max = 500) String resourceName,
            @RequestParam(name = "resourceType", required = false) @Size(max = 100) String resourceType,
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "userType", required = false) UserType userType,
            @RequestParam(name = "afterTimestamp", required = false) String afterTimestamp,
            @RequestParam(name = "offset", defaultValue = "0") @Min(0) int offset,
            @RequestParam(name = "limit", defaultValue = "10") @Min(1) @Max(100) int limit) {
        
        Date effectiveTimestamp = getEffectiveTimestamp(parseTimestamp(afterTimestamp));
        AuditEntriesResponse response = auditService.getAuditEntries(Optional.of(projectId), Optional.ofNullable(resourceName), Optional.ofNullable(resourceType), Optional.ofNullable(action), Optional.ofNullable(userType), Optional.of(effectiveTimestamp), offset, limit);
        
        return ResponseTemplate.success(response, "Successfully retrieved audit entries.");
    }

    /**
     * Retrieves audit entries across all projects with optional filtering.
     * Requires global admin permission.
     * Supports time-range filtering via afterTimestamp for efficient querying.
     * 
     * @param projectId Optional project ID to filter audit entries
     * @param resourceName Optional resource name to filter audit entries
     * @param resourceType Optional resource type to filter audit entries (e.g., "SECRET", "PROJECT")
     * @param action Optional action to filter audit entries (e.g., CREATE_SECRET, READ_SECRET)
     * @param userType Optional user type filter (SERVICE or HUMAN)
     * @param afterTimestamp Optional ISO-8601 timestamp (entries after this time)
     * @param offset Pagination offset (default: 0)
     * @param limit Pagination limit (default: 10, max: 100)
     * @return ResponseTemplate containing list of audit entries
     */
    @Operation(summary = "Retrieves audit entries across all projects with optional filtering")
    @GetMapping
    @PreAuthorize("@grayskullSecurity.hasPermission('audit.read')")
    public ResponseTemplate<AuditEntriesResponse> getAllAudits(
            @RequestParam(name = "projectId", required = false) @Size(max = 255) String projectId,
            @RequestParam(name = "resourceName", required = false) @Size(max = 500) String resourceName,
            @RequestParam(name = "resourceType", required = false) @Size(max = 100) String resourceType,
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "userType", required = false) UserType userType,
            @RequestParam(name = "afterTimestamp", required = false) String afterTimestamp,
            @RequestParam(name = "offset", defaultValue = "0") @Min(0) int offset,
            @RequestParam(name = "limit", defaultValue = "10") @Min(1) @Max(100) int limit) {
        
        AuditEntriesResponse response = auditService.getAuditEntries(Optional.ofNullable(projectId), Optional.ofNullable(resourceName), Optional.ofNullable(resourceType), Optional.ofNullable(action), Optional.ofNullable(userType), parseTimestamp(afterTimestamp), offset, limit);
        
        return ResponseTemplate.success(response, "Successfully retrieved audit entries.");
    }

    /**
     * Gets the effective timestamp for audit queries.
     * Validates user-provided timestamps and defaults to maximum age limit if not provided.
     * 
     * @param afterTimestamp Optional user-provided timestamp
     * @return Effective timestamp to use (either validated user timestamp or default max age)
     * @throws IllegalArgumentException if user-provided timestamp is older than the allowed limit
     */
    private Date getEffectiveTimestamp(Optional<Date> afterTimestamp) {
        Date minAllowedTimestamp = Date.from(Instant.now().minus(auditConfig.getMaxQueryAgeDays(), ChronoUnit.DAYS));
        if (afterTimestamp.isEmpty()) {
            return minAllowedTimestamp;
        }
        
        Date requestedTimestamp = afterTimestamp.get();
        if (requestedTimestamp.before(minAllowedTimestamp)) {
            throw new IllegalArgumentException(String.format("Cannot query audit entries older than %d days.", auditConfig.getMaxQueryAgeDays()));
        }
        
        return requestedTimestamp;
    }

    /**
     * Parses an ISO-8601 timestamp string to a Date object.
     *
     * @param timestamp ISO-8601 timestamp string
     * @return Optional containing the parsed Date, or empty if timestamp is null
     */
    private Optional<Date> parseTimestamp(String timestamp) {
        return Optional.ofNullable(timestamp)
                .map(Instant::parse)
                .map(Date::from);
    }

}

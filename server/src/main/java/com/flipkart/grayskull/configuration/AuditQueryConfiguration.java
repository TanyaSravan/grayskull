package com.flipkart.grayskull.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for audit query behavior and filtering.
 * 
 * <p>Configuration in application.yml:</p>
 * <pre>
 * grayskull:
 *   audit:
 *     max-query-age-days: 180
 *     service-user-prefix: "service:"
 *     human-user-prefix: "user:"
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "grayskull.audit")
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditQueryConfiguration {
    
    /**
     * Maximum age (in days) of audit entries that can be queried.
     * Prevents expensive queries on very old data.
     * Default: 180 days (approximately 6 months).
     */
    @Min(value = 1, message = "Max query age must be at least 1 day")
    private int maxQueryAgeDays = 180;
    
    /**
     * Prefix pattern to identify service/system users.
     * Must not be blank to ensure proper filtering.
     */
    @NotBlank(message = "Service user prefix must be configured (grayskull.audit.service-user-prefix)")
    private String serviceUserPrefix;
    
    /**
     * Prefix pattern to identify human users.
     * Must not be blank to ensure proper filtering.
     */
    @NotBlank(message = "Human user prefix must be configured (grayskull.audit.human-user-prefix)")
    private String humanUserPrefix;
}

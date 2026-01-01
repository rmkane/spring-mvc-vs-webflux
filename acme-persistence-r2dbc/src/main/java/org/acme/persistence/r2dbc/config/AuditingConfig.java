package org.acme.persistence.r2dbc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * Configuration for automatic auditing of R2DBC entities. Enables @CreatedDate
 * and
 * 
 * @LastModifiedDate annotations similar to JPA's @CreatedDate
 *                   and @LastModifiedDate.
 */
@Configuration
@EnableR2dbcAuditing
public class AuditingConfig {
}

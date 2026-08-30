package com.cms.common.config;

import com.cms.common.security.SecurityConfig;
import com.cms.common.security.UserSyncFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({SecurityConfig.class, UserSyncFilter.class})
public class CommonSecurityAutoConfiguration {
}

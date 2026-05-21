package io.github.yuyeol3.yachtbackend.config.role;

import org.springframework.context.annotation.ConditionContext;

final class ServerRoleCondition {
    private static final String ROLE_PROPERTY = "yacht.server.role";
    private static final String ALL = "all";

    private ServerRoleCondition() {
    }

    static boolean matches(ConditionContext context, String role) {
        String configuredRole = context.getEnvironment()
                .getProperty(ROLE_PROPERTY, ALL)
                .trim()
                .toLowerCase();
        return ALL.equals(configuredRole) || role.equals(configuredRole);
    }
}

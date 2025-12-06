package org.hmmk.sms.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@SecurityScheme(
        securitySchemeName = "keycloak",
        type = SecuritySchemeType.OPENIDCONNECT,
        openIdConnectUrl = "https://keycloak.fastsms.dev/realms/a2p-realm/.well-known/openid-configuration"
)
public class SwaggerConfig extends Application {

}

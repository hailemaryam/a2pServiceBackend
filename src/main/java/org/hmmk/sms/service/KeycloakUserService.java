package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class KeycloakUserService {

    @Inject
    Keycloak keycloak;

    public void createTenantAdminUser(String username, String email, String password, String tenantId, String firstName,
            String lastName) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setAttributes(Map.of("tenantId", List.of(tenantId)));

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        // Create user
        Response response = keycloak.realm("a2p-realm").users().create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatus());
        }

        // Extract generated user ID
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // 🔥 IMPORTANT: Reload and update user so attributes are saved correctly
        UserResource userResource = keycloak.realm("a2p-realm").users().get(userId);
        userResource.update(user);

        // Assign tenant_admin role
        var roleRep = keycloak.realm("a2p-realm").roles().get("tenant_admin").toRepresentation();
        userResource.roles().realmLevel().add(List.of(roleRep));
    }

    /**
     * Get user details by ID
     */
    public UserRepresentation getUser(String userId) {
        return keycloak.realm("a2p-realm").users().get(userId).toRepresentation();
    }

    /**
     * Update user details (firstName, lastName)
     */
    public void updateUser(String userId, UserRepresentation userRep) {
        keycloak.realm("a2p-realm").users().get(userId).update(userRep);
    }

    /**
     * Update user password
     */
    public void updatePassword(String userId, String newPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        UserResource userResource = keycloak.realm("a2p-realm").users().get(userId);
        userResource.resetPassword(credential);
    }

    /**
     * Assign tenant ID to user and add tenant_admin role
     */
    public void assignTenantToUser(String userId, String tenantId) {
        UserResource userResource = keycloak.realm("a2p-realm").users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        // Update attribute
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = Map.of("tenantId", List.of(tenantId));
        } else {
            attributes.put("tenantId", List.of(tenantId));
        }
        user.setAttributes(attributes);
        userResource.update(user);

        // Assign tenant_admin role
        var roleRep = keycloak.realm("a2p-realm").roles().get("tenant_admin").toRepresentation();
        userResource.roles().realmLevel().add(List.of(roleRep));
    }
}

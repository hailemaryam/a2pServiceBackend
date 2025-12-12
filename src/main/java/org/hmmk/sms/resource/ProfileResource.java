package org.hmmk.sms.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.PasswordChangeRequest;
import org.hmmk.sms.dto.UserProfileDto;
import org.hmmk.sms.dto.UserProfileUpdateRequest;
import org.hmmk.sms.service.KeycloakUserService;
import org.keycloak.representations.idm.UserRepresentation;

@Path("/api/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Profile", description = "Manage user profile")
@Authenticated
public class ProfileResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    KeycloakUserService keycloakUserService;

    @GET
    @Operation(summary = "Get profile", description = "Get current user profile")
    @APIResponse(responseCode = "200", description = "User profile", content = @Content(schema = @Schema(implementation = UserProfileDto.class)))
    public UserProfileDto getProfile() {
        String userId = jwt.getSubject();
        UserRepresentation user = keycloakUserService.getUser(userId);
        return UserProfileDto.from(user);
    }

    @PUT
    @Operation(summary = "Update profile", description = "Update user profile details")
    @APIResponse(responseCode = "200", description = "Updated user profile", content = @Content(schema = @Schema(implementation = UserProfileDto.class)))
    public UserProfileDto updateProfile(@Valid UserProfileUpdateRequest request) {
        String userId = jwt.getSubject();

        // Get existing user to ensure we don't clear other fields
        UserRepresentation user = keycloakUserService.getUser(userId);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        keycloakUserService.updateUser(userId, user);

        return UserProfileDto.from(user);
    }

    @PUT
    @Path("/password")
    @Operation(summary = "Change password", description = "Change user password")
    @APIResponse(responseCode = "204", description = "Password changed successfully")
    public void changePassword(@Valid PasswordChangeRequest request) {
        String userId = jwt.getSubject();
        keycloakUserService.updatePassword(userId, request.getNewPassword());
    }
}

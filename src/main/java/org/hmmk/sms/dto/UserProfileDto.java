package org.hmmk.sms.dto;

import lombok.Data;
import org.keycloak.representations.idm.UserRepresentation;

@Data
public class UserProfileDto {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    public static UserProfileDto from(UserRepresentation user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        return dto;
    }
}

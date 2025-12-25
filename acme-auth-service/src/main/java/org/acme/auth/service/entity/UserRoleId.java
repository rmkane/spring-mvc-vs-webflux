package org.acme.auth.service.entity;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String roleName;
}

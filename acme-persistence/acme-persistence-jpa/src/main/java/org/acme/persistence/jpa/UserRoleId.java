package org.acme.persistence.jpa;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserRoleId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long user;
    private String roleName;
}

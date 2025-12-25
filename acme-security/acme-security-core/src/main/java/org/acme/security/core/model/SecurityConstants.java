package org.acme.security.core.model;

public interface SecurityConstants {

    String DN_HEADER = "x-dn";
    String UNAUTHORIZED_MESSAGE = "Missing or invalid x-dn header";
    String MISSING_DN_MESSAGE = "Missing or empty x-dn header";
}

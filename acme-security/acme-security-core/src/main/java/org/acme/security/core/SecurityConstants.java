package org.acme.security.core;

public interface SecurityConstants {

    String USERNAME_HEADER = "x-username";
    String UNAUTHORIZED_MESSAGE = "Missing or invalid x-username header";
    String MISSING_USERNAME_MESSAGE = "Missing or empty x-username header";
}

package org.restcomm.connect.identity.passwords;

/**
 * Checks the strength of a password
 *
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public interface PasswordValidator {
    boolean isStrongEnough(String password);
}

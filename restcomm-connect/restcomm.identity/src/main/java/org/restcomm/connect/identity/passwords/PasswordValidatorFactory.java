package org.restcomm.connect.identity.passwords;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class PasswordValidatorFactory {
    public static PasswordValidator createDefault() {
        return new JavascriptPasswordValidator();
    }
}

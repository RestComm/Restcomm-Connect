package org.restcomm.connect.identity.passwords;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class JavascriptPasswordValidationTest {
    @Test
    public void passwordStrengthTest() {
        PasswordValidator validator = PasswordValidatorFactory.createDefault();
        Assert.assertFalse(validator.isStrongEnough("1234"));
        Assert.assertFalse(validator.isStrongEnough("asdf123"));
        Assert.assertTrue(validator.isStrongEnough("asd123$#@"));
        Assert.assertTrue(validator.isStrongEnough("γιωργος123#!@"));
    }
}

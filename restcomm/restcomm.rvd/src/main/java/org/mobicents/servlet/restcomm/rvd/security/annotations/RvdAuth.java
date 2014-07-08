package org.mobicents.servlet.restcomm.rvd.security.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/*
 * Annotate Jersey resource methods to authenticate incoming request
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RvdAuth {

}

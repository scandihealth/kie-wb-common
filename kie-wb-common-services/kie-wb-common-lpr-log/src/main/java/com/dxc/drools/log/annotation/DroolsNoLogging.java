package com.dxc.drools.log.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Annotation that overrides inherited {@link DroolsLoggingToDB} annotation. This means that the method calls will in fact <b>NOT</b> be logged.
 * <br/>
 * Use this in subclasses to opt-out of logging when the superclass is annotated with DroolsLoggingToDB.
 * <p/>
 * Note:
 * The interceptor will still be activated, but it will skip any special processing and instead proceed calling the wrapped method immediately.
 * <br/>
 * This DroolsNoLogging annotation <b>is not inherited</b> to <i>your</i> subclasses, meaning that since they will still inherit the original annotation, they will have their calls logged.
 */
@Retention(RUNTIME)
@Target({METHOD, TYPE})
public @interface DroolsNoLogging {
}


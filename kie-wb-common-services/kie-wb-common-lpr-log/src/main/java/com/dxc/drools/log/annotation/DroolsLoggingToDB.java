package com.dxc.drools.log.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.interceptor.InterceptorBinding;

import com.dxc.drools.log.interceptors.DroolsLoggingToDBInterceptor;
import com.logica.heca.lpr.service.SystemLogManager;
import com.logica.heca.lpr.services.LogManager;
import org.uberfire.rpc.SessionInfo;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Annotation that binds {@link DroolsLoggingToDBInterceptor} to the annotation target.
 * <p/>
 * 1. If the invoked method or class is annotated with {@link DroolsNoLogging} no logging is
 * performed.
 * <br/>
 * 1. If a user session {@link SessionInfo} has been injected into the interceptor, then the call will be audit logged
 * by {@link LogManager}.
 * <br/>
 * 2. If the called method throws an exception, then the exception will be logged by <code>log4j</code> and
 * {@link SystemLogManager}, before being re-thrown.
 * <br/>
 * 3. The call is timed and the execution time is logged by <code>slf4j</code>, using the logger named "droolsTimerLog",
 * unless annotated with {@link DroolsSuppressTimeLogger}.
 */
@Inherited
@InterceptorBinding
@Retention(RUNTIME)
@Target({METHOD, TYPE})
public @interface DroolsLoggingToDB {
}

package com.dxc.drools.log.interceptors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.dxc.drools.log.DroolsTimeLogger;
import com.dxc.drools.log.annotation.DroolsLoggingToDB;
import com.dxc.drools.log.annotation.DroolsNoLogging;
import com.dxc.drools.log.annotation.DroolsSuppressTimeLogger;
import com.logica.heca.lpr.common.constant.EJBLookupConstants;
import com.logica.heca.lpr.domain.log.SystemLogDataType;
import com.logica.heca.lpr.domain.log.SystemLogDataVO;
import com.logica.heca.lpr.domain.log.UserLogDataVO;
import com.logica.heca.lpr.service.SystemLogManagerRemote;
import com.logica.heca.lpr.services.LogManagerRemote;
import com.logica.heca.lpr.util.EjbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.rpc.SessionInfo;

/**
 * Java EE interceptor that performs audit logging, error logging, and performance logging of intercepted methods.
 * @see DroolsLoggingToDB
 */
@DroolsLoggingToDB
@Interceptor
@Priority(2000)
//we use @Priority to enable this interceptor application-wide, without having to use beans.xml in every module.
public class DroolsLoggingToDBInterceptor {
    private static final Logger logger = LoggerFactory.getLogger( DroolsLoggingToDBInterceptor.class );

    private static ArrayList<Method> methodsWithoutSessionInfo = new ArrayList<Method>();

    private DroolsLoggingToDBInterceptor.EjbUtilsWrapper ejbUtilsWrapper = new DroolsLoggingToDBInterceptor.EjbUtilsWrapper();

    private DroolsTimeLogger droolsTimeLogger = new DroolsTimeLogger();
    @Inject
    SessionInfo sessionInfo;

    @AroundInvoke
    public Object logToDB( InvocationContext invocationContext ) throws Throwable {
        Method method = invocationContext.getMethod();

        if ( hasNoLoggingAnnotation( method ) ) return invocationContext.proceed(); //skip logging of this call

        StringBuilder parameterValues = extractParameterValues( invocationContext );

        Object result;
        try {
            // INVOKE THE ORIGINAL METHOD
            droolsTimeLogger.start();
            result = invocationContext.proceed();
        } catch ( Throwable throwable ) {
            logError( getMethodSignatureString( method ), throwable );
            logger.error( "Call " + getMethodSignatureString( method ) + "failed. Throwable rethrown as RuntimeException." );
            throw throwable;
        } finally {
            if ( method.getAnnotation( DroolsSuppressTimeLogger.class ) == null ) {
                droolsTimeLogger.log( "droolsTimerLog", getMethodSignatureString( method ) );
            }

            if ( sessionInfo == null && !methodsWithoutSessionInfo.contains( method ) ) {
                methodsWithoutSessionInfo.add( method );     // to avoid logging this problem numerous times
                logger.warn( method.toString() + " no SessionInfo injected for this invocation" );
            } else {
                if ( sessionInfo.getIdentity() != null && sessionInfo.getIdentity().getIdentifier() != null ) {
                    UserLogDataVO userLogData = new UserLogDataVO(
                            new Date(),
                            getMethodSignatureString( method ),
                            parameterValues.toString(),
                            sessionInfo.getIdentity().getIdentifier(),
                            sessionInfo.getIdentity().getIdentifier()
                    );
                    logUserActivity( userLogData );
                }
            }
        }
        return result;
    }

    private String getMethodSignatureString( Method method ) {
        Class<?> declaringClass = method.getDeclaringClass();
        return declaringClass.getSimpleName() + "." + method.getName() + getParametersString( method );
    }

    private String getParametersString( Method method ) {
        String parametersString;
        Class<?>[] parameterTypes = method.getParameterTypes();
        int iMax = parameterTypes.length - 1;
        if ( iMax == -1 ) {
            parametersString = "()";
        } else {
            StringBuilder b = new StringBuilder();
            b.append( '(' );
            for ( int i = 0; ; i++ ) {
                b.append( parameterTypes[i].getSimpleName() );
                if ( i == iMax ) {
                    parametersString = b.append( ')' ).toString();
                    break;
                } else {
                    b.append( ", " );
                }
            }
        }
        return parametersString;
    }

    private StringBuilder extractParameterValues( InvocationContext invocationContext ) {
        StringBuilder parameterValues = new StringBuilder();
        Object[] args = invocationContext.getParameters();
        if ( args != null && args.length > 0 ) {
            for ( Object arg : args ) {
                if ( arg != null ) {

                    String argToString = arg.toString();
                    String simpleName = arg.getClass().getSimpleName();
                    if ( argToString.contains( simpleName ) ) {
                        simpleName = "";
                    }
                    parameterValues.append( simpleName ).append( ": [ " ).append( argToString ).append( " ]" ).append( "\n" );
                }
            }
        }
        return parameterValues;
    }

    private boolean hasNoLoggingAnnotation( Method method ) throws Exception {
        Class<?> declaringClass = method.getDeclaringClass();
        return (method.getAnnotation( DroolsNoLogging.class ) != null) || (declaringClass.getAnnotation( DroolsNoLogging.class ) != null);
    }

    private void logUserActivity( UserLogDataVO userLogDataVO ) {
        try {
            LogManagerRemote logManagerRemote = ejbUtilsWrapper.lookupLogManagerRemote();
            logManagerRemote.log( userLogDataVO );
        } catch ( Throwable t ) {
            logger.error( "User activity was not logged to the DB because of: " + t.getMessage() + " --- Attempted to log activity: " + userLogDataVO.getFunction() );
        }
    }

    private void logError( String methodName, Throwable throwable ) {
        try {
            SystemLogDataVO systemLogEntry = new SystemLogDataVO( new Date(), SystemLogDataType.ERROR.getDb(), throwable.getMessage(), methodName );
            SystemLogManagerRemote systemLogManagerRemote = ejbUtilsWrapper.lookupSystemLogManagerRemote();
            systemLogManagerRemote.log( systemLogEntry, throwable );
        } catch ( Throwable t ) {
            logger.error( "System error was not logged to the DB because of: " + t.getMessage() + " --- Attempted to log message: " + throwable.getMessage() + " --- originating from method: " + methodName );
        }

    }

    class EjbUtilsWrapper {
        LogManagerRemote lookupLogManagerRemote() {
            return ( LogManagerRemote ) EjbUtils.lookupEJB( EJBLookupConstants.LOG_MANAGER_REMOTE );
        }

        SystemLogManagerRemote lookupSystemLogManagerRemote() {
            return ( SystemLogManagerRemote ) EjbUtils.lookupEJB( EJBLookupConstants.SYSTEM_LOG_MANAGER_REMOTE );
        }
    }
}

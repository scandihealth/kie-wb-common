package com.dxc.drools.log.interceptors;

import javax.interceptor.InvocationContext;

import com.dxc.drools.log.DroolsTimeLogger;
import com.dxc.drools.log.annotation.DroolsNoLogging;
import com.dxc.drools.log.annotation.DroolsSuppressTimeLogger;
import com.logica.heca.lpr.domain.log.SystemLogDataType;
import com.logica.heca.lpr.domain.log.SystemLogDataVO;
import com.logica.heca.lpr.domain.log.UserLogDataVO;
import com.logica.heca.lpr.service.SystemLogManagerRemote;
import com.logica.heca.lpr.services.LogManagerRemote;
import org.jboss.errai.security.shared.api.identity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.uberfire.rpc.SessionInfo;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for com.dxc.drools.log.DroolsLoggingToDBInterceptor.
 */
@RunWith(MockitoJUnitRunner.class)
public class DroolsLoggingToDBInterceptorTest {

    @Mock
    private InvocationContext invocationContext;
    @Mock
    private SessionInfo sessionInfo;
    @Mock
    private DroolsTimeLogger droolsTimeLogger;
    @Mock
    private DroolsLoggingToDBInterceptor.EjbUtilsWrapper ejbUtilsWrapper;

    @InjectMocks
    private DroolsLoggingToDBInterceptor droolsLoggingToDBInterceptor;

    @Test(expected = java.lang.NullPointerException.class)
    public void testLogToDBNullInvocationContext() throws Throwable {
        // Exercise method under test
        droolsLoggingToDBInterceptor.logToDB(null);
        fail("Using a 'null' InvocationContext did not produce expected NullPointerException");
    }

    @Test
    public void testDroolsNoLoggingAnnotation() throws Throwable {

        // Specify mock behavior
        when(invocationContext.getMethod()).thenReturn(DummyClass.class.getMethod("noLoggingMethod"));
        when(invocationContext.proceed()).thenReturn("42");

        // Exercise method under test
        Object result = droolsLoggingToDBInterceptor.logToDB(invocationContext);

        // Verify expected behavoir
        verify(invocationContext).getMethod();
        verify(invocationContext).proceed();
        verifyNoMoreInteractions(invocationContext);

        // Assert state
        assertTrue("Result of invocation has unexpected type", result instanceof String);
        assertEquals("'42' expected as result of invocation", "42", result.toString());
    }

    @Test
    public void testLoggingNoArgumentMethod() throws Throwable {

        // Specify mock behavior
        LogManagerRemote logManagerRemote = mock(LogManagerRemote.class);
        ArgumentCaptor<UserLogDataVO> userLogDataVOArgumentCaptor = ArgumentCaptor.forClass(UserLogDataVO.class);
        when(ejbUtilsWrapper.lookupLogManagerRemote()).thenReturn(logManagerRemote);

        when(invocationContext.getMethod()).thenReturn(DummyClass.class.getMethod("noArgsMethod"));
        when(invocationContext.proceed()).thenReturn("42");
        when(invocationContext.getParameters()).thenReturn(null);

        User mockUser = mock(User.class);
        when(sessionInfo.getIdentity()).thenReturn(mockUser);
        when(mockUser.getIdentifier()).thenReturn("mockIdentifierId");

        // Exercise method under test
        Object result = droolsLoggingToDBInterceptor.logToDB(invocationContext);

        // Verify expected behavoir
        verify(invocationContext).getMethod();
        verify(invocationContext).proceed();
        verify(invocationContext).getParameters();

        verify(sessionInfo, times(4)).getIdentity();
        verify(mockUser, times(3)).getIdentifier();

        verify(logManagerRemote).log(userLogDataVOArgumentCaptor.capture());

        verify(droolsTimeLogger).start();
        verify(droolsTimeLogger).log(anyString(),anyString());

        // Assert state
        UserLogDataVO capturedArgument = userLogDataVOArgumentCaptor.getValue();
        assertEquals("logManager.log called with wrong UserLogDataVO.function argument", "DummyClass.noArgsMethod()", capturedArgument.getFunction());
        assertEquals("logManager.log called with wrong UserLogDataVO.information argument", "", capturedArgument.getInformation());
        assertEquals("logManager.log called with wrong UserLogDataVO.userId argument", "mockIdentifierId", capturedArgument.getUserId());
        assertEquals("logManager.log called with wrong UserLogDataVO.name argument", "mockIdentifierId", capturedArgument.getName());

        // Assert state
        assertTrue("Result of invocation has unexpected type", result instanceof String);
        assertEquals("'42' expected as result of invocation", "42", result.toString());

        // Verify only specified methods are called
        verifyNoMoreInteractions(invocationContext, logManagerRemote, mockUser, sessionInfo, droolsTimeLogger);

    }

    @Test
    public void testDroolsSuppressTimeLogger() throws Throwable {

        // Specify mock behavior
        LogManagerRemote logManagerRemote = mock(LogManagerRemote.class);
        when(ejbUtilsWrapper.lookupLogManagerRemote()).thenReturn(logManagerRemote);

        when(invocationContext.getMethod()).thenReturn(DummyClass.class.getMethod("noTimeLoggerMethod"));
        when(invocationContext.proceed()).thenReturn("42");
        when(invocationContext.getParameters()).thenReturn(null);

        User mockUser = mock(User.class);
        when(sessionInfo.getIdentity()).thenReturn(mockUser);
        when(mockUser.getIdentifier()).thenReturn("mockIdentifierId");

        // Exercise method under test
        droolsLoggingToDBInterceptor.logToDB(invocationContext);

        // Verify expected behavoir
        verify(droolsTimeLogger).start();

        // Verify only specified methods are called
        verifyNoMoreInteractions(droolsTimeLogger);

    }

    @Test
    public void testLogging() throws Throwable {

        // Specify mock behavior
        LogManagerRemote logManagerRemote = mock(LogManagerRemote.class);
        ArgumentCaptor<UserLogDataVO> userLogDataVOArgumentCaptor = ArgumentCaptor.forClass(UserLogDataVO.class);
        when(ejbUtilsWrapper.lookupLogManagerRemote()).thenReturn(logManagerRemote);

        when(invocationContext.getMethod()).thenReturn(DummyClass.class.getMethod("method", String.class));
        when(invocationContext.proceed()).thenReturn("42");
        String[] args = {"argumentValue"};
        when(invocationContext.getParameters()).thenReturn(args);

        User mockUser = mock(User.class);
        when(sessionInfo.getIdentity()).thenReturn(mockUser);
        when(mockUser.getIdentifier()).thenReturn("mockIdentifierId");

        // Exercise method under test
        Object result = droolsLoggingToDBInterceptor.logToDB(invocationContext);

        // Verify expected behavoir
        verify(invocationContext).getMethod();
        verify(invocationContext).proceed();
        verify(invocationContext).getParameters();

        verify(sessionInfo, times(4)).getIdentity();
        verify(mockUser, times(3)).getIdentifier();

        verify(logManagerRemote).log(userLogDataVOArgumentCaptor.capture());

        // Assert state
        UserLogDataVO capturedArgument = userLogDataVOArgumentCaptor.getValue();
        assertEquals("logManager.log called with wrong UserLogDataVO.function argument", "DummyClass.method(String)", capturedArgument.getFunction());
        assertEquals("logManager.log called with wrong UserLogDataVO.information argument", "String: [ argumentValue ]\n", capturedArgument.getInformation());
        assertEquals("logManager.log called with wrong UserLogDataVO.userId argument", "mockIdentifierId", capturedArgument.getUserId());
        assertEquals("logManager.log called with wrong UserLogDataVO.name argument", "mockIdentifierId", capturedArgument.getName());

        // Assert state
        assertTrue("Result of invocation has unexpected type", result instanceof String);
        assertEquals("'42' expected as result of invocation", "42", result.toString());

        // Verify only specified methods are called
        verifyNoMoreInteractions(invocationContext, logManagerRemote, mockUser, sessionInfo);

    }

    @Test
    public void testErrorLogging() throws Throwable {

        // Specify mock behavior
        LogManagerRemote logManagerRemote = mock(LogManagerRemote.class);
        when(ejbUtilsWrapper.lookupLogManagerRemote()).thenReturn(logManagerRemote);
        SystemLogManagerRemote systemLogManagerRemote = mock(SystemLogManagerRemote.class);
        ArgumentCaptor<SystemLogDataVO> systemLogDataVOArgumentCaptor = ArgumentCaptor.forClass(SystemLogDataVO.class);
        ArgumentCaptor<Throwable> throwableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        when(ejbUtilsWrapper.lookupSystemLogManagerRemote()).thenReturn(systemLogManagerRemote);

        when(invocationContext.getMethod()).thenReturn(DummyClass.class.getMethod("noArgsMethod"));
        when(invocationContext.proceed()).thenThrow(new RuntimeException("runtimeException"));

        User mockUser = mock(User.class);
        when(sessionInfo.getIdentity()).thenReturn(mockUser);
        when(mockUser.getIdentifier()).thenReturn("mockIdentifierId");

        // Exercise method under test
        try {
            droolsLoggingToDBInterceptor.logToDB(invocationContext);
        } catch (RuntimeException e) {
            assertEquals("runtimeException", e.getMessage());
        }
        // Verify expected behavoir
        verify(invocationContext).getMethod();
        verify(invocationContext).proceed();
        verify(invocationContext).getParameters();

        verify(sessionInfo, times(4)).getIdentity();
        verify(mockUser, times(3)).getIdentifier();

        verify(systemLogManagerRemote).log(systemLogDataVOArgumentCaptor.capture(), throwableArgumentCaptor.capture());

        // Assert state
        SystemLogDataVO capturedSystemLogDataVO = systemLogDataVOArgumentCaptor.getValue();
        assertEquals("runtimeException", capturedSystemLogDataVO.getInformation());
        assertEquals(SystemLogDataType.ERROR, capturedSystemLogDataVO.getType());
        assertEquals("DummyClass.noArgsMethod()", capturedSystemLogDataVO.getModule());

        Throwable capturedThrowable = throwableArgumentCaptor.getValue();
        assertEquals("runtimeException", capturedThrowable.getMessage());

        // Verify only specified methods are called
        verifyNoMoreInteractions(invocationContext, systemLogManagerRemote, mockUser, sessionInfo);

    }

    // Dummy class provoking different interceptor behavior. Mocking is not possible as the Method class is final and
    // Mockito doesn't support mocking final classes.
    @SuppressWarnings({"WeakerAccess", "unused"})
    class DummyClass {
        public void noArgsMethod() {
        }

        public void method(String arg) {
        }

        @DroolsNoLogging
        public void noLoggingMethod() {
        }

        @DroolsSuppressTimeLogger
        public void noTimeLoggerMethod() {
        }
    }

}
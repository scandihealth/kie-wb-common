package com.dxc.drools.log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for com.dxc.drools.log.DroolsTimeLogger.
 */
@RunWith(MockitoJUnitRunner.class)
public class DroolsTimeLoggerTest {

    private static final String LOGGER_NAME = "logger-name";
    private static final String METHOD_NAME = "method-name";

    @Mock
    private DroolsTimeLogger.LogWrapper logWrapper;

    @InjectMocks
    private DroolsTimeLogger droolsTimeLogger;

    @Test
    public void testInActiveLog() throws Exception {
        //setup
        Logger loggerMock = mock(Logger.class);
        ArgumentCaptor<String> logStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        when(logWrapper.getLogger(LOGGER_NAME)).thenReturn(loggerMock);
        when(loggerMock.isInfoEnabled()).thenReturn(false);

        //execute
        droolsTimeLogger.start();
        droolsTimeLogger.log(LOGGER_NAME, METHOD_NAME);

        //verify behavoir
        verify(logWrapper).getLogger(LOGGER_NAME);
        verifyNoMoreInteractions(logWrapper);

        verify(loggerMock).isInfoEnabled();
        verify(loggerMock).warn(logStringArgumentCaptor.capture());
        verifyNoMoreInteractions(loggerMock);

        //verify state
        assertEquals("Log message not as expected", "Cannot log time as level info is not enabled", logStringArgumentCaptor.getValue() );
    }

    @Test
    public void testActiveLog() throws Exception {
        //setup
        Logger loggerMock = mock(Logger.class);
        ArgumentCaptor<String> logStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        when(logWrapper.getLogger(LOGGER_NAME)).thenReturn(loggerMock);
        when(loggerMock.isInfoEnabled()).thenReturn(true);

        //execute
        long beforeTime = System.nanoTime();
        droolsTimeLogger.start();
        droolsTimeLogger.log(LOGGER_NAME, METHOD_NAME);
        long afterTime = System.nanoTime();

        //verify behavoir
        verify(logWrapper).getLogger(LOGGER_NAME);
        verifyNoMoreInteractions(logWrapper);

        verify(loggerMock).isInfoEnabled();
        verify(loggerMock).info(logStringArgumentCaptor.capture());
        verifyNoMoreInteractions(loggerMock);

        //verify state
        assertTrue("Method name expected", logStringArgumentCaptor.getValue().contains(METHOD_NAME));
        assertTrue("Time measured no as expected", ((afterTime - beforeTime)/1000) >= extractTime( logStringArgumentCaptor.getValue() ));
    }

    private long extractTime(String s) {
        // Log message has format "| method-name |          4| ms|"
        return Long.parseLong(s.split("\\|")[2].trim());
    }


}
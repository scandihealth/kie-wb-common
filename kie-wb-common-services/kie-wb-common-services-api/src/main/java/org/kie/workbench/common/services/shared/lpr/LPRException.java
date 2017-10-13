package org.kie.workbench.common.services.shared.lpr;

import org.jboss.errai.common.client.api.annotations.Portable;

/**
 * Created on 13-10-2017.
 */
@Portable
public class LPRException extends Exception {

    public LPRException() {
    }

    public LPRException( String message ) {
        super( message );
    }

    public LPRException( String message, Throwable cause ) {
        super( message, cause );
    }

    public LPRException( Throwable cause ) {
        super( cause );
    }
}

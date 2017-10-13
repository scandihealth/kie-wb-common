package org.kie.workbench.common.services.shared.lpr;

import org.jboss.errai.bus.server.annotations.Remote;
import org.uberfire.backend.vfs.Path;


/**
 * Created on 09-10-2017.
 */
@Remote
public interface LPRManageProductionService {

    Path copyToProductionBranch( Path rulePath );

    Path deleteFromProductionBranch( Path rulePath );
}

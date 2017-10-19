package org.kie.workbench.common.services.shared.lpr;

import org.jboss.errai.bus.server.annotations.Remote;
import org.uberfire.backend.vfs.Path;

/**
 * Created on 09-10-2017.
 */
@Remote
public interface LPRManageProductionService {

    Path copyToProductionBranch(Path sourcePath);

    Path deleteFromProductionBranch(Path sourcePath);

    /**
     * @param pathToLatest path to read all versions from
     * @return Path to latest prod version or null if no prod version exists
     */
    Path getProdVersion( Path pathToLatest);
}

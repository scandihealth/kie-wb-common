package org.kie.workbench.common.services.backend.lpr;

import java.net.URISyntaxException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.dxc.drools.log.annotation.DroolsLoggingToDB;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.common.services.shared.lpr.LPRManageProductionService;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.commons.validation.PortablePreconditions;
import org.uberfire.ext.editor.commons.backend.version.VersionUtil;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.StandardCopyOption;

/**
 * Created on 09-10-2017.
 */
@Service
@ApplicationScoped
@DroolsLoggingToDB
public class LPRManageProductionServiceImpl implements LPRManageProductionService {
    //todo ttn unit tests
    private IOService ioService;
    private VersionUtil versionUtil;

    @Inject
    public LPRManageProductionServiceImpl( @Named("ioStrategy") final IOService ioService, VersionUtil versionUtil ) {
        this.ioService = PortablePreconditions.checkNotNull( "ioService", ioService );
        this.versionUtil = PortablePreconditions.checkNotNull( "versionUtil", versionUtil );
    }

    @Override
    public Path copyToProductionBranch( Path sourcePath ) {
        org.uberfire.java.nio.file.Path ruleSourceFilePath = Paths.convert( sourcePath );
        String version = versionUtil.getVersion( ruleSourceFilePath );
        PortablePreconditions.checkCondition( "sourcePath should point to master", LPRGitBranchesConsts.MASTER.equalsIgnoreCase( version ) );
        org.uberfire.java.nio.file.Path prodFilePath;
        try {
            prodFilePath = versionUtil.getPath( ruleSourceFilePath, LPRGitBranchesConsts.PROD );
        } catch ( URISyntaxException e ) {
            throw new RuntimeException( "URISyntaxException while parsing sourcePath URI", e );
        }
        ioService.copy( ruleSourceFilePath, prodFilePath, StandardCopyOption.REPLACE_EXISTING );
        return Paths.convert( prodFilePath );
    }

    @Override
    public Path deleteFromProductionBranch( Path rulePath ) {
        return null;
    }
}

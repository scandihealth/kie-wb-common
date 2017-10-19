package org.kie.workbench.common.services.backend.lpr;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.dxc.drools.log.annotation.DroolsLoggingToDB;
import org.guvnor.common.services.shared.metadata.model.LprMetadataConsts;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.common.services.shared.lpr.LPRManageProductionService;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.PathFactory;
import org.uberfire.commons.validation.PortablePreconditions;
import org.uberfire.ext.editor.commons.backend.version.VersionRecordService;
import org.uberfire.ext.editor.commons.backend.version.VersionUtil;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.base.version.VersionRecord;
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
    private VersionRecordService versionRecordService;

    @Inject
    public LPRManageProductionServiceImpl(
            @Named("ioStrategy") final IOService ioService,
            VersionUtil versionUtil,
            VersionRecordService versionRecordService ) {
        this.ioService = PortablePreconditions.checkNotNull( "ioService", ioService );
        this.versionUtil = PortablePreconditions.checkNotNull( "versionUtil", versionUtil );
        this.versionRecordService = versionRecordService;
    }

    @Override
    public Path copyToProductionBranch( Path sourcePath ) {
        org.uberfire.java.nio.file.Path ruleSourceFilePath = Paths.convert( sourcePath );
        org.uberfire.java.nio.file.Path prodFilePath = getProdPath( ruleSourceFilePath );
        ioService.copy( ruleSourceFilePath, prodFilePath, StandardCopyOption.REPLACE_EXISTING );
        return Paths.convert( prodFilePath );
    }

    @Override
    public Path deleteFromProductionBranch( Path sourcePath ) {
        org.uberfire.java.nio.file.Path ruleSourceFilePath = Paths.convert( sourcePath );
        org.uberfire.java.nio.file.Path prodFilePath = getProdPath( ruleSourceFilePath );
        ioService.delete( prodFilePath );
        return Paths.convert( prodFilePath );
    }

    @Override
    public Path getProdVersion( Path pathToLatest ) {
        List<VersionRecord> versionRecords = versionRecordService.loadVersionRecords( Paths.convert( pathToLatest ) );
        Collections.reverse( versionRecords );
        for ( VersionRecord record : versionRecords ) {
            //versionRecords is sorted by commit date, descending, so the first prod version we find is the newest prod version
            //todo ttn does not work if rule was renamed after being put into prod
            Path pathToVersion = PathFactory.newPathBasedOn( pathToLatest.getFileName(), record.uri(), pathToLatest );
            Map<String, Object> attributes = ioService.readAttributes( Paths.convert( pathToVersion ) );
            Long prodDate = ( Long ) attributes.get( LprMetadataConsts.PRODUCTION_DATE );
            if ( prodDate != null && prodDate > 0L ) {
                return pathToVersion;
            }
        }
        return null; //no prod version found
    }

    private org.uberfire.java.nio.file.Path getProdPath( org.uberfire.java.nio.file.Path ruleSourceFilePath ) {
        String version = versionUtil.getVersion( ruleSourceFilePath );
        PortablePreconditions.checkCondition( "sourcePath should point to master", LPRGitBranchesConsts.MASTER.equalsIgnoreCase( version ) );
        org.uberfire.java.nio.file.Path prodFilePath;
        try {
            prodFilePath = versionUtil.getPath( ruleSourceFilePath, LPRGitBranchesConsts.PROD );
        } catch ( URISyntaxException e ) {
            throw new RuntimeException( "URISyntaxException while parsing sourcePath URI", e );
        }
        return prodFilePath;
    }

}

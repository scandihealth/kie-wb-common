/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.kie.workbench.common.screens.search.backend.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.inject.Instance;

import org.apache.lucene.search.Sort;
import org.guvnor.common.services.backend.metadata.DublinCoreAttributesMock;
import org.guvnor.common.services.backend.metadata.LprMetaAttributesMock;
import org.guvnor.common.services.backend.metadata.VersionAttributesMock;
import org.guvnor.common.services.backend.metadata.attribute.LprMetaView;
import org.guvnor.common.services.backend.metadata.attribute.OtherMetaView;
import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.guvnor.common.services.shared.metadata.model.LprRuleGroup;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.organizationalunit.OrganizationalUnitService;
import org.guvnor.structure.organizationalunit.impl.OrganizationalUnitImpl;
import org.guvnor.structure.repositories.Repository;
import org.jboss.errai.security.shared.api.identity.User;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.screens.search.model.QueryMetadataPageRequest;
import org.kie.workbench.common.screens.search.model.SearchPageRow;
import org.kie.workbench.common.screens.search.model.SearchTermPageRequest;
import org.kie.workbench.common.services.shared.project.KieProject;
import org.kie.workbench.common.services.shared.project.KieProjectService;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.PathFactory;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.search.DateRange;
import org.uberfire.ext.metadata.search.IOSearchService;
import org.uberfire.io.IOService;
import org.uberfire.io.attribute.DublinCoreView;
import org.uberfire.java.nio.base.version.VersionAttributeView;
import org.uberfire.java.nio.file.Path;
import org.uberfire.paging.PageResponse;
import org.uberfire.security.authz.AuthorizationManager;
import org.uberfire.workbench.type.ResourceTypeDefinition;

import static org.guvnor.common.services.shared.metadata.model.LprMetadataConsts.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SearchServiceImplTest {

    private IOSearchService ioSearchService;

    private IOService ioService;

    private OrganizationalUnitService organizationalUnitService;

    private KieProjectService projectService;

    protected User identity;

    private AuthorizationManager authorizationManager;

    private Instance<ResourceTypeDefinition> typeRegister;

    private SearchServiceImpl searchService;

    private final OrganizationalUnit ou1 = new OrganizationalUnitImpl( "ou1",
            "owner",
            "ou1" );
    private final OrganizationalUnit ou2 = new OrganizationalUnitImpl( "ou2",
            "owner",
            "ou2" );
    private final Repository repo1 = mock( Repository.class );

    private final Repository repo2 = mock( Repository.class );

    private final KieProject project1 = mock( KieProject.class );

    @Before
    public void setup() {
        ioSearchService = mock( IOSearchService.class );
        ioService = mock( IOService.class );
        organizationalUnitService = mock( OrganizationalUnitService.class );
        projectService = mock( KieProjectService.class );
        identity = mock( User.class );
        authorizationManager = mock( AuthorizationManager.class );
        typeRegister = new MockTypeRegister();

        searchService = new SearchServiceImpl( ioSearchService,
                ioService,
                organizationalUnitService,
                projectService,
                identity,
                authorizationManager,
                typeRegister );
        searchService.init();

        final Collection<OrganizationalUnit> allOUs = new ArrayList<OrganizationalUnit>();

        final org.uberfire.backend.vfs.Path repo1Root = mock( org.uberfire.backend.vfs.Path.class );
        when( repo1Root.toURI() ).thenReturn( "file://repo1/p0" );
        final org.uberfire.backend.vfs.Path repo2Root = mock( org.uberfire.backend.vfs.Path.class );
        when( repo2Root.toURI() ).thenReturn( "file://repo2/p1" );

        when( repo1.getAlias() ).thenReturn( "repo1" );
        when( repo1.getRoot() ).thenReturn( repo1Root );

        when( repo2.getAlias() ).thenReturn( "repo2" );
        when( repo2.getRoot() ).thenReturn( repo2Root );

        ou1.getRepositories().add( repo1 );
        ou2.getRepositories().add( repo2 );

        allOUs.add( ou1 );
        allOUs.add( ou2 );

        when( organizationalUnitService.getOrganizationalUnits() ).thenReturn( allOUs );
    }

    @Test
    public void testRepositoryAccessLackOUPermissions() {
        //Setup access rights - Revoke access to OU2
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( false );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        final Path[] paths = searchService.getAuthorizedRepositoryRoots();

        assertEquals( 1,
                paths.length );
        assertEquals( "p0",
                paths[0].getFileName().toString() );
    }

    @Test
    public void testRepositoryAccessLackRepositoryPermissions() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Revoke access to repo2
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( false );

        final Path[] paths = searchService.getAuthorizedRepositoryRoots();

        assertEquals( 1,
                paths.length );
        assertEquals( "p0",
                paths[0].getFileName().toString() );
    }

    @Test
    public void testFullTextSearchProjectAccess() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( true );

        final SearchTermPageRequest pageRequest = new SearchTermPageRequest( "smurf",
                0,
                5 );

        //Setup search
        final org.uberfire.backend.vfs.Path vfsPath = PathFactory.newPath( "file1", "default://project1/file1" );
        final Path nioPath = Paths.convert( vfsPath );
        final KObject kObject = mock( KObject.class );

        when( kObject.getKey() ).thenReturn( "default://project1/file1" );
        when( ioSearchService.fullTextSearchHits( eq( "smurf" ),
                Matchers.<Path>anyVararg() ) ).thenReturn( 1 );
        when( ioSearchService.fullTextSearch( eq( "smurf" ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                if ( filter.accept( kObject ) ) {
                    result.add( nioPath );
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( project1 );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( new LprMetaAttributesMock() );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search
        final PageResponse<SearchPageRow> results = searchService.fullTextSearch( pageRequest );
        assertEquals( 1,
                results.getTotalRowSize() );
        assertEquals( vfsPath.getFileName(),
                results.getPageRowList().get( 0 ).getPath().getFileName() );
    }

    @Test
    public void testFullTextSearchProjectAccessLackPermissions() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Revoke access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( false );

        final SearchTermPageRequest pageRequest = new SearchTermPageRequest( "smurf",
                0,
                5 );

        //Setup search
        final org.uberfire.backend.vfs.Path vfsPath = PathFactory.newPath( "file1", "default://project1/file1" );
        final Path nioPath = Paths.convert( vfsPath );
        final KObject kObject = mock( KObject.class );

        when( kObject.getKey() ).thenReturn( "default://project1/file1" );
        when( ioSearchService.fullTextSearchHits( eq( "smurf" ),
                Matchers.<Path>anyVararg() ) ).thenReturn( 1 );
        when( ioSearchService.fullTextSearch( eq( "smurf" ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                if ( filter.accept( kObject ) ) {
                    result.add( nioPath );
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( project1 );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( new LprMetaAttributesMock() );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search
        final PageResponse<SearchPageRow> results = searchService.fullTextSearch( pageRequest );
        assertEquals( 0,
                results.getTotalRowSize() );
    }

    @Test
    public void testMetadataSearchProjectAccess() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( true );

        final QueryMetadataPageRequest pageRequest = new QueryMetadataPageRequest( Collections.EMPTY_MAP,
                null,
                null,
                null,
                null,
                0,
                5 );

        //Setup search
        final org.uberfire.backend.vfs.Path vfsPath = PathFactory.newPath( "file1", "default://project1/file1" );
        final Path nioPath = Paths.convert( vfsPath );
        final KObject kObject = mock( KObject.class );

        when( kObject.getKey() ).thenReturn( "default://project1/file1" );
        when( ioSearchService.searchByAttrsHits( any( Map.class ),
                Matchers.<Path>anyVararg() ) ).thenReturn( 1 );
        when( ioSearchService.searchByAttrs( any( Map.class ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                any( Sort.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                if ( filter.accept( kObject ) ) {
                    result.add( nioPath );
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( project1 );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( new LprMetaAttributesMock() );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search
        final PageResponse<SearchPageRow> results = searchService.queryMetadata( pageRequest );
        assertEquals( 1,
                results.getTotalRowSize() );
        assertEquals( vfsPath.getFileName(),
                results.getPageRowList().get( 0 ).getPath().getFileName() );
    }

    @Test
    public void testMetadataSearchProjectAccessLackPermissions() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Revoke access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( false );

        final QueryMetadataPageRequest pageRequest = new QueryMetadataPageRequest( Collections.EMPTY_MAP,
                null,
                null,
                null,
                null,
                0,
                5 );

        //Setup search
        final org.uberfire.backend.vfs.Path vfsPath = PathFactory.newPath( "file1", "default://project1/file1" );
        final Path nioPath = Paths.convert( vfsPath );
        final KObject kObject = mock( KObject.class );

        when( kObject.getKey() ).thenReturn( "default://project1/file1" );
        when( ioSearchService.searchByAttrsHits( any( Map.class ),
                Matchers.<Path>anyVararg() ) ).thenReturn( 1 );
        when( ioSearchService.searchByAttrs( any( Map.class ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                if ( filter.accept( kObject ) ) {
                    result.add( nioPath );
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( project1 );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( new LprMetaAttributesMock() );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search
        final PageResponse<SearchPageRow> results = searchService.queryMetadata( pageRequest );
        assertEquals( 0,
                results.getTotalRowSize() );
    }

    @Test
    public void testFullTextSearchOutsideProjectStructure() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( true );

        final SearchTermPageRequest pageRequest = new SearchTermPageRequest( "smurf",
                0,
                5 );

        //Setup search
        final org.uberfire.backend.vfs.Path vfsPath = PathFactory.newPath( "file1", "default://project1/file1" );
        final Path nioPath = Paths.convert( vfsPath );
        final KObject kObject = mock( KObject.class );

        when( kObject.getKey() ).thenReturn( "default://project1/file1" );
        when( ioSearchService.fullTextSearchHits( eq( "smurf" ),
                Matchers.<Path>anyVararg() ) ).thenReturn( 1 );
        when( ioSearchService.fullTextSearch( eq( "smurf" ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                if ( filter.accept( kObject ) ) {
                    result.add( nioPath );
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( null );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( new LprMetaAttributesMock() );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search
        final PageResponse<SearchPageRow> results = searchService.fullTextSearch( pageRequest );
        assertEquals( 1,
                results.getTotalRowSize() );
        assertEquals( vfsPath.getFileName(),
                results.getPageRowList().get( 0 ).getPath().getFileName() );
    }

    @Test
    public void testMetadataSearchOutsideProjectStructure() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( true );

        final QueryMetadataPageRequest pageRequest = new QueryMetadataPageRequest( Collections.EMPTY_MAP,
                null,
                null,
                null,
                null,
                0,
                5 );

        //Setup search
        final org.uberfire.backend.vfs.Path vfsPath = PathFactory.newPath( "file1", "default://project1/file1" );
        final Path nioPath = Paths.convert( vfsPath );
        final KObject kObject = mock( KObject.class );

        when( kObject.getKey() ).thenReturn( "default://project1/file1" );
        when( ioSearchService.searchByAttrsHits( any( Map.class ),
                Matchers.<Path>anyVararg() ) ).thenReturn( 1 );
        when( ioSearchService.searchByAttrs( any( Map.class ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                any( Sort.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                if ( filter.accept( kObject ) ) {
                    result.add( nioPath );
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( null );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( new LprMetaAttributesMock() );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search
        final PageResponse<SearchPageRow> results = searchService.queryMetadata( pageRequest );
        assertEquals( 1,
                results.getTotalRowSize() );
        assertEquals( vfsPath.getFileName(),
                results.getPageRowList().get( 0 ).getPath().getFileName() );
    }

    @Test
    public void testFullTextSearchMultiplePages() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( true );

        //Setup search (total SIZE is deliberately not a multiple of PAGE_SIZE to check partial page responses)
        final int SIZE = 13;
        final int PAGE_SIZE = 5;
        final org.uberfire.backend.vfs.Path vfsPath[] = new org.uberfire.backend.vfs.Path[SIZE];
        final Path nioPath[] = new Path[SIZE];
        final KObject kObject[] = new KObject[SIZE];
        for ( int i = 0; i < SIZE; i++ ) {
            vfsPath[i] = PathFactory.newPath( "file" + i,
                    "default://project1/file" + i );
            nioPath[i] = Paths.convert( vfsPath[i] );
            kObject[i] = mock( KObject.class );
            when( kObject[i].getKey() ).thenReturn( "default://project1/file" + i );
        }
        when( ioSearchService.fullTextSearchHits( eq( "smurf" ),
                Matchers.<Path>anyVararg() ) ).thenReturn( SIZE );
        when( ioSearchService.fullTextSearch( eq( "smurf" ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                for ( int i = 0; i < SIZE; i++ ) {
                    if ( filter.accept( kObject[i] ) ) {
                        result.add( nioPath[i] );
                    }
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( project1 );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( new LprMetaAttributesMock() );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search - Page 1
        int startIndex = 0;
        final SearchTermPageRequest page1Request = new SearchTermPageRequest( "smurf",
                startIndex,
                PAGE_SIZE );
        final PageResponse<SearchPageRow> page1Results = searchService.fullTextSearch( page1Request );
        assertTrue( page1Results.isFirstPage() );
        assertFalse( page1Results.isLastPage() );
        assertEquals( PAGE_SIZE,
                page1Results.getPageRowList().size() );
        assertEquals( SIZE,
                page1Results.getTotalRowSize() );
        assertTrue( page1Results.isTotalRowSizeExact() );
        for ( int i = 0; i < PAGE_SIZE; i++ ) {
            assertEquals( vfsPath[startIndex + i].getFileName(),
                    page1Results.getPageRowList().get( i ).getPath().getFileName() );
        }

        //Perform search - Page 2
        startIndex = startIndex + page1Results.getPageRowList().size();
        final SearchTermPageRequest page2Request = new SearchTermPageRequest( "smurf",
                startIndex,
                PAGE_SIZE );
        final PageResponse<SearchPageRow> page2Results = searchService.fullTextSearch( page2Request );
        assertFalse( page2Results.isFirstPage() );
        assertFalse( page2Results.isLastPage() );
        assertEquals( PAGE_SIZE,
                page2Results.getPageRowList().size() );
        assertEquals( SIZE,
                page2Results.getTotalRowSize() );
        assertTrue( page2Results.isTotalRowSizeExact() );
        for ( int i = 0; i < PAGE_SIZE; i++ ) {
            assertEquals( vfsPath[startIndex + i].getFileName(),
                    page2Results.getPageRowList().get( i ).getPath().getFileName() );
        }

        //Perform search - Page 3
        startIndex = startIndex + page2Results.getPageRowList().size();
        final SearchTermPageRequest page3Request = new SearchTermPageRequest( "smurf",
                startIndex,
                PAGE_SIZE );
        final PageResponse<SearchPageRow> page3Results = searchService.fullTextSearch( page3Request );
        assertFalse( page3Results.isFirstPage() );
        assertTrue( page3Results.isLastPage() );
        assertEquals( 3,
                page3Results.getPageRowList().size() );
        assertEquals( SIZE,
                page3Results.getTotalRowSize() );
        assertTrue( page3Results.isTotalRowSizeExact() );
        for ( int i = 0; i < 3; i++ ) {
            assertEquals( vfsPath[startIndex + i].getFileName(),
                    page3Results.getPageRowList().get( i ).getPath().getFileName() );
        }
    }

    @Test
    public void testMetadataSearchMultiplePages() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( true );

        //Setup search (total SIZE is deliberately not a multiple of PAGE_SIZE to check partial page responses)
        final int SIZE = 13;
        final int PAGE_SIZE = 5;
        final org.uberfire.backend.vfs.Path vfsPath[] = new org.uberfire.backend.vfs.Path[SIZE];
        final Path nioPath[] = new Path[SIZE];
        final KObject kObject[] = new KObject[SIZE];
        for ( int i = 0; i < SIZE; i++ ) {
            vfsPath[i] = PathFactory.newPath( "file" + i,
                    "default://project1/file" + i );
            nioPath[i] = Paths.convert( vfsPath[i] );
            kObject[i] = mock( KObject.class );
            when( kObject[i].getKey() ).thenReturn( "default://project1/file" + i );
        }

        when( ioSearchService.searchByAttrsHits( any( Map.class ),
                Matchers.<Path>anyVararg() ) ).thenReturn( SIZE );
        when( ioSearchService.searchByAttrs( any( Map.class ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                any( Sort.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                for ( int i = 0; i < SIZE; i++ ) {
                    if ( filter.accept( kObject[i] ) ) {
                        result.add( nioPath[i] );
                    }
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( project1 );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( new LprMetaAttributesMock() );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search - Page 1
        int startIndex = 0;
        final QueryMetadataPageRequest page1Request = new QueryMetadataPageRequest( Collections.EMPTY_MAP,
                null,
                null,
                null,
                null,
                startIndex,
                PAGE_SIZE );
        final PageResponse<SearchPageRow> page1Results = searchService.queryMetadata( page1Request );
        assertTrue( page1Results.isFirstPage() );
        assertFalse( page1Results.isLastPage() );
        assertEquals( PAGE_SIZE,
                page1Results.getPageRowList().size() );
        assertEquals( SIZE,
                page1Results.getTotalRowSize() );
        assertTrue( page1Results.isTotalRowSizeExact() );
        for ( int i = 0; i < PAGE_SIZE; i++ ) {
            assertEquals( vfsPath[startIndex + i].getFileName(),
                    page1Results.getPageRowList().get( i ).getPath().getFileName() );
        }

        //Perform search - Page 2
        startIndex = startIndex + page1Results.getPageRowList().size();
        final QueryMetadataPageRequest page2Request = new QueryMetadataPageRequest( Collections.EMPTY_MAP,
                null,
                null,
                null,
                null,
                startIndex,
                PAGE_SIZE );
        final PageResponse<SearchPageRow> page2Results = searchService.queryMetadata( page2Request );
        assertFalse( page2Results.isFirstPage() );
        assertFalse( page2Results.isLastPage() );
        assertEquals( PAGE_SIZE,
                page2Results.getPageRowList().size() );
        assertEquals( SIZE,
                page2Results.getTotalRowSize() );
        assertTrue( page2Results.isTotalRowSizeExact() );
        for ( int i = 0; i < PAGE_SIZE; i++ ) {
            assertEquals( vfsPath[startIndex + i].getFileName(),
                    page2Results.getPageRowList().get( i ).getPath().getFileName() );
        }

        //Perform search - Page 3
        startIndex = startIndex + page2Results.getPageRowList().size();
        final QueryMetadataPageRequest page3Request = new QueryMetadataPageRequest( Collections.EMPTY_MAP,
                null,
                null,
                null,
                null,
                startIndex,
                PAGE_SIZE );
        final PageResponse<SearchPageRow> page3Results = searchService.queryMetadata( page3Request );
        assertFalse( page3Results.isFirstPage() );
        assertTrue( page3Results.isLastPage() );
        assertEquals( 3,
                page3Results.getPageRowList().size() );
        assertEquals( SIZE,
                page3Results.getTotalRowSize() );
        assertTrue( page3Results.isTotalRowSizeExact() );
        for ( int i = 0; i < 3; i++ ) {
            assertEquals( vfsPath[startIndex + i].getFileName(),
                    page3Results.getPageRowList().get( i ).getPath().getFileName() );
        }
    }

    @Test
    public void testMetadataSearchLpr() {
        //Setup access rights - Grant access to all OUs
        when( authorizationManager.authorize( ou1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( ou2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to all Repositories
        when( authorizationManager.authorize( repo1,
                identity ) ).thenReturn( true );
        when( authorizationManager.authorize( repo2,
                identity ) ).thenReturn( true );

        //Setup access rights - Grant access to Project1
        when( authorizationManager.authorize( project1,
                identity ) ).thenReturn( true );

        final LprMetaAttributesMock lprMock = new LprMetaAttributesMock();
        final Map<String, Object> searchAttrs = new HashMap<String, Object>() {{
            String wildCardErrorNumber = "?" + lprMock.errorNumber().toString().substring( 1 );
            String[] errorTextWords = lprMock.errorText().split( "\\s+" );
            put( SEARCH_IS_PRODUCTION, lprMock.hasProdVersion() );
            put( SEARCH_REPORT_RECEIVED_DATE, new Date( lprMock.reportReceivedFromDate() ) );
            put( SEARCH_ENCOUNTER_START_DATE, new Date( lprMock.encounterStartToDate() ) );
            put( SEARCH_ENCOUNTER_END_DATE, new Date( lprMock.encounterEndFromDate() ) );
            put( SEARCH_EPISODE_OF_CARE_START_DATE, new Date( lprMock.episodeOfCareStartFromDate() ) );
            put( ERROR_NUMBER, wildCardErrorNumber );
            put( ERROR_TEXT, errorTextWords );
            put( ERROR_TYPE, lprMock.errorType() );
            put( RULE_GROUP, lprMock.ruleGroup() );
            put( IS_VALID_FOR_LPR_REPORTS, true );
            put( IS_VALID_FOR_DUSAS_ABROAD_REPORTS, true );
            put( IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS, true );
            put( IS_VALID_FOR_PRIVATE_SECTOR_REPORTS, false );
        }};

        final QueryMetadataPageRequest pageRequest = new QueryMetadataPageRequest( searchAttrs,
                null,
                null,
                null,
                null,
                0,
                5 );

        //Setup search
        final org.uberfire.backend.vfs.Path vfsPath = PathFactory.newPath( "file1", "default://project1/file1" );
        final Path nioPath = Paths.convert( vfsPath );
        final KObject kObject = mock( KObject.class );

        when( kObject.getKey() ).thenReturn( "default://project1/file1" );
        when( ioSearchService.searchByAttrsHits( any( Map.class ),
                Matchers.<Path>anyVararg() ) ).thenReturn( 1 );
        when( ioSearchService.searchByAttrs( any( Map.class ),
                any( SearchServiceImpl.PagedCountingFilter.class ),
                any( Sort.class ),
                Matchers.<Path>anyVararg() ) ).thenAnswer( new Answer<List<Path>>() {
            @Override
            public List<Path> answer( final InvocationOnMock invocation ) throws Throwable {
                final SearchServiceImpl.PagedCountingFilter filter = ( SearchServiceImpl.PagedCountingFilter ) invocation.getArguments()[1];
                final List<Path> result = new ArrayList<Path>();
                if ( filter.accept( kObject ) ) {
                    result.add( nioPath );
                }
                return result;
            }
        } );

        when( projectService.resolveProject( any( org.uberfire.backend.vfs.Path.class ) ) ).thenReturn( project1 );

        final DublinCoreView dublinCoreView = mock( DublinCoreView.class );
        final OtherMetaView otherMetaView = mock( OtherMetaView.class );
        final VersionAttributeView versionAttributeView = mock( VersionAttributeView.class );
        final LprMetaView lprMetaView = mock( LprMetaView.class );
        when( dublinCoreView.readAttributes() ).thenReturn( new DublinCoreAttributesMock() );
        when( versionAttributeView.readAttributes() ).thenReturn( new VersionAttributesMock( Collections.EMPTY_LIST ) );
        when( lprMetaView.readAttributes() ).thenReturn( lprMock );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( DublinCoreView.class ) ) ).thenReturn( dublinCoreView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( OtherMetaView.class ) ) ).thenReturn( otherMetaView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( VersionAttributeView.class ) ) ).thenReturn( versionAttributeView );
        when( ioService.getFileAttributeView( any( Path.class ),
                eq( LprMetaView.class ) ) ).thenReturn( lprMetaView );

        //Perform search
        final PageResponse<SearchPageRow> results = searchService.queryMetadata( pageRequest );
        assertEquals( 1,
                results.getTotalRowSize() );
        assertEquals( vfsPath.getFileName(),
                results.getPageRowList().get( 0 ).getPath().getFileName() );

        ArgumentCaptor<Map> attrsArgumentCaptor = ArgumentCaptor.forClass( Map.class );
        verify( ioSearchService ).searchByAttrsHits( attrsArgumentCaptor.capture(), Matchers.<Path>anyVararg() );
        Map capturedArgument = attrsArgumentCaptor.getValue();
        //These should be passed through, as is
        assertEquals( searchAttrs.get( ERROR_NUMBER ), capturedArgument.get( ERROR_NUMBER ) );
        assertEquals( searchAttrs.get( ERROR_TEXT ), capturedArgument.get( ERROR_TEXT ) );
        assertEquals( (( LprErrorType ) searchAttrs.get( ERROR_TYPE )).getId(), capturedArgument.get( ERROR_TYPE ) );
        assertEquals( (( LprRuleGroup ) searchAttrs.get( RULE_GROUP )).getId(), capturedArgument.get( RULE_GROUP ) );
        assertEquals( searchAttrs.get( IS_VALID_FOR_LPR_REPORTS ), capturedArgument.get( IS_VALID_FOR_LPR_REPORTS ) );
        assertEquals( searchAttrs.get( IS_VALID_FOR_DUSAS_ABROAD_REPORTS ), capturedArgument.get( IS_VALID_FOR_DUSAS_ABROAD_REPORTS ) );
        assertEquals( searchAttrs.get( IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS ), capturedArgument.get( IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS ) );
        assertEquals( searchAttrs.get( IS_VALID_FOR_PRIVATE_SECTOR_REPORTS ), capturedArgument.get( IS_VALID_FOR_PRIVATE_SECTOR_REPORTS ) );

        //each search date should be transformed into two date ranges
        Date searchDate = ( Date ) searchAttrs.get( SEARCH_REPORT_RECEIVED_DATE );
        DateRange dateRange = ( DateRange ) capturedArgument.get( REPORT_RECEIVED_FROM_DATE );
        assertTrue( dateRange.before().equals( searchDate ) );
        assertTrue( dateRange.after().equals( new Date( Long.MIN_VALUE ) ) );
        dateRange = ( DateRange ) capturedArgument.get( REPORT_RECEIVED_TO_DATE );
        assertTrue( dateRange.before().equals( new Date( Long.MAX_VALUE ) ) );
        assertTrue( dateRange.after().equals( searchDate ) );

        searchDate = ( Date ) searchAttrs.get( SEARCH_ENCOUNTER_START_DATE );
        dateRange = ( DateRange ) capturedArgument.get( ENCOUNTER_START_FROM_DATE );
        assertTrue( dateRange.before().equals( searchDate ) );
        assertTrue( dateRange.after().equals( new Date( Long.MIN_VALUE ) ) );
        dateRange = ( DateRange ) capturedArgument.get( ENCOUNTER_START_TO_DATE );
        assertTrue( dateRange.before().equals( new Date( Long.MAX_VALUE ) ) );
        assertTrue( dateRange.after().equals( searchDate ) );

        searchDate = ( Date ) searchAttrs.get( SEARCH_ENCOUNTER_END_DATE );
        dateRange = ( DateRange ) capturedArgument.get( ENCOUNTER_END_FROM_DATE );
        assertTrue( dateRange.before().equals( searchDate ) );
        assertTrue( dateRange.after().equals( new Date( Long.MIN_VALUE ) ) );
        dateRange = ( DateRange ) capturedArgument.get( ENCOUNTER_END_TO_DATE );
        assertTrue( dateRange.before().equals( new Date( Long.MAX_VALUE ) ) );
        assertTrue( dateRange.after().equals( searchDate ) );

        searchDate = ( Date ) searchAttrs.get( SEARCH_EPISODE_OF_CARE_START_DATE );
        dateRange = ( DateRange ) capturedArgument.get( EPISODE_OF_CARE_START_FROM_DATE );
        assertTrue( dateRange.before().equals( searchDate ) );
        assertTrue( dateRange.after().equals( new Date( Long.MIN_VALUE ) ) );
        dateRange = ( DateRange ) capturedArgument.get( EPISODE_OF_CARE_START_TO_DATE );
        assertTrue( dateRange.before().equals( new Date( Long.MAX_VALUE ) ) );
        assertTrue( dateRange.after().equals( searchDate ) );
    }
}

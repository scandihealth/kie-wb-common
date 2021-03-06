/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.backend.builder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.drools.core.rule.TypeMetaInfo;
import org.guvnor.common.services.project.builder.model.BuildMessage;
import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.builder.service.BuildService;
import org.guvnor.common.services.project.builder.service.BuildValidationHelper;
import org.guvnor.common.services.project.model.POM;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.scanner.KieModuleMetaData;
import org.kie.workbench.common.services.backend.common.Predicate;
import org.kie.workbench.common.services.backend.validation.asset.DefaultGenericKieValidator;
import org.kie.workbench.common.services.backend.whitelist.PackageNameSearchProvider;
import org.kie.workbench.common.services.backend.whitelist.PackageNameWhiteListLoader;
import org.kie.workbench.common.services.backend.whitelist.PackageNameWhiteListSaver;
import org.kie.workbench.common.services.backend.whitelist.PackageNameWhiteListServiceImpl;
import org.kie.workbench.common.services.shared.project.KieProjectService;
import org.kie.workbench.common.services.shared.project.ProjectImportsService;
import org.kie.workbench.common.services.shared.whitelist.PackageNameWhiteListService;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.fs.file.SimpleFileSystemProvider;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BuilderTest
        extends BuilderTestBase {

    private final Predicate<String> alwaysTrue = new Predicate<String>() {
        @Override
        public boolean apply( final String o ) {
            return true;
        }
    };

    @Mock
    private PackageNameSearchProvider packageNameSearchProvider;

    private IOService ioService;
    private KieProjectService projectService;
    private ProjectImportsService importsService;
    private LRUProjectDependenciesClassLoaderCache dependenciesClassLoaderCache;
    private LRUPomModelCache pomModelCache;
    private BuildService buildService;
    private DefaultGenericKieValidator validator;

    @BeforeClass
    public static void setupSystemProperties() {
        //These are not needed for the tests
        System.setProperty( "org.uberfire.nio.git.daemon.enabled",
                            "false" );
        System.setProperty( "org.uberfire.nio.git.ssh.enabled",
                            "false" );
        System.setProperty( "org.uberfire.sys.repo.monitor.disabled",
                            "true" );
    }

    @Before
    public void setUp() throws Exception {
        PackageNameSearchProvider.PackageNameSearch nameSearch = mock( PackageNameSearchProvider.PackageNameSearch.class );
        when( nameSearch.search() ).thenReturn( new HashSet<String>() );
        when( packageNameSearchProvider.newTopLevelPackageNamesSearch( any( POM.class ) ) ).thenReturn( nameSearch );
        startMain();
        setUpGuvnorM2Repo();

        ioService = getReference( IOService.class );
        projectService = getReference( KieProjectService.class );
        importsService = getReference( ProjectImportsService.class );
        dependenciesClassLoaderCache = getReference( LRUProjectDependenciesClassLoaderCache.class );
        pomModelCache = getReference( LRUPomModelCache.class );
        buildService = getReference( BuildService.class );
        validator = getReference( DefaultGenericKieValidator.class );
    }

    @Test
    public void testBuilderSimpleKProject() throws Exception {
        LRUPomModelCache pomModelCache = getReference( LRUPomModelCache.class );

        URL url = this.getClass().getResource( "/GuvnorM2RepoDependencyExample1" );
        SimpleFileSystemProvider p = new SimpleFileSystemProvider();
        org.uberfire.java.nio.file.Path path = p.getPath( url.toURI() );

        final Project project = projectService.resolveProject( Paths.convert( path ) );

        final Builder builder = new Builder( project,
                                             ioService,
                                             projectService,
                                             importsService,
                                             new ArrayList<BuildValidationHelper>(),
                                             dependenciesClassLoaderCache,
                                             pomModelCache,
                                             getPackageNameWhiteListService(),
                                             alwaysTrue );

        assertNotNull( builder.getKieContainer() );
    }

    @Test
    public void testBuilderKProjectHasDependency() throws Exception {
        URL url = this.getClass().getResource( "/GuvnorM2RepoDependencyExample2" );
        SimpleFileSystemProvider p = new SimpleFileSystemProvider();
        org.uberfire.java.nio.file.Path path = p.getPath( url.toURI() );

        final Project project = projectService.resolveProject( Paths.convert( path ) );

        final Builder builder = new Builder( project,
                                             ioService,
                                             projectService,
                                             importsService,
                                             new ArrayList<BuildValidationHelper>(),
                                             dependenciesClassLoaderCache,
                                             pomModelCache,
                                             getPackageNameWhiteListService(),
                                             alwaysTrue );

        final BuildResults results = builder.build();

        //Debug output
        if ( !results.getMessages().isEmpty() ) {
            for ( BuildMessage m : results.getMessages() ) {
                System.out.println( m.getText() );
            }
        }

        assertTrue( results.getMessages().isEmpty() );
    }

    @Test
    public void testBuilderKProjectHasSnapshotDependency() throws Exception {
        URL url = this.getClass().getResource( "/GuvnorM2RepoDependencyExample2Snapshot" );
        SimpleFileSystemProvider p = new SimpleFileSystemProvider();
        org.uberfire.java.nio.file.Path path = p.getPath( url.toURI() );

        final Project project = projectService.resolveProject( Paths.convert( path ) );

        final Builder builder = new Builder( project,
                                             ioService,
                                             projectService,
                                             importsService,
                                             new ArrayList<BuildValidationHelper>(),
                                             dependenciesClassLoaderCache,
                                             pomModelCache,
                                             getPackageNameWhiteListService(),
                                             alwaysTrue );

        final BuildResults results = builder.build();

        //Debug output
        if ( !results.getMessages().isEmpty() ) {
            for ( BuildMessage m : results.getMessages() ) {
                System.out.println( m.getText() );
            }
        }

        assertTrue( results.getMessages().isEmpty() );
    }

    @Test
    public void testBuilderKProjectHasDependencyMetaData() throws Exception {
        URL url = this.getClass().getResource( "/GuvnorM2RepoDependencyExample2" );
        SimpleFileSystemProvider p = new SimpleFileSystemProvider();
        org.uberfire.java.nio.file.Path path = p.getPath( url.toURI() );

        final Project project = projectService.resolveProject( Paths.convert( path ) );

        final Builder builder = new Builder( project,
                                             ioService,
                                             projectService,
                                             importsService,
                                             new ArrayList<BuildValidationHelper>(),
                                             dependenciesClassLoaderCache,
                                             pomModelCache,
                                             getPackageNameWhiteListService(),
                                             alwaysTrue );

        final BuildResults results = builder.build();

        //Debug output
        if ( !results.getMessages().isEmpty() ) {
            for ( BuildMessage m : results.getMessages() ) {
                System.out.println( m.getText() );
            }
        }

        assertTrue( results.getMessages().isEmpty() );

        final KieModuleMetaData metaData = KieModuleMetaData.Factory.newKieModuleMetaData( builder.getKieModule() );

        //Check packages
        final Set<String> packageNames = new HashSet<String>();
        final Iterator<String> packageNameIterator = metaData.getPackages().iterator();
        while ( packageNameIterator.hasNext() ) {
            packageNames.add( packageNameIterator.next() );
        }
        assertEquals( 2,
                      packageNames.size() );
        assertTrue( packageNames.contains( "defaultpkg" ) );
        assertTrue( packageNames.contains( "org.kie.workbench.common.services.builder.tests.test1" ) );

        //Check classes
        final String packageName = "org.kie.workbench.common.services.builder.tests.test1";
        assertEquals( 1,
                      metaData.getClasses( packageName ).size() );
        final String className = metaData.getClasses( packageName ).iterator().next();
        assertEquals( "Bean",
                      className );

        //Check metadata
        final Class clazz = metaData.getClass( packageName,
                                               className );
        final TypeMetaInfo typeMetaInfo = metaData.getTypeMetaInfo( clazz );
        assertNotNull( typeMetaInfo );
        assertFalse( typeMetaInfo.isEvent() );
    }

    @Test
    public void testKProjectContainsXLS() throws Exception {
        URL url = this.getClass().getResource( "/ExampleWithExcel" );
        SimpleFileSystemProvider p = new SimpleFileSystemProvider();
        org.uberfire.java.nio.file.Path path = p.getPath( url.toURI() );

        final Project project = projectService.resolveProject( Paths.convert( path ) );

        final Builder builder = new Builder( project,
                                             ioService,
                                             projectService,
                                             importsService,
                                             new ArrayList<BuildValidationHelper>(),
                                             dependenciesClassLoaderCache,
                                             pomModelCache,
                                             getPackageNameWhiteListService(),
                                             alwaysTrue );

        final BuildResults results = builder.build();

        //Debug output
        if ( !results.getMessages().isEmpty() ) {
            for ( BuildMessage m : results.getMessages() ) {
                System.out.println( m.getText() );
            }
        }

        assertTrue( results.getMessages().isEmpty() );
    }

    @Test
    public void testBuilderFixForBrokenKProject() throws Exception {

        LRUPomModelCache pomModelCache = getReference( LRUPomModelCache.class );

        SimpleFileSystemProvider provider = new SimpleFileSystemProvider();
        org.uberfire.java.nio.file.Path path = provider.getPath( this.getClass().getResource( "/BuilderExampleBrokenSyntax" ).toURI() );

        final Project project = projectService.resolveProject( Paths.convert( path ) );

        final Builder builder = new Builder( project,
                                             ioService,
                                             projectService,
                                             importsService,
                                             new ArrayList<BuildValidationHelper>(),
                                             dependenciesClassLoaderCache,
                                             pomModelCache,
                                             mock( PackageNameWhiteListService.class ),
                                             alwaysTrue );

        assertNull( builder.getKieContainer() );

        builder.deleteResource( provider.getPath( this.getClass().getResource( File.separatorChar + "BuilderExampleBrokenSyntax" +
                                                                                       File.separatorChar + "src" +
                                                                                       File.separatorChar + "main" +
                                                                                       File.separatorChar + "resources" +
                                                                                       File.separatorChar + "rule1.drl"
                                                                             ).toURI() ) );

        assertNotNull( builder.getKieContainer() );
    }

    @Test
    public void testBuilderKieContainerInstantiation() throws Exception {

        final URL url = this.getClass().getResource( "/GuvnorM2RepoDependencyExample1" );
        final SimpleFileSystemProvider p = new SimpleFileSystemProvider();
        final org.uberfire.java.nio.file.Path path = p.getPath( url.toURI() );

        final Project project = projectService.resolveProject( Paths.convert( path ) );

        //Build Project, including Rules and Global definition
        final Builder builder = new Builder( project,
                                             ioService,
                                             projectService,
                                             importsService,
                                             new ArrayList<BuildValidationHelper>(),
                                             dependenciesClassLoaderCache,
                                             pomModelCache,
                                             getPackageNameWhiteListService(),
                                             alwaysTrue );

        assertNotNull( builder.getKieContainer() );

        //Validate Rule excluding Global definition
        final URL urlToValidate = this.getClass().getResource( "/GuvnorM2RepoDependencyExample1/src/main/resources/rule2.drl" );
        final org.uberfire.java.nio.file.Path pathToValidate = p.getPath( urlToValidate.toURI() );
        final List<ValidationMessage> validationMessages = validator.validate( Paths.convert( pathToValidate ),
                                                                               Resources.toString( urlToValidate, Charsets.UTF_8 ) );
        assertNotNull( validationMessages );
        assertEquals( 0,
                      validationMessages.size() );

        // Retrieve a KieSession for the Project and set the Global. This should not fail as the
        // KieContainer is retrieved direct from the KieBuilder and not KieRepository (as was the
        // case before BZ1202551 was fixed.
        final KieContainer kieContainer1 = builder.getKieContainer();
        final KieSession kieSession1 = kieContainer1.newKieSession();
        kieSession1.setGlobal( "list",
                               new ArrayList<String>() );
    }

    private PackageNameWhiteListService getPackageNameWhiteListService() {
        return new PackageNameWhiteListServiceImpl( ioService,
                                                    mock( KieProjectService.class ),
                                                    new PackageNameWhiteListLoader( packageNameSearchProvider,
                                                                                    ioService ),
                                                    mock( PackageNameWhiteListSaver.class ) );
    }
}
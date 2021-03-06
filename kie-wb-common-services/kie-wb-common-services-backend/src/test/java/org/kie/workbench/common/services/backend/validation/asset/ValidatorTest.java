/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.services.backend.validation.asset;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.guvnor.common.services.shared.message.Level;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.guvnor.test.TestFileSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ValidatorTest {

    @Mock
    Path path;

    private TestFileSystem testFileSystem;

    private DefaultGenericKieValidator validator;
    private ValidatorBuildService validatorBuildService;

    @Before
    public void setUp() throws Exception {
        testFileSystem = new TestFileSystem();
        validatorBuildService = testFileSystem.getReference( ValidatorBuildService.class );
        validator = new DefaultGenericKieValidator( validatorBuildService );
    }

    @After
    public void tearDown() throws Exception {
        testFileSystem.tearDown();
    }

    @Test
    public void testValidateWithAValidDRLFile() throws Throwable {
        final Path path = path( "/GuvnorM2RepoDependencyExample1/src/main/resources/rule2.drl" );
        final String content = "package org.kie.workbench.common.services.builder.tests.test1\n" +
                "\n" +
                "rule R2\n" +
                "when\n" +
                "Bean()\n" +
                "then\n" +
                "end";

        List<ValidationMessage> errors = validator.validate( path, content );

        assertTrue( errors.isEmpty() );
    }

    @Test
    public void testValidateWithAInvalidDRLFile() throws Throwable {
        final Path path = path( "/GuvnorM2RepoDependencyExample1/src/main/resources/rule2.drl" );
        final String content = "package org.kie.workbench.common.services.builder.tests.test1\n" +
                "\n" +
                "rule R2\n" +
                "when\n" +
                "Ban()\n" +
                "then\n" +
                "end";

        List<ValidationMessage> errors = validator.validate( path, content );

        assertFalse( errors.isEmpty() );
    }

    @Test
    public void testValidateWithAValidJavaFile() throws Throwable {
        final Path path1 = path( "/GuvnorM2RepoDependencyExample1/src/main/java/org/kie/workbench/common/services/builder/tests/test1/Bean.java" );
        final String content = "package org.kie.workbench.common.services.builder.tests.test1;\n" +
                "\n" +
                "public class Bean {\n" +
                "    private final int value;\n" +
                "\n" +
                "    public Bean(int value) {\n" +
                "        this.value = value;\n" +
                "    }\n" +
                "\n" +
                "}";

        List<ValidationMessage> validate = validator.validate( path1, content );

        assertTrue( validate.isEmpty() );
    }

    @Test
    public void testValidateWithAInvalidJavaFile() throws Throwable {
        final Path path1 = path( "/GuvnorM2RepoDependencyExample1/src/main/java/org/kie/workbench/common/services/builder/tests/test1/Bean.java" );
        final String content = "package org.kie.workbench.common.services.builder.tests.test1;\n" +
                "\n" +
                "public class Bean {\n" +
                "    private fnal int value;\n" +
                "\n" +
                "}\n";

        List<ValidationMessage> validate = validator.validate( path1, content );

        assertFalse( validate.isEmpty() );
    }

    @Test
    public void testValidateWhenTheresNoProject() throws Exception {
        Path path = path( "/META-INF/beans.xml" );
        URL urlToValidate = this.getClass().getResource( "/META-INF/beans.xml" );

        List<ValidationMessage> errors = validator.validate( path, Resources.toString( urlToValidate, Charsets.UTF_8 ) );

        assertTrue( errors.isEmpty() );
    }

    @Test
    public void testFilterMessageWhenMessageIsInvalid() throws Throwable {
        Path path = path( "/GuvnorM2RepoDependencyExample1/src/main/resources/rule2.drl" );
        ValidationMessage errorMessage = errorMessage( path( "/GuvnorM2RepoDependencyExample1/src/main/resources/rule1.drl" ) );

        assertFalse( validator.isValidPath( path, errorMessage ) );
    }

    @Test
    public void testFilterMessageWhenMessageIsValid() throws Throwable {
        Path path = path( "/GuvnorM2RepoDependencyExample1/src/main/resources/rule2.drl" );
        ValidationMessage errorMessage = errorMessage( path );

        assertTrue( validator.isValidPath( path, errorMessage ) );
    }

    @Test
    public void testFilterMessageWhenMessageIsBlank() throws Throwable {
        Path path = path( "/GuvnorM2RepoDependencyExample1/src/main/resources/rule2.drl" );
        ValidationMessage errorMessage = errorMessage( null );

        assertTrue( validator.isValidPath( path, errorMessage ) );
    }

    private ValidationMessage errorMessage( Path path ) {
        return new ValidationMessage( 0, Level.ERROR, path, 0, 0, null );
    }

    private Path path( final String resourceName ) throws URISyntaxException {
        final URL urlToValidate = this.getClass().getResource( resourceName );
        return Paths.convert( testFileSystem.fileSystemProvider.getPath( urlToValidate.toURI() ) );
    }

}

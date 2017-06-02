package org.kie.workbench.common.screens.search.backend.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.guvnor.common.services.backend.metadata.DublinCoreAttributesMock;
import org.guvnor.common.services.backend.metadata.LprMetaAttributesMock;
import org.guvnor.common.services.backend.metadata.attribute.LprMetaAttributesUtil;
import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.junit.Test;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.model.KProperty;
import org.uberfire.ext.metadata.search.ClusterSegment;
import org.uberfire.ext.metadata.search.IOSearchService;
import org.uberfire.io.attribute.DublinCoreAttributesUtil;
import org.uberfire.java.nio.base.FileSystemId;
import org.uberfire.java.nio.base.SegmentedPath;
import org.uberfire.java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Created on 29-05-2017.
 */
public class SearchLprMetadataTest extends BaseIndexTest {
    private static final String REPO_NAME = "temp-repo-search-lpr-metadata-test";

    @Override
    protected String[] getRepositoryNames() {
        return new String[]{REPO_NAME};
    }

    @Test
    public void testSearch() throws IOException, InterruptedException {
        //SETUP
        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile01.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile02.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile03.txt" );

        Map<String, Object> attrs = getAttributesMap();

        //Rule with just default Mock attributes
        ioService().write( path1, "Rule1", attrs );

        //Rule with completely different attributes
        attrs.put( "lprmeta.type", "NORMAL" );
        attrs.put( "lprmeta.errorNumber", 1L );
        attrs.put( "lprmeta.isdraft", true );
        attrs.put( "lprmeta.inproduction", false );
        attrs.put( "lprmeta.errorText", "some error text" );
        attrs.put( "lprmeta.errorType", LprErrorType.WARNING.name() );
        attrs.put( "lprmeta.ruleGroup", "LPR.MOBST" );
        attrs.put( "lprmeta.ruleValidFromDate", 400L );
        attrs.put( "lprmeta.ruleValidToDate", 500L );
        ioService().write( path2, "Rule2", attrs );

        //Rule with similar attributes
        attrs.put( "lprmeta.errorNumber", 2L );
        attrs.put( "lprmeta.inproduction", true );
        ioService().write( path3, "Rule3", attrs );

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index

        //ASSERTIONS
        Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( "lprmeta.errorNumber", "1" );
        }};

        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyResults( results, "lprmeta.errorNumber", "1" );
        }

        searchAttributes = new HashMap<String, Object>() {{
            put( "lprmeta.errorNumber", "2" );
            put( "lprmeta.isdraft", true );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyResults( results, "lprmeta.errorNumber", "2" );
            verifyResults( results, "lprmeta.isdraft", "0" ); //true is represented as "0" in KObject
        }

        searchAttributes = new HashMap<String, Object>() {{
            put( "lprmeta.isdraft", true );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 2, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 2, results.size() );
            verifyResults( results, "lprmeta.isdraft", "0" ); //true is represented as "0" in KObject
        }

        searchAttributes = new HashMap<String, Object>() {{
            put( "lprmeta.inproduction", true );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyResults( results, "lprmeta.inproduction", "0" ); //true is represented as "0" in KObject
            verifyResults( results, "lprmeta.errorNumber", "2" );
        }

        searchAttributes = new HashMap<String, Object>() {{
            put( "lprmeta.errorNumber", "3" );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 0, hits );
        }

        searchAttributes = new HashMap<String, Object>() {{
            put( "lprmeta.errorText", LprMetaAttributesMock.TEST_ERROR_TEXT );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyResults( results, "lprmeta.errorText", LprMetaAttributesMock.TEST_ERROR_TEXT );
        }

        searchAttributes = new HashMap<String, Object>() {{
            put( "lprmeta.errorType", LprErrorType.ERROR.name() );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyResults( results, "lprmeta.errorType", LprErrorType.ERROR.name() );
        }

        searchAttributes = new HashMap<String, Object>() {{
            put( "lprmeta.ruleGroup", LprMetaAttributesMock.TEST_RULE_GROUP );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyResults( results, "lprmeta.ruleGroup", LprMetaAttributesMock.TEST_RULE_GROUP );
        }

        //todo ttn test date ranges metadata
    }

    private void verifyResults( List<KObject> results, String metadataKey, String metadataValue ) {
        for ( KObject result : results ) {
            boolean found = false;
            for ( KProperty<?> property : result.getProperties() ) {
                if ( metadataKey.equals( property.getName() ) ) {
                    assertEquals( metadataValue, property.getValue() );
                    assertFalse( "rule had metadata " + metadataKey + " more than once", found );
                    found = true;
                }
            }
            assertTrue( "rule did not have metadata " + metadataKey, found );
        }
    }


    private ClusterSegment getClusterSegment() {
        return new ClusterSegment() {
            @Override
            public String getClusterId() {
                return (( FileSystemId ) getBasePath( REPO_NAME ).getFileSystem()).id();
            }

            @Override
            public String[] segmentIds() {
                return new String[]{(( SegmentedPath ) getBasePath( REPO_NAME )).getSegmentId()};
            }
        };
    }

    private Map<String, Object> getAttributesMap() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.putAll( DublinCoreAttributesUtil.toMap( new DublinCoreAttributesMock(), "*" ) );
        attrs.putAll( LprMetaAttributesUtil.toMap( new LprMetaAttributesMock(), "*" ) );
        return attrs;
    }
}


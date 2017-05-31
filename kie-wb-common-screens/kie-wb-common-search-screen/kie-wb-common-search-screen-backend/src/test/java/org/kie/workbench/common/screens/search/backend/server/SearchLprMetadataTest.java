package org.kie.workbench.common.screens.search.backend.server;

import org.guvnor.common.services.backend.metadata.DublinCoreAttributesMock;
import org.guvnor.common.services.backend.metadata.LprMetaAttributesMock;
import org.guvnor.common.services.backend.metadata.attribute.LprMetaAttributesUtil;
import org.junit.*;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.search.ClusterSegment;
import org.uberfire.ext.metadata.search.DateRange;
import org.uberfire.ext.metadata.search.IOSearchService;
import org.uberfire.io.attribute.DublinCoreAttributes;
import org.uberfire.io.attribute.DublinCoreAttributesUtil;
import org.uberfire.java.nio.base.FileSystemId;
import org.uberfire.java.nio.base.SegmentedPath;
import org.uberfire.java.nio.base.dotfiles.DotFileOption;
import org.uberfire.java.nio.base.options.CommentedOption;
import org.uberfire.java.nio.file.OpenOption;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.attribute.FileAttribute;
import org.uberfire.java.nio.file.attribute.FileTime;

import java.io.IOException;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

/**
 * Created by prc on 29-05-2017.
 */
public class SearchLprMetadataTest extends BaseIndexTest {
    private static final String REPO_NAME  = "temp-repo-search-lpr-metadata-test";

    //todo prc make it work

    @Override
    protected String[] getRepositoryNames() {
        return new String[]{ REPO_NAME };
    }

    @Test
    public void testSearchOnErrorNumber() throws IOException, InterruptedException {
        final String SEARCH_KEY = "lprmeta.errorNumber";

        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile01.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile02.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile03.txt" );

        Map<String, Object> attrs = getAttributesMap();

        ioService().write( path1, "aTestRule", attrs, new CommentedOption("lala"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, 200L);
        ioService().write( path2, "aTestRule", attrs, new CommentedOption("alal"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, 100L);
        ioService().write( path3, "aTestRule", attrs, new CommentedOption("Benny"));

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index


        final Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( SEARCH_KEY,
                    "200" );
        }};

        //Attribute Search
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment());
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes,
                    new IOSearchService.NoOpFilter(), getClusterSegment());
            assertEquals( 1,
                    hits );
            assertEquals( 1,
                    results.size() );
        }
    }

    @Test
    public void testSearchOnRuleType() throws IOException, InterruptedException {
        final String SEARCH_KEY = "lprmeta.type";

        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile11.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile12.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile13.txt" );

        Map<String, Object> attrs = getAttributesMap();

        ioService().write( path1, "aTestRule", attrs, new CommentedOption("lala"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, "NORMAL");
        ioService().write( path2, "aTestRule", attrs, new CommentedOption("alal"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, "RESULT_REPORTING");
        ioService().write( path3, "aTestRule", attrs, new CommentedOption("Benny"));

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index


        final Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( SEARCH_KEY,
                    "RESULT_REPORTING" );
        }};

        //Attribute Search
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment());
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes,
                    new IOSearchService.NoOpFilter(), getClusterSegment());
            assertEquals( 1,
                    hits );
            assertEquals( 1,
                    results.size() );
        }
    }

    @Test
    public void testSearchOnIsDraft() throws IOException, InterruptedException {
        final String SEARCH_KEY = "lprmeta.isdraft";

        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile21.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile22.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile23.txt" );

        Map<String, Object> attrs = getAttributesMap();

        ioService().write( path1, "aTestRule", attrs, new CommentedOption("lala"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, true);
        ioService().write( path2, "aTestRule", attrs, new CommentedOption("alal"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, false);
        ioService().write( path3, "aTestRule", attrs, new CommentedOption("Benny"));

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index


        final Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( SEARCH_KEY,
                    false );
        }};

        //Attribute Search
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment());
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes,
                    new IOSearchService.NoOpFilter(), getClusterSegment());
            assertEquals( 1,
                    hits );
            assertEquals( 1,
                    results.size() );
        }
    }

    @Test
    public void testSearchOnInProduction() throws IOException, InterruptedException {
        final String SEARCH_KEY = "lprmeta.inproduction";

        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile31.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile32.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile33.txt" );

        Map<String, Object> attrs = getAttributesMap();

        ioService().write( path1, "aTestRule", attrs, new CommentedOption("lala"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, true);
        ioService().write( path2, "aTestRule", attrs, new CommentedOption("alal"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, true);
        ioService().write( path3, "aTestRule", attrs, new CommentedOption("Benny"));

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index


        final Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( SEARCH_KEY,
                    false );
        }};

        //Attribute Search
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment());
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes,
                    new IOSearchService.NoOpFilter(), getClusterSegment());
            assertEquals( 1,
                    hits );
            assertEquals( 1,
                    results.size() );
        }
    }

    @Test
    public void testSearchOnErrorText() throws IOException, InterruptedException {
        final String SEARCH_KEY = "lprmeta.errorText";

        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile41.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile42.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile43.txt" );

        Map<String, Object> attrs = getAttributesMap();

        ioService().write( path1, "aTestRule", attrs, new CommentedOption("lala"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, "Dette er ikke en sjov text");
        ioService().write( path2, "aTestRule", attrs, new CommentedOption("alal"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, "Dette er nemlig en sjov text");
        ioService().write( path3, "aTestRule", attrs, new CommentedOption("Benny"));

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index


        final Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( SEARCH_KEY,
                    "Dette er ikke en sjov text" );
        }};

        //Attribute Search
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment());
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes,
                    new IOSearchService.NoOpFilter(), getClusterSegment());
            assertEquals( 1,
                    hits );
            assertEquals( 1,
                    results.size() );
        }
    }

    @Test
    public void testSearchOnErrorType() throws IOException, InterruptedException {
        final String SEARCH_KEY = "lprmeta.errorType";

        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile51.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile52.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile53.txt" );

        Map<String, Object> attrs = getAttributesMap();

        ioService().write( path1, "aTestRule", attrs, new CommentedOption("lala"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, "FATAL");
        ioService().write( path2, "aTestRule", attrs, new CommentedOption("alal"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, "WARNING");
        ioService().write( path3, "aTestRule", attrs, new CommentedOption("Benny"));

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index


        final Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( SEARCH_KEY,
                    "WARNING" );
        }};

        //Attribute Search
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment());
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes,
                    new IOSearchService.NoOpFilter(), getClusterSegment());
            assertEquals( 1,
                    hits );
            assertEquals( 1,
                    results.size() );
        }
    }

    @Test
    public void testSearchOnRuleGroup() throws IOException, InterruptedException {
        final String SEARCH_KEY = "lprmeta.ruleGroup";

        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile61.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile62.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile63.txt" );

        Map<String, Object> attrs = getAttributesMap();

        ioService().write( path1, "aTestRule", attrs, new CommentedOption("lala"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, "SecondRuleGroup");
        ioService().write( path2, "aTestRule", attrs, new CommentedOption("alal"));

        attrs.remove(SEARCH_KEY);
        attrs.put(SEARCH_KEY, "ThirdRuleGroup");
        ioService().write( path3, "aTestRule", attrs, new CommentedOption("Benny"));

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index


        final Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( SEARCH_KEY,
                    "ThirdRuleGroup" );
        }};

        //Attribute Search
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment());
//            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes,
//                    new IOSearchService.NoOpFilter(), getClusterSegment());
            assertEquals( 1,
                    hits );
//            assertEquals( 1,
//                    results.size() );
        }
    }

    @Test
    public void testSearchOnRuleValidDate() throws IOException, InterruptedException {
        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile71.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile72.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile73.txt" );

        Map<String, Object> attrs = getAttributesMap();

        ioService().write( path1, "aTestRule", attrs, new CommentedOption("lala"));

        attrs.remove("lprmeta.ruleValidFromDate");
        attrs.remove("lprmeta.ruleValidToDate");
        attrs.put("lprmeta.ruleValidFromDate", 400L);
        attrs.put("lprmeta.ruleValidToDate", 500L);


        ioService().write( path2, "aTestRule", attrs, new CommentedOption("alal"));

        attrs.remove("lprmeta.ruleValidFromDate");
        attrs.remove("lprmeta.ruleValidToDate");
        attrs.put("lprmeta.ruleValidFromDate", 600L);
        attrs.put("lprmeta.ruleValidToDate", 700L);
        ioService().write( path3, "aTestRule", attrs, new CommentedOption("Benny"));

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index


        final Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put("lprmeta.ruleValidDate",
                    new DateRange() {
                        @Override
                        public Date before() {
                            return new Date(475);
                        }

                        @Override
                        public Date after() {
                            return new Date(425);
                        }
                    });
        }};

        //Attribute Search
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment());
//            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes,
//                    new IOSearchService.NoOpFilter(), getClusterSegment());
            assertEquals( 1,
                    hits );
//            assertEquals( 1,
//                    results.size() );
        }
    }

    private ClusterSegment getClusterSegment() {
        return new ClusterSegment() {
            @Override
            public String getClusterId() {
                return ( (FileSystemId) getBasePath( REPO_NAME ).getFileSystem() ).id();
            }

            @Override
            public String[] segmentIds() {
                return new String[]{ ( (SegmentedPath) getBasePath( REPO_NAME ) ).getSegmentId() };
            }
        };
    }

    private Map<String, Object> getAttributesMap() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.putAll( DublinCoreAttributesUtil.toMap( new DublinCoreAttributesMock(), "*" ) );

        attrs.putAll( LprMetaAttributesUtil.toMap( new LprMetaAttributesMock(), "*"));

        return attrs;
    }
}


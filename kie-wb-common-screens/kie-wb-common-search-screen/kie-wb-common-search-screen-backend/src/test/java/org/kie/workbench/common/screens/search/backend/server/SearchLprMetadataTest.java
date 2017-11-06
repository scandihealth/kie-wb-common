package org.kie.workbench.common.screens.search.backend.server;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.guvnor.common.services.backend.metadata.DublinCoreAttributesMock;
import org.guvnor.common.services.backend.metadata.LprMetaAttributesMock;
import org.guvnor.common.services.backend.metadata.attribute.LprMetaAttributes;
import org.guvnor.common.services.backend.metadata.attribute.LprMetaAttributesUtil;
import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.guvnor.common.services.shared.metadata.model.LprMetadataConsts;
import org.guvnor.common.services.shared.metadata.model.LprRuleGroup;
import org.junit.Before;
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
import static org.kie.workbench.common.screens.search.backend.server.SearchServiceImpl.*;

/**
 * Created on 29-05-2017.
 */
public class SearchLprMetadataTest extends BaseIndexTest {
    private static final String REPO_NAME = "temp-repo-search-lpr-metadata-test";

    @Override
    protected String[] getRepositoryNames() {
        return new String[]{REPO_NAME};
    }

    @Before
    public void createRules() throws InterruptedException, CloneNotSupportedException {
        final Path path1 = getBasePath( REPO_NAME ).resolve( "indexedFile01.txt" );
        final Path path2 = getBasePath( REPO_NAME ).resolve( "indexedFile02.txt" );
        final Path path3 = getBasePath( REPO_NAME ).resolve( "indexedFile03.txt" );

        //A rule with just defaul1t Mock attributes
        LprMetaAttributesMock rule1 = new LprMetaAttributesMock();
        Map<String, Object> attrs1 = getAttributesMap( rule1 );
        ioService().write( path1, "Rule1", attrs1 );

        //A rule with completely different attributes
        LprMetaAttributesMock rule2 = new LprMetaAttributesMock();
        rule2.errorNumber = 100L;
        rule2.errorText = "other error text";
        rule2.errorType = LprErrorType.WARN;
        rule2.ruleGroup = LprRuleGroup.PSYKI;
        rule2.reportReceivedFromDate = 150L;
        rule2.reportReceivedToDate = 250L;
        rule2.encounterStartFromDate = 0L;
        rule2.encounterStartToDate = Long.MAX_VALUE;
        rule2.encounterEndFromDate = 0L;
        rule2.encounterEndToDate = 750L;
        rule2.episodeOfCareStartFromDate = 200L;
        rule2.episodeOfCareStartToDate = Long.MAX_VALUE;
        rule2.productionDate = 700L;
        rule2.archivedDate = 800L;
        rule2.hasProdVersion = true;
        rule2.isValidForLPRReports = false;
        rule2.isValidForDUSASAbroadReports = true;
        rule2.isValidForDUSASSpecialityReports = false;
        rule2.isValidForPrimarySectorReports = false;
        Map<String, Object> attrs2 = getAttributesMap( rule2 );
        ioService().write( path2, "Rule2", attrs2 );

        //A rule with mostly similar attributes
        LprMetaAttributesMock rule3 = ( LprMetaAttributesMock ) rule2.clone();
        rule3.errorNumber = 101L;
        rule3.errorText = "other, but similar, error text";
        rule3.reportReceivedFromDate = 1000L;
        rule3.reportReceivedToDate = 2000L;
        rule3.hasProdVersion = false;
        rule3.productionDate = 0L;
        rule3.archivedDate = 0L;
        Map<String, Object> attrs3 = getAttributesMap( rule3 );
        ioService().write( path3, "Rule3", attrs3 );

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index
    }


    /**
     * For some reason, BaseIndexTest cannot handle multiple tests in a test class. Therefore there is only 1 test method that is a long scenario test
     */
    @SuppressWarnings("Duplicates")
    @Test
    public void testSearch() {
        //Single field
        Map<String, Object> searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ERROR_NUMBER, "1" );
        }};

        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAll( results, LprMetadataConsts.ERROR_NUMBER, "1" );
        }

        //Multiple fields
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ERROR_NUMBER, "1" );
            put( LprMetadataConsts.IS_VALID_FOR_LPR_REPORTS, true );
            put( LprMetadataConsts.IS_VALID_FOR_PRIMARY_SECTOR_REPORTS, true );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAll( results, LprMetadataConsts.ERROR_NUMBER, "1" );
            verifyAll( results, LprMetadataConsts.IS_VALID_FOR_LPR_REPORTS, "0" ); //true is represented as "0" in KObject
            verifyAll( results, LprMetadataConsts.IS_VALID_FOR_PRIMARY_SECTOR_REPORTS, "0" ); //true is represented as "0" in KObject
            verifyAll( results, LprMetadataConsts.IS_VALID_FOR_DUSAS_ABROAD_REPORTS, "1" ); //false is represented as "1" in KObject
        }

        //Multple hits
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.IS_VALID_FOR_DUSAS_ABROAD_REPORTS, true );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 2, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 2, results.size() );
            verifyAll( results, LprMetadataConsts.IS_VALID_FOR_DUSAS_ABROAD_REPORTS, "0" ); //true is represented as "0" in KObject
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "100" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "101" );
        }

        //No hits
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS, true );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 0, hits );
        }

        //Wildcard error number search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ERROR_NUMBER, "1*" );
        }};

        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 3, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 3, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "1" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "100" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "101" );
        }

        //Wildcard error number search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ERROR_NUMBER, "1??" );
        }};

        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 2, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 2, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "100" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "101" );
        }

        //error text search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ERROR_TEXT, "other" );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 2, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 2, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_TEXT, "other error text" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_TEXT, "other, but similar, error text" );
        }

        //error text no hits
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ERROR_TEXT, "oth" );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 0, hits );
        }

        //Wildcard error text search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ERROR_TEXT, "oth*" );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 2, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 2, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_TEXT, "other error text" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_TEXT, "other, but similar, error text" );
        }

        //Report received search (start of 1st interval)
        searchAttributes = new HashMap<String, Object>() {{
            Date reportReceivedDate = new Date( 100 );
            put( LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, toDateRange( reportReceivedDate, new Date( 0L ) ) );
            put( LprMetadataConsts.REPORT_RECEIVED_TO_DATE, toDateRange( new Date( Long.MAX_VALUE ), reportReceivedDate ) );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAll( results, LprMetadataConsts.ERROR_NUMBER, "1" );
            verifyAll( results, LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, "100" );
            verifyAll( results, LprMetadataConsts.REPORT_RECEIVED_TO_DATE, "200" );
        }

        //Report received search (middle of 1st interval)
        searchAttributes = new HashMap<String, Object>() {{
            Date reportReceivedDate = new Date( 120 );
            put( LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, toDateRange( reportReceivedDate, new Date( 0L ) ) );
            put( LprMetadataConsts.REPORT_RECEIVED_TO_DATE, toDateRange( new Date( Long.MAX_VALUE ), reportReceivedDate ) );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAll( results, LprMetadataConsts.ERROR_NUMBER, "1" );
            verifyAll( results, LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, "100" );
            verifyAll( results, LprMetadataConsts.REPORT_RECEIVED_TO_DATE, "200" );
        }

        //Report received search (middle of 1st interval start of 2nd interval)
        searchAttributes = new HashMap<String, Object>() {{
            Date reportReceivedDate = new Date( 150 );
            put( LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, toDateRange( reportReceivedDate, new Date( 0L ) ) );
            put( LprMetadataConsts.REPORT_RECEIVED_TO_DATE, toDateRange( new Date( Long.MAX_VALUE ), reportReceivedDate ) );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 2, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 2, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "1" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "100" );
            verifyAtLeastOne( results, LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, "100" );
            verifyAtLeastOne( results, LprMetadataConsts.REPORT_RECEIVED_TO_DATE, "200" );
            verifyAtLeastOne( results, LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, "150" );
            verifyAtLeastOne( results, LprMetadataConsts.REPORT_RECEIVED_TO_DATE, "250" );
        }


        //Report received search (end of 2nd interval)
        searchAttributes = new HashMap<String, Object>() {{
            Date reportReceivedDate = new Date( 250 );
            put( LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, toDateRange( reportReceivedDate, new Date( 0L ) ) );
            put( LprMetadataConsts.REPORT_RECEIVED_TO_DATE, toDateRange( new Date( Long.MAX_VALUE ), reportReceivedDate ) );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "100" );
            verifyAll( results, LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, "150" );
            verifyAll( results, LprMetadataConsts.REPORT_RECEIVED_TO_DATE, "250" );
        }

        //Report received search (outside interval)
        searchAttributes = new HashMap<String, Object>() {{
            Date reportReceivedDate = new Date( 300 );
            put( LprMetadataConsts.REPORT_RECEIVED_FROM_DATE, toDateRange( reportReceivedDate, new Date( 0L ) ) );
            put( LprMetadataConsts.REPORT_RECEIVED_TO_DATE, toDateRange( new Date( Long.MAX_VALUE ), reportReceivedDate ) );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 0, hits );
        }


        //Encounter start search (middle of unlimited interval)
        searchAttributes = new HashMap<String, Object>() {{
            Date encounterStartDate = new Date( 50 );
            put( LprMetadataConsts.ENCOUNTER_START_FROM_DATE, toDateRange( encounterStartDate, new Date( 0L ) ) );
            put( LprMetadataConsts.ENCOUNTER_START_TO_DATE, toDateRange( new Date( Long.MAX_VALUE ), encounterStartDate ) );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 2, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 2, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "100" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "101" );
            verifyAll( results, LprMetadataConsts.ENCOUNTER_START_FROM_DATE, "0" );
            verifyAll( results, LprMetadataConsts.ENCOUNTER_START_TO_DATE, String.valueOf( Long.MAX_VALUE ) );
        }

        //Encounter end search (middle of 2 intervals)
        searchAttributes = new HashMap<String, Object>() {{
            Date encounterStartDate = new Date( 550 );
            put( LprMetadataConsts.ENCOUNTER_END_FROM_DATE, toDateRange( encounterStartDate, new Date( 0L ) ) );
            put( LprMetadataConsts.ENCOUNTER_END_TO_DATE, toDateRange( new Date( Long.MAX_VALUE ), encounterStartDate ) );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 3, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 3, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "1" );
            verifyAtLeastOne( results, LprMetadataConsts.ENCOUNTER_END_FROM_DATE, "500" );
            verifyAtLeastOne( results, LprMetadataConsts.ENCOUNTER_END_TO_DATE, "600" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "100" );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "101" );
            verifyAtLeastOne( results, LprMetadataConsts.ENCOUNTER_END_FROM_DATE, "0" );
            verifyAtLeastOne( results, LprMetadataConsts.ENCOUNTER_END_TO_DATE, "750" );
        }


        //episode of care search (single day interval)
        searchAttributes = new HashMap<String, Object>() {{
            Date reportReceivedDate = new Date( 100 );
            put( LprMetadataConsts.EPISODE_OF_CARE_START_FROM_DATE, toDateRange( reportReceivedDate, new Date( 0L ) ) );
            put( LprMetadataConsts.EPISODE_OF_CARE_START_TO_DATE, toDateRange( new Date( Long.MAX_VALUE ), reportReceivedDate ) );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAtLeastOne( results, LprMetadataConsts.ERROR_NUMBER, "1" );
            verifyAll( results, LprMetadataConsts.EPISODE_OF_CARE_START_FROM_DATE, "100" );
            verifyAll( results, LprMetadataConsts.EPISODE_OF_CARE_START_TO_DATE, "100" );
        }

        //error type search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ERROR_TYPE, LprErrorType.ERROR.getId() );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAll( results, LprMetadataConsts.ERROR_TYPE, LprErrorType.ERROR.getId() );
            verifyAll( results, LprMetadataConsts.ERROR_NUMBER, "1" );
        }

        //rule group search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.RULE_GROUP, LprRuleGroup.PSYKI.getId() );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 2, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 2, results.size() );
            verifyAll( results, LprMetadataConsts.RULE_GROUP, LprRuleGroup.PSYKI.getId() );
        }

        //draft version search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.HAS_PROD_VERSION, false );
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAll( results, LprMetadataConsts.ERROR_NUMBER, "101" );
            verifyAll( results, LprMetadataConsts.HAS_PROD_VERSION, "1" ); //"1" means false in KIE-world (don't ask why..)
            verifyAll( results, LprMetadataConsts.PRODUCTION_DATE, "0" );
            verifyAll( results, LprMetadataConsts.ARCHIVED_DATE, "0" );
        }

        //prod version search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.HAS_PROD_VERSION, true );
            put( LprMetadataConsts.ARCHIVED_DATE, toDateRange( new Date( 0L ), new Date( 0L ) ) ); //archivedDate value of 0 means the rule is not archived
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAll( results, LprMetadataConsts.ERROR_NUMBER, "1" );
            verifyAll( results, LprMetadataConsts.HAS_PROD_VERSION, "0" ); //"0" means true in KIE-world (don't ask why..)
            verifyAll( results, LprMetadataConsts.PRODUCTION_DATE, "700" );
            verifyAll( results, LprMetadataConsts.ARCHIVED_DATE, "0" );
        }

        //archived version search
        searchAttributes = new HashMap<String, Object>() {{
            put( LprMetadataConsts.ARCHIVED_DATE, toDateRange( new Date( Long.MAX_VALUE ), new Date( 1L ) ) ); //archivedDate value of 0 means the rule is not archived, so we exclude that value
        }};
        {
            final int hits = config.getSearchIndex().searchByAttrsHits( searchAttributes, getClusterSegment() );
            assertEquals( 1, hits );
            final List<KObject> results = config.getSearchIndex().searchByAttrs( searchAttributes, new IOSearchService.NoOpFilter(), getClusterSegment() );
            assertEquals( 1, results.size() );
            verifyAll( results, LprMetadataConsts.ERROR_NUMBER, "100" );
            verifyAll( results, LprMetadataConsts.HAS_PROD_VERSION, "0" ); //"0" means true in KIE-world (don't ask why..)
            verifyAll( results, LprMetadataConsts.PRODUCTION_DATE, "700" );
            verifyAll( results, LprMetadataConsts.ARCHIVED_DATE, "800" );
        }


    }

    private void verifyAll( List<KObject> results, String metadataKey, String metadataValue ) {
        int found = 0;
        for ( KObject result : results ) {
            boolean foundHere = false;
            for ( KProperty<?> property : result.getProperties() ) {
                if ( metadataKey.equals( property.getName() ) ) {
                    assertEquals( metadataValue, property.getValue() );
                    assertFalse( "rule had metadata " + metadataKey + " more than once", foundHere );
                    foundHere = true;
                    found++;
                }
            }
        }
        assertTrue( "All results did not have metadata " + metadataKey + " with value " + metadataValue, found == results.size() );
    }

    private void verifyAtLeastOne( List<KObject> results, String metadataKey, String metadataValue ) {
        boolean found = false;
        for ( KObject result : results ) {
            for ( KProperty<?> property : result.getProperties() ) {
                if ( metadataKey.equals( property.getName() ) ) {
                    if ( metadataValue.equals( property.getValue() ) ) {
                        found = true;
                    }
                }
            }
        }
        assertTrue( "no results had metadata " + metadataKey + " with value " + metadataValue, found );
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

    private Map<String, Object> getAttributesMap( LprMetaAttributes lprMetaAttributes ) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.putAll( DublinCoreAttributesUtil.toMap( new DublinCoreAttributesMock(), "*" ) );
        attrs.putAll( LprMetaAttributesUtil.toMap( lprMetaAttributes, "*" ) );
        return attrs;
    }
}


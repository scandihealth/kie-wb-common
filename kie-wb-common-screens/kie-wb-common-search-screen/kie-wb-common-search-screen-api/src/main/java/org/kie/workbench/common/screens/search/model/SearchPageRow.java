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

package org.kie.workbench.common.screens.search.model;

import java.util.Date;

import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.guvnor.common.services.shared.metadata.model.LprRuleGroup;
import org.jboss.errai.common.client.api.annotations.Portable;
import org.uberfire.backend.vfs.Path;
import org.uberfire.paging.AbstractPathPageRow;

@Portable
public class SearchPageRow extends AbstractPathPageRow {

    private String description;
    private String abbreviatedDescription;
    private String creator;
    private Date createdDate;
    private String lastContributor;
    private Date lastModified;
    private boolean disabled;
    private Long lprMetaErrorNumber;
    private String lprMetaErrorText;
    private LprErrorType lprMetaErrorType;
    private LprRuleGroup lprMetaRuleGroup;
    private Date lprMetaProductionDate;
    private Date lprMetaArchivedDate;
    private boolean lprMetaIsValidForLPRReports;
    private boolean lprMetaIsValidForDUSASAbroadReports;
    private boolean lprMetaIsValidForDUSASSpecialityReports;
    private boolean lprMetaIsValidForPrivateSectorReports;

    public SearchPageRow() {
        super();
    }

    public SearchPageRow( final Path path ) {
        super( path );
    }

    public SearchPageRow( final Path path,
                          final String creator,
                          final Date createdDate,
                          final String lastContributor,
                          final Date lastModified,
                          final String description,
                          Long lprMetaErrorNumber,
                          String lprMetaErrorText,
                          LprErrorType lprMetaErrorType,
                          LprRuleGroup lprMetaRuleGroup,
                          Date lprMetaProductionDate,
                          Date lprMetaArchivedDate,
                          boolean lprMetaIsValidForLPRReports,
                          boolean lprMetaIsValidForDUSASAbroadReports,
                          boolean lprMetaIsValidForDUSASSpecialityReports,
                          boolean lprMetaIsValidForPrivateSectorReports ) {
        super( path );
        this.creator = creator;
        this.createdDate = createdDate;
        this.lastContributor = lastContributor;
        this.lastModified = lastModified;
        this.description = description;
        this.lprMetaErrorNumber = lprMetaErrorNumber;
        this.lprMetaErrorText = lprMetaErrorText;
        this.lprMetaErrorType = lprMetaErrorType;
        this.lprMetaRuleGroup = lprMetaRuleGroup;
        this.lprMetaProductionDate = lprMetaProductionDate;
        this.lprMetaArchivedDate = lprMetaArchivedDate;
        this.lprMetaIsValidForLPRReports = lprMetaIsValidForLPRReports;
        this.lprMetaIsValidForDUSASAbroadReports = lprMetaIsValidForDUSASAbroadReports;
        this.lprMetaIsValidForDUSASSpecialityReports = lprMetaIsValidForDUSASSpecialityReports;
        this.lprMetaIsValidForPrivateSectorReports = lprMetaIsValidForPrivateSectorReports;
    }

    @Override
    public int compareTo( AbstractPathPageRow o ) {
        if ( o instanceof SearchPageRow ) {
            SearchPageRow other = ( SearchPageRow ) o;
            if ( this.lprMetaErrorNumber == null && other.lprMetaErrorNumber != null ) {
                return -1;
            } else if ( this.lprMetaErrorNumber == null ) { //other.lprMetaErrorNumber is null
                return 0;
            } else if ( other.lprMetaErrorNumber == null ) { //this.lprMetaErrorNumber is not null
                return 1;
            } else {
                return this.lprMetaErrorNumber.compareTo( other.lprMetaErrorNumber );
            }
        } else {
            return super.compareTo( o );
        }
    }

    public String getAbbreviatedDescription() {
        return abbreviatedDescription;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getCreator() {
        return creator;
    }

    public String getDescription() {
        return description;
    }

    public String getLastContributor() {
        return lastContributor;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setAbbreviatedDescription( String abbreviatedDescription ) {
        this.abbreviatedDescription = abbreviatedDescription;
    }

    public void setCreatedDate( Date createdDate ) {
        this.createdDate = createdDate;
    }

    public void setCreator( String creator ) {
        this.creator = creator;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setLastContributor( String lastContributor ) {
        this.lastContributor = lastContributor;
    }

    public void setLastModified( Date lastModified ) {
        this.lastModified = lastModified;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled( final boolean disabled ) {
        this.disabled = disabled;
    }


    public Long getLprMetaErrorNumber() {
        return lprMetaErrorNumber;
    }

    public String getLprMetaErrorText() {
        return lprMetaErrorText;
    }

    public LprErrorType getLprMetaErrorType() {
        return lprMetaErrorType;
    }

    public LprRuleGroup getLprMetaRuleGroup() {
        return lprMetaRuleGroup;
    }

    public Date getLprMetaProductionDate() {
        return lprMetaProductionDate;
    }

    public Date getLprMetaArchivedDate() {
        return lprMetaArchivedDate;
    }

    public boolean getLprMetaIsValidForLPRReports() {
        return lprMetaIsValidForLPRReports;
    }

    public boolean getLprMetaIsValidForDUSASAbroadReports() {
        return lprMetaIsValidForDUSASAbroadReports;
    }

    public boolean getLprMetaIsValidForDUSASSpecialityReports() {
        return lprMetaIsValidForDUSASSpecialityReports;
    }

    public boolean getLprMetaIsValidForPrivateSectorReports() {
        return lprMetaIsValidForPrivateSectorReports;
    }
}

/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.kie.workbench.common.screens.search.client.widgets;

import java.util.Collections;
import java.util.Date;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.screens.search.client.resources.i18n.Constants;
import org.kie.workbench.common.screens.search.model.QueryMetadataPageRequest;
import org.kie.workbench.common.screens.search.model.SearchPageRow;
import org.kie.workbench.common.screens.search.model.SearchTermPageRequest;
import org.kie.workbench.common.screens.search.service.SearchService;
import org.kie.workbench.common.widgets.client.tables.AbstractPathPagedTable;
import org.uberfire.ext.widgets.common.client.tables.TitledTextCell;
import org.uberfire.ext.widgets.common.client.tables.TitledTextColumn;
import org.uberfire.paging.PageResponse;

import static org.jboss.errai.bus.client.api.base.MessageBuilder.*;

/**
 * Widget with a table of Assets.
 */
public class SearchResultTable extends AbstractPathPagedTable<SearchPageRow> {

    private static final int PAGE_SIZE = 10;

//    private ClientTypeRegistry clientTypeRegistry = null;

    public SearchResultTable() {
        super( PAGE_SIZE );

        setDataProvider( new AsyncDataProvider<SearchPageRow>() {
            protected void onRangeChanged( HasData<SearchPageRow> display ) {
                updateRowCount( 0,
                        true );
                updateRowData( 0,
                        Collections.<SearchPageRow>emptyList() );
            }
        } );

        setTableStyle();
    }

    public SearchResultTable( final QueryMetadataPageRequest queryRequest ) {
        super( PAGE_SIZE );

        if ( queryRequest.getPageSize() == null ) {
            queryRequest.setPageSize( PAGE_SIZE );
        }

        setDataProvider( new AsyncDataProvider<SearchPageRow>() {
            protected void onRangeChanged( HasData<SearchPageRow> display ) {
                queryRequest.setStartRowIndex( dataGrid.getPageStart() );
                queryRequest.setPageSize( dataGrid.getPageSize() );

                createCall( new RemoteCallback<PageResponse<SearchPageRow>>() {
                    public void callback( final PageResponse<SearchPageRow> response ) {

                        updateRowCount( response.getTotalRowSize(),
                                response.isTotalRowSizeExact() );
                        updateRowData( response.getStartRowIndex(),
                                response.getPageRowList() );
                    }
                }, SearchService.class ).queryMetadata( queryRequest );
            }
        } );

        setTableStyle();
    }

    public SearchResultTable( final SearchTermPageRequest searchRequest ) {
        super( PAGE_SIZE );

        if ( searchRequest.getPageSize() == null ) {
            searchRequest.setPageSize( PAGE_SIZE );
        }

        setDataProvider( new AsyncDataProvider<SearchPageRow>() {
            protected void onRangeChanged( HasData<SearchPageRow> display ) {
                searchRequest.setStartRowIndex( dataGrid.getPageStart() );
                searchRequest.setPageSize( dataGrid.getPageSize() );

                createCall( new RemoteCallback<PageResponse<SearchPageRow>>() {
                    public void callback( final PageResponse<SearchPageRow> response ) {
                        updateRowCount( response.getTotalRowSize(),
                                response.isTotalRowSizeExact() );
                        updateRowData( response.getStartRowIndex(),
                                response.getPageRowList() );
                    }
                }, SearchService.class ).fullTextSearch( searchRequest );
            }
        } );

        setTableStyle();
    }

    protected void setTableStyle() {
        final Style style = dataGrid.getElement().getStyle();
        style.setMarginLeft( 0, Style.Unit.PX );
        style.setMarginRight( 0, Style.Unit.PX );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addAncillaryColumns() {
/*
        final Column<SearchPageRow, ComparableImageResource> formatColumn = new Column<SearchPageRow, ComparableImageResource>( new ComparableImageResourceCell() ) {

            public ComparableImageResource getValue( SearchPageRow row ) {
                final ClientResourceType associatedType = getClientTypeRegistry().resolve( row.getPath() );

                final Image icon;
                if ( associatedType.getIcon() == null || !( associatedType.getIcon() instanceof Image ) ) {
                    icon = new Image( ImageResources.INSTANCE.file() );
                } else {
                    icon = (Image) associatedType.getIcon();
                }

                return new ComparableImageResource( associatedType.getShortName(), icon );
            }
        };

        dataGrid.addColumn( formatColumn,
                            Constants.INSTANCE.Format() );
*/
        final Column<SearchPageRow, String> ruleGroupColumn = new Column<SearchPageRow, String>( new TextCell() ) {
            public String getValue( SearchPageRow row ) {
                return row.getLprMetaRuleGroup().getDisplayText();
            }
        };
        dataGrid.addColumn( ruleGroupColumn, Constants.INSTANCE.RuleGroupMetaData() );

        final TitledTextColumn<SearchPageRow> titleColumn = new TitledTextColumn<SearchPageRow>() {
            public TitledTextCell.TitledText getValue( SearchPageRow row ) {
                return new TitledTextCell.TitledText( row.getPath().getFileName(),
                        row.getAbbreviatedDescription() );
            }
        };
        dataGrid.addColumn( titleColumn,
                Constants.INSTANCE.Name() );

        final Column<SearchPageRow, String> errorNumberColumn = new Column<SearchPageRow, String>( new TextCell() ) {
            public String getValue( SearchPageRow row ) {
                return String.valueOf( row.getLprMetaErrorNumber() );
            }
        };
        dataGrid.addColumn( errorNumberColumn, Constants.INSTANCE.ErrorNumberMetaData() );

        final Column<SearchPageRow, String> errorTextColumn = new Column<SearchPageRow, String>( new TextCell() ) {
            public String getValue( SearchPageRow row ) {
                return row.getLprMetaErrorText();
            }
        };
        dataGrid.addColumn( errorTextColumn, Constants.INSTANCE.ErrorTextMetaData() );

        final Column<SearchPageRow, String> errorTypeColumn = new Column<SearchPageRow, String>( new TextCell() ) {
            public String getValue( SearchPageRow row ) {
                return row.getLprMetaErrorType().getDisplayText();
            }
        };
        dataGrid.addColumn( errorTypeColumn, Constants.INSTANCE.ErrorTypeMetaData() );

        final Column<SearchPageRow, Date> productionDateColumn = new Column<SearchPageRow, Date>(
                new DateCell( DateTimeFormat.getFormat( DateTimeFormat.PredefinedFormat.DATE_SHORT ) ) ) {
            public Date getValue( SearchPageRow row ) {
                return row.getLprMetaProductionDate();
            }
        };
        dataGrid.addColumn( productionDateColumn, Constants.INSTANCE.ProductionMetaData() );

        final Column<SearchPageRow, Date> archivedDateColumn = new Column<SearchPageRow, Date>(
                new DateCell( DateTimeFormat.getFormat( DateTimeFormat.PredefinedFormat.DATE_SHORT ) ) ) {
            public Date getValue( SearchPageRow row ) {
                return row.getLprMetaArchivedDate();
            }
        };
        dataGrid.addColumn( archivedDateColumn, Constants.INSTANCE.ArchivedMetaData() );

        final Column<SearchPageRow, Date> createdDateColumn = new Column<SearchPageRow, Date>(
                new DateCell( DateTimeFormat.getFormat( DateTimeFormat.PredefinedFormat.DATE_SHORT ) ) ) {
            public Date getValue( SearchPageRow row ) {
                return row.getCreatedDate();
            }
        };
        dataGrid.addColumn( createdDateColumn, Constants.INSTANCE.CreatedDate(), false );

        final Column<SearchPageRow, Date> lastModifiedColumn = new Column<SearchPageRow, Date>(
                new DateCell( DateTimeFormat.getFormat( DateTimeFormat.PredefinedFormat.DATE_SHORT ) ) ) {
            public Date getValue( final SearchPageRow row ) {
                return row.getLastModified();
            }
        };
        dataGrid.addColumn( lastModifiedColumn, Constants.INSTANCE.LastModified(), false );

        final Column<SearchPageRow, Boolean> isValidForLPRReportsColumn = new Column<SearchPageRow, Boolean>( new DisabledCheckboxCell() ) {
            public Boolean getValue( SearchPageRow row ) {
                return row.getLprMetaIsValidForLPRReports();
            }
        };
        dataGrid.addColumn( isValidForLPRReportsColumn, Constants.INSTANCE.IsValidForLPRReportsMetaData() );

        final Column<SearchPageRow, Boolean> isValidForDUSASAbroadReportsColumn = new Column<SearchPageRow, Boolean>( new DisabledCheckboxCell() ) {
            public Boolean getValue( SearchPageRow row ) {
                return row.getLprMetaIsValidForDUSASAbroadReports();
            }
        };
        dataGrid.addColumn( isValidForDUSASAbroadReportsColumn, Constants.INSTANCE.IsValidForDUSASAbroadReportsMetaData() );

        final Column<SearchPageRow, Boolean> isValidForDUSASSpecialityReports = new Column<SearchPageRow, Boolean>( new DisabledCheckboxCell() ) {
            public Boolean getValue( SearchPageRow row ) {
                return row.getLprMetaIsValidForDUSASSpecialityReports();
            }
        };
        dataGrid.addColumn( isValidForDUSASSpecialityReports, Constants.INSTANCE.IsValidForDUSASSpecialityReportsMetaData() );

        final Column<SearchPageRow, Boolean> isValidForPrimarySectorReports = new Column<SearchPageRow, Boolean>( new DisabledCheckboxCell() ) {
            public Boolean getValue( SearchPageRow row ) {
                return row.getLprMetaIsValidForPrimarySectorReports();
            }
        };
        dataGrid.addColumn( isValidForPrimarySectorReports, Constants.INSTANCE.IsValidForPrimarySectorReportsMetaData() );

/*
        final Column<SearchPageRow, Boolean> isDisabledColumn = new Column<SearchPageRow, Boolean>( new CheckboxCellImpl( true ) ) {
            public Boolean getValue( final SearchPageRow row ) {
                return row.isDisabled();
            }
        };
        dataGrid.addColumn( isDisabledColumn,
                            Constants.INSTANCE.Disabled(),
                            false );
*/
    }

/*
    private ClientTypeRegistry getClientTypeRegistry() {
        if ( clientTypeRegistry == null ) {
            clientTypeRegistry = IOC.getBeanManager().lookupBean( ClientTypeRegistry.class ).getInstance();
        }
        return clientTypeRegistry;
    }
*/

    private static class DisabledCheckboxCell extends CheckboxCell {

        /**
         * An html string representation of a disabled checked input box.
         */
        private static final SafeHtml INPUT_CHECKED_DISABLED = SafeHtmlUtils.fromSafeConstant( "<input type=\"checkbox\" tabindex=\"-1\" checked disabled/>" );

        /**
         * An html string representation of a disabled unchecked input box.
         */
        private static final SafeHtml INPUT_UNCHECKED_DISABLED = SafeHtmlUtils.fromSafeConstant( "<input type=\"checkbox\" tabindex=\"-1\"  disabled/>" );


        @Override
        public void render( Context context, Boolean value, SafeHtmlBuilder sb ) {
            // Get the view data.
            Object key = context.getKey();
            Boolean viewData = getViewData(key);
            if (viewData != null && viewData.equals(value)) {
                clearViewData(key);
                viewData = null;
            }

            if (value != null && ((viewData != null) ? viewData : value)) {
                sb.append(INPUT_CHECKED_DISABLED);
            } else {
                sb.append(INPUT_UNCHECKED_DISABLED);
            }
        }
    }
}

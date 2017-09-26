/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.search.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.CalendarUtil;
import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.guvnor.common.services.shared.metadata.model.LprRuleGroup;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Column;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelCollapse;
import org.gwtbootstrap3.client.ui.PanelGroup;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Radio;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.kie.workbench.common.screens.search.client.resources.i18n.Constants;
import org.kie.workbench.common.screens.search.client.widgets.SearchResultTable;
import org.kie.workbench.common.screens.search.model.QueryMetadataPageRequest;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.workbench.type.ClientTypeRegistry;
import org.uberfire.ext.widgets.common.client.common.DatePicker;
import org.uberfire.ext.widgets.common.client.common.NumericLongTextBox;

import static org.guvnor.common.services.shared.metadata.model.LprMetadataConsts.*;

//todo write unit tests
@Dependent
@WorkbenchScreen(identifier = "FindForm")
public class FindForm
        extends Composite {

    interface FindFormBinder
            extends
            UiBinder<Widget, FindForm> {

    }

    private static FindFormBinder uiBinder = GWT.create( FindFormBinder.class );

    @Inject
    private ClientTypeRegistry clientTypeRegistry;

    @UiField
    SimplePanel errorPanel;

    @UiField
    FormGroup formGroup;

    @UiField
    Form form;

    @UiField
    TextBox errorNumberTextBox;

    @UiFactory
    NumericLongTextBox constructErrorNumberNumericTextBox() {
        return new NumericLongTextBox( true );
    }

    @UiField
    TextBox errorTextTextBox;

    @UiField
    Radio isProduction;

    @UiField
    Radio isDraft;

    @UiField
    Radio isArchived;

    @UiField
    CheckBox isValidForLPRReports;

    @UiField
    CheckBox isValidForDUSASAbroadReports;

    @UiField
    CheckBox isValidForDUSASSpecialityReports;

    @UiField
    ListBox ruleGroupListBox;

    @UiField
    ListBox errorTypeListBox;

    @UiField
    DatePicker reportReceivedDate;

    @UiField
    DatePicker encounterStartDate;

    @UiField
    DatePicker encounterEndDate;

    @UiField
    DatePicker episodeOfCareStartDate;

//    @UiField
//    TextBox sourceTextBox;

    @UiField
    TextBox createdByTextBox;

//    @UiField
//    TextBox descriptionByTextBox;
//
//    @UiField
//    Typeahead formatTypeahead;
//
//    @UiField
//    TextBox subjectTextBox;
//
//    @UiField
//    TextBox typeTextBox;

    @UiField
    TextBox lastModifiedByTextBox;

//    @UiField
//    TextBox externalLinkTextBox;

    @UiField
    TextBox checkinCommentTextBox;

    @UiField
    DatePicker createdAfter;

    @UiField
    DatePicker createdBefore;

    @UiField
    DatePicker lastModifiedAfter;

    @UiField
    DatePicker lastModifiedBefore;

    @UiField
    PanelGroup accordion;

    @UiField
    PanelHeader formAccordionHeader;

    @UiField
    PanelCollapse formAccordionCollapse;

    @UiField
    PanelHeader resultAccordionHeader;

    @UiField
    PanelCollapse resultAccordionCollapse;

    @UiField
    Column simplePanel;

    @PostConstruct
    public void init() {
        initWidget( uiBinder.createAndBindUi( this ) );

        accordion.setId( DOM.createUniqueId() );
        formAccordionHeader.setDataParent( accordion.getId() );
        formAccordionHeader.setDataTargetWidget( formAccordionCollapse );
        resultAccordionHeader.setDataParent( accordion.getId() );
        resultAccordionHeader.setDataTargetWidget( resultAccordionCollapse );

        createdAfter.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        createdBefore.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        lastModifiedAfter.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        lastModifiedBefore.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        reportReceivedDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        encounterStartDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        encounterEndDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        episodeOfCareStartDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );

        formGroup.setStyleName( null );

//        formatTypeahead.setDatasets( new StringDataset( new ArrayList<String>() {{
//            for ( final ClientResourceType resourceType : clientTypeRegistry.getRegisteredTypes() ) {
//                add( resourceType.getShortName() );
//            }
//        }} ) );

        for ( LprRuleGroup ruleGroup : LprRuleGroup.values() ) {
            ruleGroupListBox.addItem( ruleGroup.getDisplayText(), ruleGroup.toString() );
        }
        ruleGroupListBox.setSelectedIndex( 0 );

        for ( LprErrorType errorType : LprErrorType.values() ) {
            errorTypeListBox.addItem( errorType.getDisplayText(), errorType.toString() );
        }
        errorTypeListBox.setSelectedIndex( 0 );
    }

    @UiHandler("clear")
    public void onClearClick( final ClickEvent e ) {
        form.reset();
    }

    @UiHandler("search")
    public void onSearchClick( final ClickEvent e ) {
        errorPanel.clear();
        formGroup.setValidationState( ValidationState.NONE );
        final Map<String, Object> metadata = new HashMap<String, Object>();

        String errorNumberValue = errorNumberTextBox.getValue().trim();
        if ( !errorNumberValue.isEmpty() ) {
            metadata.put( ERROR_NUMBER, errorNumberValue );
        }
        String errorText = errorTextTextBox.getText().trim();
        if ( !errorText.isEmpty() ) {
            String[] words = errorText.split( "\\s+" );
            metadata.put( ERROR_TEXT, words );
        }

        if ( Boolean.TRUE.equals( isProduction.getValue() ) ) {
            metadata.put( SEARCH_IS_PRODUCTION, isProduction.getValue() );
        }

        if ( Boolean.TRUE.equals( isDraft.getValue() ) ) {
            metadata.put( SEARCH_IS_DRAFT, isDraft.getValue() );
        }

        if ( Boolean.TRUE.equals( isArchived.getValue() ) ) {
            metadata.put( SEARCH_IS_ARCHIVED, isArchived.getValue() );
        }

        if ( Boolean.TRUE.equals( isValidForLPRReports.getValue() ) ) {
            metadata.put( IS_VALID_FOR_LPR_REPORTS, isValidForLPRReports.getValue() );
        }
        if ( Boolean.TRUE.equals( isValidForDUSASAbroadReports.getValue() ) ) {
            metadata.put( IS_VALID_FOR_DUSAS_ABROAD_REPORTS, isValidForDUSASAbroadReports.getValue() );
        }
        if ( Boolean.TRUE.equals( isValidForDUSASSpecialityReports.getValue() ) ) {
            metadata.put( IS_VALID_FOR_DUSAS_SPECIALITY_REPORTS, isValidForDUSASSpecialityReports.getValue() );
        }

        LprRuleGroup ruleGroup = LprRuleGroup.valueOf( ruleGroupListBox.getSelectedValue() );
        if ( ruleGroup != LprRuleGroup.NONE ) {
            metadata.put( RULE_GROUP, ruleGroup );
        }

        LprErrorType errorType = LprErrorType.valueOf( errorTypeListBox.getSelectedValue() );
        if ( errorType != LprErrorType.OK ) {
            metadata.put( ERROR_TYPE, errorType );
        }

        if ( reportReceivedDate.getValue() != null ) {
            metadata.put( SEARCH_REPORT_RECEIVED_DATE, reportReceivedDate.getValue() );
        }

        if ( encounterStartDate.getValue() != null ) {
            metadata.put( SEARCH_ENCOUNTER_START_DATE, encounterStartDate.getValue() );
        }

        if ( encounterEndDate.getValue() != null ) {
            metadata.put( SEARCH_ENCOUNTER_END_DATE, encounterEndDate.getValue() );
        }

        if ( episodeOfCareStartDate.getValue() != null ) {
            metadata.put( SEARCH_EPISODE_OF_CARE_START_DATE, episodeOfCareStartDate.getValue() );
        }

//        if ( !sourceTextBox.getText().trim().isEmpty() ) {
//            metadata.put( "dcore.source[0]", sourceTextBox.getText().trim() );
//        }
//
//        if ( !createdByTextBox.getText().trim().isEmpty() ) {
//            metadata.put( "createdBy", createdByTextBox.getText().trim() );
//        }
//
//        if ( !descriptionByTextBox.getText().trim().isEmpty() ) {
//            metadata.put( "dcore.description[0]", descriptionByTextBox.getText().trim() );
//        }
//
//        if ( !formatTypeahead.getText().trim().isEmpty() ) {
//            final String pattern = clientTypeRegistry.resolveWildcardPattern( formatTypeahead.getText().trim() );
//            metadata.put( "filename", pattern );
//        }
//
//        if ( !subjectTextBox.getText().trim().isEmpty() ) {
//            metadata.put( "dcore.subject[0]", subjectTextBox.getText().trim() );
//        }
//
//        if ( !typeTextBox.getText().trim().isEmpty() ) {
//            metadata.put( "dcore.type[0]", typeTextBox.getText().trim() );
//        }

        if ( !lastModifiedByTextBox.getText().trim().isEmpty() ) {
            metadata.put( "lastModifiedBy", lastModifiedByTextBox.getText().trim() );
        }

//        if ( !externalLinkTextBox.getText().trim().isEmpty() ) {
//            metadata.put( "dcore.relation[0]", externalLinkTextBox.getText().trim() );
//        }

        if ( !checkinCommentTextBox.getText().trim().isEmpty() ) {
            metadata.put( "checkinComment", checkinCommentTextBox.getText().trim() );
        }

        boolean hasSomeDateValue = false;

        if ( createdAfter.getValue() != null ) {
            hasSomeDateValue = true;
        }

        if ( createdBefore.getValue() != null ) {
            hasSomeDateValue = true;
        }

        if ( lastModifiedAfter.getValue() != null ) {
            hasSomeDateValue = true;
        }

        Date lastModifiedBeforeValue = lastModifiedBefore.getValue();
        if ( lastModifiedBeforeValue != null ) {
            hasSomeDateValue = true;
        }

        if ( metadata.size() == 0 && !hasSomeDateValue ) {
            //We use a 'hidden' lastModifiedBefore criteria that returns all rules if the user has not selected any criteria
            lastModifiedBeforeValue = new Date();
            CalendarUtil.addDaysToDate( new Date(), 1 );
        }

        final SearchResultTable queryTable = new SearchResultTable( new QueryMetadataPageRequest( metadata,
                createdAfter.getValue(), createdBefore.getValue(),
                lastModifiedAfter.getValue(), lastModifiedBeforeValue,
                0, null ) );
        simplePanel.clear();

        simplePanel.add( queryTable );

        formAccordionCollapse.setIn( false );
        resultAccordionCollapse.setIn( true );
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return Constants.INSTANCE.AssetSearch();
    }

}

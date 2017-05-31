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

import java.util.ArrayList;
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
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Column;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelCollapse;
import org.gwtbootstrap3.client.ui.PanelGroup;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.extras.typeahead.client.base.StringDataset;
import org.gwtbootstrap3.extras.typeahead.client.ui.Typeahead;
import org.kie.workbench.common.screens.search.client.resources.i18n.Constants;
import org.kie.workbench.common.screens.search.client.widgets.SearchResultTable;
import org.kie.workbench.common.screens.search.model.QueryMetadataPageRequest;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.workbench.type.ClientResourceType;
import org.uberfire.client.workbench.type.ClientTypeRegistry;
import org.uberfire.ext.widgets.common.client.common.DatePicker;
import org.uberfire.ext.widgets.common.client.common.NumericLongTextBox;

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
    NumericLongTextBox errorNumberNumericTextBox;

    @UiFactory
    NumericLongTextBox constructErrorNumberNumericTextBox() {
        return new NumericLongTextBox( true );
    }

    @UiField
    TextBox errorTextTextBox;

    @UiField
    CheckBox inProduction;

    @UiField
    CheckBox isDraft;

    @UiField
    ListBox ruleGroupListBox;

    @UiField
    ListBox errorTypeListBox;

    @UiField
    DatePicker ruleValidDate;

    @UiField
    TextBox sourceTextBox;

    @UiField
    TextBox createdByTextBox;

    @UiField
    TextBox descriptionByTextBox;

    @UiField
    Typeahead formatTypeahead;

    @UiField
    TextBox subjectTextBox;

    @UiField
    TextBox typeTextBox;

    @UiField
    TextBox lastModifiedByTextBox;

    @UiField
    TextBox externalLinkTextBox;

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
        ruleValidDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );

        formGroup.setStyleName( null );

        formatTypeahead.setDatasets( new StringDataset( new ArrayList<String>() {{
            for ( final ClientResourceType resourceType : clientTypeRegistry.getRegisteredTypes() ) {
                add( resourceType.getShortName() );
            }
        }} ) );

        String[] ruleGroups = new String[]{
                ""
                , "LPR.MOBST"
                , "LPR.ULYKK"
                , "FIXED.LPR.SKSKO"
                , "LPR.STEDF"
                , "LPR.OKOMB"
                , "FIXED.LPR.PASSV"
                , "LPR.INDUD"
                , "FIXED.DUSAS.SPEC"
                , "LPR.INDUD/SKSKO/MOBST"
                , "LPR.PSYKI"
                , "FIXED.,VENTE"
                , "FIXED.LPR.OPERA"
                , "FIXED.LPR.BOBST"
                , "LPR.INDUD/BESØG"
                , "LPR.INDUD/SKSKO"
                , "LPR.BESØG"
                , "LPR.SKSKO"
                , "LPR.INDUD/PASSV"
                , "LPR.PATIENT"
                , "LPR.PASSV"
                , "FIXED.LPR.MOBST"
                , "FIXED.LPR.DIAGN"
                , "FIXED.LPR.STEDF"
                , "FIXED.LPR.OKOMB"
                , "LPR.INDUD/VENTE"
                , "LPR.VENTE"
                , "LPR.OPERA"
                , "DUSAS"
                , "FIXED.LPR.INDUD"
                , "LPR.BOBST"
                , "DUSAS.SPEC"
                , "FIXED.LPR.PSYKI"
                , "LPR.Psykiatri"};

        for ( String sRuleGroup : ruleGroups ) {
            ruleGroupListBox.addItem( sRuleGroup );
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
        errorNumberNumericTextBox.setValue( "" ); //necessary because uberfire extensions puts in a "0" when resetting this input
    }

    @UiHandler("search")
    public void onSearchClick( final ClickEvent e ) {
        errorPanel.clear();
        formGroup.setValidationState( ValidationState.NONE );
        final Map<String, Object> metadata = new HashMap<String, Object>();

        String errorNumberValue = errorNumberNumericTextBox.getValue().trim();
        if ( !errorNumberValue.isEmpty() && errorNumberNumericTextBox.isValidValue( errorNumberValue, true ) ) {
            if ( Long.parseLong( errorNumberValue ) > 0 ) {
                metadata.put( "lprmeta.errorNumber", errorNumberValue );
            }
        }
        if ( !errorTextTextBox.getText().trim().isEmpty() ) {
            metadata.put( "lprmeta.errorText", errorTextTextBox.getText().trim() );
        }

        if ( Boolean.TRUE.equals( inProduction.getValue() ) ) {
            metadata.put( "lprmeta.inproduction", inProduction.getValue() );
        }

        if ( Boolean.TRUE.equals( isDraft.getValue() ) ) {
            metadata.put( "lprmeta.isdraft", isDraft.getValue() );
        }

        if ( !ruleGroupListBox.getSelectedValue().trim().isEmpty() ) {
            metadata.put( "lprmeta.ruleGroup", ruleGroupListBox.getSelectedItemText().trim() );
        }

        String errorTypeSelectedValue = errorTypeListBox.getSelectedValue();
        if ( LprErrorType.valueOf( errorTypeSelectedValue ) != LprErrorType.NONE && !errorTypeSelectedValue.trim().isEmpty() ) {
            metadata.put( "lprmeta.errorType", errorTypeSelectedValue );
        }

        if ( ruleValidDate.getValue() != null ) {
            metadata.put( "lprmeta.ruleValidDate", ruleValidDate.getValue() );
        }

        if ( !sourceTextBox.getText().trim().isEmpty() ) {
            metadata.put( "dcore.source[0]", sourceTextBox.getText().trim() );
        }

        if ( !createdByTextBox.getText().trim().isEmpty() ) {
            metadata.put( "createdBy", createdByTextBox.getText().trim() );
        }

        if ( !descriptionByTextBox.getText().trim().isEmpty() ) {
            metadata.put( "dcore.description[0]", descriptionByTextBox.getText().trim() );
        }

        if ( !formatTypeahead.getText().trim().isEmpty() ) {
            final String pattern = clientTypeRegistry.resolveWildcardPattern( formatTypeahead.getText().trim() );
            metadata.put( "filename", pattern );
        }

        if ( !subjectTextBox.getText().trim().isEmpty() ) {
            metadata.put( "dcore.subject[0]", subjectTextBox.getText().trim() );
        }

        if ( !typeTextBox.getText().trim().isEmpty() ) {
            metadata.put( "dcore.type[0]", typeTextBox.getText().trim() );
        }

        if ( !lastModifiedByTextBox.getText().trim().isEmpty() ) {
            metadata.put( "lastModifiedBy", lastModifiedByTextBox.getText().trim() );
        }

        if ( !externalLinkTextBox.getText().trim().isEmpty() ) {
            metadata.put( "dcore.relation[0]", externalLinkTextBox.getText().trim() );
        }

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
            CalendarUtil.addDaysToDate( new Date(), 1 );        }

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

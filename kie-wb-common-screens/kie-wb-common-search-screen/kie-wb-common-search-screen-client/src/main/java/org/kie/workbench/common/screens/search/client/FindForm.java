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
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.gwtbootstrap3.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.AlertType;
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

    private static final String ERROR_TYPE_WARNING = "Advarsel";
    private static final String ERROR_TYPE_ERROR = "Fejl";
    private static final String ERROR_TYPE_FATAL = "Fatal";

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

    @UiField
    TextBox errorTextTextBox;

    @UiField
    ListBox ruleGroupListBox;

    @UiField
    ListBox errorTypeListBox;

    @UiField
    DatePicker recievedValidFromDate;

    @UiField
    DatePicker recievedValidToDate;

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

        formGroup.setStyleName( null );

        formatTypeahead.setDatasets( new StringDataset( new ArrayList<String>() {{
            for ( final ClientResourceType resourceType : clientTypeRegistry.getRegisteredTypes() ) {
                add( resourceType.getShortName() );
            }
        }} ) );
    }

    @Override
    protected void onLoad() {
        String[] ruleGroups = new String[] { ""
                ,"LPR.MOBST"
                ,"LPR.ULYKK"
                ,"FIXED.LPR.SKSKO"
                ,"LPR.STEDF"
                ,"LPR.OKOMB"
                ,"FIXED.LPR.PASSV"
                ,"LPR.INDUD"
                ,"FIXED.DUSAS.SPEC"
                ,"LPR.INDUD/SKSKO/MOBST"
                ,"LPR.PSYKI"
                ,"FIXED.,VENTE"
                ,"FIXED.LPR.OPERA"
                ,"FIXED.LPR.BOBST"
                ,"LPR.INDUD/BESØG"
                ,"LPR.INDUD/SKSKO"
                ,"LPR.BESØG"
                ,"LPR.SKSKO"
                ,"LPR.INDUD/PASSV"
                ,"LPR.PATIENT"
                ,"LPR.PASSV"
                ,"FIXED.LPR.MOBST"
                ,"FIXED.LPR.DIAGN"
                ,"FIXED.LPR.STEDF"
                ,"FIXED.LPR.OKOMB"
                ,"LPR.INDUD/VENTE"
                ,"LPR.VENTE"
                ,"LPR.OPERA"
                ,"DUSAS"
                ,"FIXED.LPR.INDUD"
                ,"LPR.BOBST"
                ,"DUSAS.SPEC"
                ,"FIXED.LPR.PSYKI"
                ,"LPR.Psykiatri"};

        for (String sRuleGroup : ruleGroups ) {
            ruleGroupListBox.addItem(sRuleGroup);
        }
        ruleGroupListBox.setSelectedIndex(0);

        String[] errorTypes = new String[] {"", ERROR_TYPE_WARNING, ERROR_TYPE_ERROR, ERROR_TYPE_FATAL};
        for (String sErrorType : errorTypes ) {
            errorTypeListBox.addItem(sErrorType);
        }
        errorTypeListBox.setSelectedIndex(0);
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
        if( !errorNumberNumericTextBox.getText().trim().isEmpty() &&
                errorNumberNumericTextBox.isValidValue(errorNumberNumericTextBox.getText().trim(), false)) {
            if( Long.parseLong( errorNumberNumericTextBox.getText().trim() ) > 0 ) {
                metadata.put("lprmeta.errorNumber", errorNumberNumericTextBox.getText().trim());
            }
        }
        if(!errorTextTextBox.getText().trim().isEmpty()) {
            metadata.put( "lprmeta.errorText", errorTextTextBox.getText().trim() );
        }

        if(!ruleGroupListBox.getSelectedItemText().trim().isEmpty()) {
            metadata.put( "lprmeta.ruleGroup", ruleGroupListBox.getSelectedItemText().trim() );
        }

        if(!errorTypeListBox.getSelectedItemText().trim().isEmpty()) {
            String errorTypeSearch = "";
            final String sErrorType = errorTypeListBox.getSelectedItemText().trim();
            if(ERROR_TYPE_WARNING.equals(sErrorType))
                errorTypeSearch = "WARNING";
            else if(ERROR_TYPE_ERROR.equals(sErrorType))
                errorTypeSearch = "ERROR";
            else if(ERROR_TYPE_FATAL.equals(sErrorType))
                errorTypeSearch = "FATAL";

            metadata.put( "lprmeta.errorType", errorTypeSearch );
        }

        if(recievedValidFromDate.getValue() != null && recievedValidFromDate.getValue().getTime() > 0) {
            metadata.put( "lprmeta.recievedValidFromDate", recievedValidFromDate.getValue().getTime() );
        }

        if(recievedValidToDate.getValue() != null && recievedValidToDate.getValue().getTime() > 0) {
            metadata.put( "lprmeta.recievedValidToDate", recievedValidToDate.getValue().getTime() );
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

        Date searchCreatedAfter = null;
        if ( createdAfter.getValue() != null ) {
            hasSomeDateValue = true;
            searchCreatedAfter = createdAfter.getValue();
        }

        Date searchCreatedBefore = null;
        if ( createdBefore.getValue() != null ) {
            hasSomeDateValue = true;
            searchCreatedBefore = createdBefore.getValue();
        }

        if ( lastModifiedAfter.getValue() != null ) {
            hasSomeDateValue = true;
        }

        if ( lastModifiedBefore.getValue() != null ) {
            hasSomeDateValue = true;
        }

        if ( metadata.size() == 0 && !hasSomeDateValue ) {
//            formGroup.setValidationState( ValidationState.ERROR );
//            Alert alert = new Alert( Constants.INSTANCE.AtLeastOneFieldMustBeSet(), AlertType.DANGER );
//            alert.setVisible( true );
//            alert.setDismissable( true );
//            errorPanel.add( alert );
//            return;
            searchCreatedAfter = new Date(Long.MIN_VALUE);
            searchCreatedBefore = new Date(2500, 12, 12, 23, 59);
        }

        final SearchResultTable queryTable = new SearchResultTable( new QueryMetadataPageRequest( metadata,
                searchCreatedAfter, searchCreatedBefore,
                                                                                                  lastModifiedAfter.getValue(), lastModifiedBefore.getValue(),
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

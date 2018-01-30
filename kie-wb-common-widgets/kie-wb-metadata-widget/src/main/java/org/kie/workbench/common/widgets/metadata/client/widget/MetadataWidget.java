/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.widgets.metadata.client.widget;

import java.util.Date;
import javax.annotation.PostConstruct;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import org.guvnor.common.services.shared.metadata.model.LprErrorType;
import org.guvnor.common.services.shared.metadata.model.LprRuleGroup;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.extras.datepicker.client.ui.base.events.ClearDateEvent;
import org.gwtbootstrap3.extras.datepicker.client.ui.base.events.ClearDateHandler;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
import org.kie.workbench.common.widgets.metadata.client.resources.ImageResources;
import org.kie.workbench.common.widgets.metadata.client.resources.i18n.MetadataConstants;
import org.uberfire.backend.vfs.impl.LockInfo;
import org.uberfire.ext.widgets.common.client.common.BusyIndicatorView;
import org.uberfire.ext.widgets.common.client.common.DatePicker;
import org.uberfire.ext.widgets.common.client.common.HasBusyIndicator;
import org.uberfire.ext.widgets.common.client.common.NumericLongTextBox;
import org.uberfire.ext.widgets.common.client.common.popups.YesNoCancelPopup;
import org.uberfire.mvp.Command;

import static org.uberfire.commons.validation.PortablePreconditions.*;

/**
 * This displays the metadata for a versionable artifact. It also captures
 * edits, but it does not load or save anything itself.
 */
public class MetadataWidget
        extends Composite
        implements HasBusyIndicator {

    interface Binder
            extends
            UiBinder<Widget, MetadataWidget> {

    }

    private static Binder uiBinder = GWT.create( Binder.class );

    private Metadata metadata = null;
    private boolean readOnly;

    private Runnable forceUnlockHandler;
    private String currentUser;

    @UiField
    TagWidget tags;
    @UiField
    FormControlStatic note;
    /*
    @UiField
    FormControlStatic uri;
    @UiField
    TextBox subject;
    @UiField
    TextBox type;
    @UiField
    TextBox external;
    @UiField
    TextBox source;
    */
    @UiField
    CheckBox isValidForLPRReports;
    @UiField
    CheckBox isValidForDUSASAbroadReports;
    @UiField
    CheckBox isValidForDUSASSpecialityReports;
    @UiField
    CheckBox isValidForPrimarySectorReports;
    @UiField
    DatePicker reportReceivedFrom;
    @UiField
    DatePicker reportReceivedTo;
    @UiField
    DatePicker encounterStartFromDate;
    @UiField
    DatePicker encounterStartToDate;
    @UiField
    DatePicker encounterEndFromDate;
    @UiField
    DatePicker encounterEndToDate;
    @UiField
    DatePicker episodeOfCareStartFromDate;
    @UiField
    DatePicker episodeOfCareStartToDate;
    @UiField
    NumericLongTextBox errorNumber;
    @UiField
    TextArea errorText;
    @UiField
    ListBox ruleGroup;
    @UiField
    ListBox errorType;
    @UiField
    FormControlStatic lockedBy;
    @UiField
    PushButton unlock;

    private BusyIndicatorView busyIndicatorView;

    public MetadataWidget( BusyIndicatorView busyIndicatorView ) {
        this.busyIndicatorView = busyIndicatorView;
        initWidget( uiBinder.createAndBindUi( this ) );
    }

    @PostConstruct
    public void init() {
        reportReceivedFrom.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        reportReceivedTo.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        encounterStartFromDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        encounterStartToDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        encounterEndFromDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        encounterEndToDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        episodeOfCareStartFromDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        episodeOfCareStartToDate.setFormat( ApplicationPreferences.getDroolsDateFormat() );
        ruleGroup.setMultipleSelect( false );


    }

    public void setContent( final Metadata metadata,
                            final boolean readOnly ) {
        this.metadata = checkNotNull( "metadata", metadata );
        this.readOnly = readOnly;

        loadData();
    }

    public void setForceUnlockHandler( final Runnable forceUnlockHandler ) {
        this.forceUnlockHandler = forceUnlockHandler;
    }

    public void setCurrentUser( String currentUser ) {
        this.currentUser = currentUser;
    }

    private void loadData() {
        initComponents();
        tags.setContent( metadata, this.readOnly );
        note.setText( metadata.getCheckinComment() );
/*

        uri.setText( metadata.getRealPath().toURI() );

        subject.setText( metadata.getSubject() );
        subject.addKeyUpHandler( new KeyUpHandler() {
            @Override
            public void onKeyUp( KeyUpEvent event ) {
                metadata.setSubject( subject.getText() );
            }
        } );

        type.setText( metadata.getType() );
        type.addKeyUpHandler( new KeyUpHandler() {
            @Override
            public void onKeyUp( KeyUpEvent event ) {
                metadata.setType( type.getText() );
            }
        } );

        external.setText( metadata.getExternalRelation() );
        external.addKeyUpHandler( new KeyUpHandler() {
            @Override
            public void onKeyUp( KeyUpEvent event ) {
                metadata.setExternalRelation( external.getText() );
            }
        } );

        source.setText( metadata.getExternalSource() );
        source.addKeyUpHandler( new KeyUpHandler() {
            @Override
            public void onKeyUp( KeyUpEvent event ) {
                metadata.setExternalSource( source.getText() );
            }
        } );
*/

        setLockStatus( metadata.getLockInfo() );
        reportReceivedFrom.setValue( metadata.getReportReceivedFromDate() > 0 ? new Date( metadata.getReportReceivedFromDate() ) : null );
        reportReceivedFrom.addValueChangeHandler( new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange( ValueChangeEvent<Date> event ) {
                metadata.setReportReceivedFromDate( reportReceivedFrom.getValue().getTime() );
            }
        } );
        reportReceivedFrom.addClearDateHandler( new ClearDateHandler() {
            @Override
            public void onClearDate( ClearDateEvent evt ) {
                metadata.setReportReceivedFromDate( Long.MIN_VALUE );
            }
        } );

        reportReceivedTo.setValue( metadata.getReportReceivedToDate() < Long.MAX_VALUE ? new Date( metadata.getReportReceivedToDate() ) : null );
        reportReceivedTo.addClearDateHandler( new ClearDateHandler() {
            @Override
            public void onClearDate( ClearDateEvent evt ) {
                metadata.setReportReceivedToDate( Long.MAX_VALUE );
            }
        } );
        reportReceivedTo.addValueChangeHandler( new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange( ValueChangeEvent<Date> event ) {
                metadata.setReportReceivedToDate( reportReceivedTo.getValue().getTime() );
            }
        } );

        encounterStartFromDate.setValue( metadata.getEncounterStartFromDate() > 0 ? new Date( metadata.getEncounterStartFromDate() ) : null );
        encounterStartFromDate.addValueChangeHandler( new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange( ValueChangeEvent<Date> event ) {
                metadata.setEncounterStartFromDate( encounterStartFromDate.getValue().getTime() );
            }
        } );
        encounterStartFromDate.addClearDateHandler( new ClearDateHandler() {
            @Override
            public void onClearDate( ClearDateEvent evt ) {
                metadata.setEncounterStartFromDate( Long.MIN_VALUE );
            }
        } );

        encounterStartToDate.setValue( metadata.getEncounterStartToDate() < Long.MAX_VALUE ? new Date( metadata.getEncounterStartToDate() ) : null );
        encounterStartToDate.addClearDateHandler( new ClearDateHandler() {
            @Override
            public void onClearDate( ClearDateEvent evt ) {
                metadata.setEncounterStartToDate( Long.MAX_VALUE );
            }
        } );
        encounterStartToDate.addValueChangeHandler( new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange( ValueChangeEvent<Date> event ) {
                metadata.setEncounterStartToDate( encounterStartToDate.getValue().getTime() );
            }
        } );

        encounterEndFromDate.setValue( metadata.getEncounterEndFromDate() > 0 ? new Date( metadata.getEncounterEndFromDate() ) : null );
        encounterEndFromDate.addValueChangeHandler( new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange( ValueChangeEvent<Date> event ) {
                metadata.setEncounterEndFromDate( encounterEndFromDate.getValue().getTime() );
            }
        } );
        encounterEndFromDate.addClearDateHandler( new ClearDateHandler() {
            @Override
            public void onClearDate( ClearDateEvent evt ) {
                metadata.setEncounterEndFromDate( Long.MIN_VALUE );
            }
        } );

        encounterEndToDate.setValue( metadata.getEncounterEndToDate() < Long.MAX_VALUE ? new Date( metadata.getEncounterEndToDate() ) : null );
        encounterEndToDate.addClearDateHandler( new ClearDateHandler() {
            @Override
            public void onClearDate( ClearDateEvent evt ) {
                metadata.setEncounterEndToDate( Long.MAX_VALUE );
            }
        } );
        encounterEndToDate.addValueChangeHandler( new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange( ValueChangeEvent<Date> event ) {
                metadata.setEncounterEndToDate( encounterEndToDate.getValue().getTime() );
            }
        } );

        episodeOfCareStartFromDate.setValue( metadata.getEpisodeOfCareStartFromDate() > 0 ? new Date( metadata.getEpisodeOfCareStartFromDate() ) : null );
        episodeOfCareStartFromDate.addValueChangeHandler( new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange( ValueChangeEvent<Date> event ) {
                metadata.setEpisodeOfCareStartFromDate( episodeOfCareStartFromDate.getValue().getTime() );
            }
        } );
        episodeOfCareStartFromDate.addClearDateHandler( new ClearDateHandler() {
            @Override
            public void onClearDate( ClearDateEvent evt ) {
                metadata.setEpisodeOfCareStartFromDate( Long.MIN_VALUE );
            }
        } );

        episodeOfCareStartToDate.setValue( metadata.getEpisodeOfCareStartToDate() < Long.MAX_VALUE ? new Date( metadata.getEpisodeOfCareStartToDate() ) : null );
        episodeOfCareStartToDate.addClearDateHandler( new ClearDateHandler() {
            @Override
            public void onClearDate( ClearDateEvent evt ) {
                metadata.setEpisodeOfCareStartToDate( Long.MAX_VALUE );
            }
        } );
        episodeOfCareStartToDate.addValueChangeHandler( new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange( ValueChangeEvent<Date> event ) {
                metadata.setEpisodeOfCareStartToDate( episodeOfCareStartToDate.getValue().getTime() );
            }
        } );

        isValidForLPRReports.setValue( metadata.isValidForLPRReports() );
        isValidForLPRReports.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                metadata.setValidForLPRReports( isValidForLPRReports.getValue() );
            }
        } );

        isValidForDUSASAbroadReports.setValue( metadata.isValidForDUSASAbroadReports() );
        isValidForDUSASAbroadReports.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                metadata.setValidForDUSASAbroadReports( isValidForDUSASAbroadReports.getValue() );
            }
        } );

        isValidForDUSASSpecialityReports.setValue( metadata.isValidForDUSASSpecialityReports() );
        isValidForDUSASSpecialityReports.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                metadata.setValidForDUSASSpecialityReports( isValidForDUSASSpecialityReports.getValue() );
            }
        } );

        isValidForPrimarySectorReports.setValue( metadata.isValidForPrimarySectorReports() );
        isValidForPrimarySectorReports.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                metadata.setValidForPrimarySectorReports( isValidForPrimarySectorReports.getValue() );
            }
        } );

        errorNumber.setValue( metadata.getErrorNumber().toString() );
        errorNumber.addValueChangeHandler( new ValueChangeHandler<String>() {
            @Override
            public void onValueChange( ValueChangeEvent<String> event ) {
                final Long lValue = Long.parseLong( errorNumber.getValue() );
                metadata.setErrorNumber( lValue );
            }
        } );

        errorText.setValue( metadata.getErrorText() );
        errorText.addValueChangeHandler( new ValueChangeHandler<String>() {
            @Override
            public void onValueChange( ValueChangeEvent<String> event ) {
                metadata.setErrorText( errorText.getText() );
            }
        } );

        selectItemInListBox( ruleGroup, metadata.getRuleGroup().toString() );
        ruleGroup.addChangeHandler( new ChangeHandler() {
            @Override
            public void onChange( ChangeEvent changeEvent ) {
                metadata.setRuleGroup( LprRuleGroup.valueOf( ruleGroup.getSelectedValue() ) );
            }
        } );

        selectItemInListBox( errorType, metadata.getErrorType().toString() );
        errorType.addChangeHandler( new ChangeHandler() {
            @Override
            public void onChange( ChangeEvent changeEvent ) {
                metadata.setErrorType( LprErrorType.valueOf( errorType.getSelectedValue() ) );
            }
        } );
    }

    private void initComponents() {
        for ( LprRuleGroup ruleGroup : LprRuleGroup.values() ) {
            this.ruleGroup.addItem( ruleGroup.getDisplayText(), ruleGroup.toString() );
        }
        ruleGroup.setSelectedIndex( 0 );

        for ( LprErrorType lprErrorType : LprErrorType.values() ) {
            errorType.addItem( lprErrorType.getDisplayText(), lprErrorType.toString() );
        }
        errorType.setSelectedIndex( 0 );
    }

    private void selectItemInListBox( ListBox listBox, String value ) {
        int itemCount = listBox.getItemCount();
        for ( int i = 0; i < itemCount; i++ ) {
            String listBoxValue = listBox.getValue( i );
            if ( listBoxValue.equals( value ) ) {
                listBox.setSelectedIndex( i );
                //DomEvent.fireNativeEvent(Document.get().createChangeEvent(), listBox);
                return;
            }
        }
    }

    public void setLockStatus( final LockInfo lockInfo ) {
        lockedBy.setText( getLockStatusText( lockInfo ) );
        maybeShowForceUnlockButton( lockInfo );
    }

    String getLockStatusText( final LockInfo lockInfo ) {
        final String lockStatusText;

        if ( lockInfo.isLocked() ) {
            if ( lockInfo.lockedBy().equals( currentUser ) ) {
                lockStatusText = MetadataConstants.INSTANCE.LockedByHintOwned();
            } else {
                lockStatusText = MetadataConstants.INSTANCE.LockedByHint() + " " + lockInfo.lockedBy();
            }

        } else {
            lockStatusText = MetadataConstants.INSTANCE.UnlockedHint();
        }

        return lockStatusText;
    }

    private void maybeShowForceUnlockButton( final LockInfo lockInfo ) {
        final Image unlockImage = new Image( ImageResources.INSTANCE.unlock() );
        unlock.setHTML( "<span>" + unlockImage.toString() + " " + unlock.getText() + "</span>" );
        unlock.getElement().setAttribute( "data-uf-lock", "false" );
        unlock.setVisible( lockInfo.isLocked() );
        unlock.setEnabled( true );
    }

    @Deprecated
    public Metadata getContent() {
        return metadata;
    }

    @Override
    public void showBusyIndicator( final String message ) {
        busyIndicatorView.showBusyIndicator( message );
    }

    @Override
    public void hideBusyIndicator() {
        busyIndicatorView.hideBusyIndicator();
    }

    public void setNote( String text ) {
        note.setText( text );
    }

    @UiHandler("unlock")
    public void onForceUnlock( ClickEvent e ) {
        final YesNoCancelPopup yesNoCancelPopup =
                YesNoCancelPopup.newYesNoCancelPopup( MetadataConstants.INSTANCE.ForceUnlockConfirmationTitle(),
                        MetadataConstants.INSTANCE.ForceUnlockConfirmationText( metadata.getLockInfo().lockedBy() ),
                        new Command() {
                            @Override
                            public void execute() {
                                forceUnlockHandler.run();
                                unlock.setEnabled( false );
                            }
                        },
                        new Command() {

                            @Override
                            public void execute() {
                            }
                        },
                        null );
        yesNoCancelPopup.setClosable( false );
        yesNoCancelPopup.show();
    }
}


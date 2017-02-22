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

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.TextBox;
//import org.gwtbootstrap3.extras.datepicker.client.ui.DatePicker;
import org.uberfire.ext.widgets.common.client.common.DatePicker;
import org.kie.workbench.common.widgets.metadata.client.resources.ImageResources;
import org.kie.workbench.common.widgets.metadata.client.resources.i18n.MetadataConstants;
import org.uberfire.backend.vfs.impl.LockInfo;
import org.uberfire.ext.widgets.common.client.common.BusyIndicatorView;
import org.uberfire.ext.widgets.common.client.common.HasBusyIndicator;
import org.uberfire.ext.widgets.common.client.common.popups.YesNoCancelPopup;
import org.uberfire.mvp.Command;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;

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
    @UiField
    CheckBox inProduction;
    @UiField
    CheckBox isDraft;
    @UiField
    DatePicker validFrom;
    @UiField
    DatePicker validTo;
    @UiField
    FormControlStatic lockedBy;
    @UiField
    PushButton unlock;

    private BusyIndicatorView busyIndicatorView;

    public MetadataWidget( BusyIndicatorView busyIndicatorView ) {
        this.busyIndicatorView = busyIndicatorView;
        initWidget( uiBinder.createAndBindUi( this ) );
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

        tags.setContent( metadata, this.readOnly );

        note.setText( metadata.getCheckinComment() );

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

        setLockStatus(metadata.getLockInfo());

        Long validFromTS = metadata.getValidFrom();
        validFrom.setValue( new Date(validFromTS ));
        validFrom.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(ValueChangeEvent<Date> event) {
                Date date = validFrom.getValue();
                Long validFromTimeStramp = date.getTime();
                metadata.setValidFrom(validFromTimeStramp);
            }
        });

        Long validToTS = metadata.getValidTo();
        validTo.setValue(new Date(validToTS));
        validTo.addValueChangeHandler(new ValueChangeHandler<Date>() {
              @Override
              public void onValueChange(ValueChangeEvent<Date> event) {
                    Date date = validTo.getValue();
                    Long validToTimestamp = date.getTime();
                    metadata.setValidTo(validToTimestamp);
              }
          });

        isDraft.setValue(metadata.isDraft());
        isDraft.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                boolean x = isDraft.getValue();
                metadata.setDraft(x);
            }
        });

        inProduction.setValue(metadata.isInProduction());
        inProduction.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                boolean z = inProduction.getValue();
                metadata.setInProduction(z);
            }
        });
    }

    public void setLockStatus(final LockInfo lockInfo) {
        lockedBy.setText( getLockStatusText( lockInfo ) );
        maybeShowForceUnlockButton(lockInfo);
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
    
    private void maybeShowForceUnlockButton(final LockInfo lockInfo) {
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

    @UiHandler( "unlock" )
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


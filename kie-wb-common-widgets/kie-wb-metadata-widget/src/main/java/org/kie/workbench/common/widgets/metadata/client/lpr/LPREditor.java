package org.kie.workbench.common.widgets.metadata.client.lpr;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.IsWidget;
import org.guvnor.common.services.shared.message.Level;
import org.guvnor.common.services.shared.metadata.model.LprRuleType;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.guvnor.common.services.shared.validation.ValidationService;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.guvnor.messageconsole.events.PublishMessagesEvent;
import org.guvnor.messageconsole.events.SystemMessage;
import org.guvnor.messageconsole.events.UnpublishMessagesEvent;
import org.gwtbootstrap3.client.ui.Button;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.container.IOC;
import org.kie.workbench.common.services.shared.lpr.LPRManageProductionService;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
import org.kie.workbench.common.widgets.client.popups.validation.ValidationPopup;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.kie.workbench.common.widgets.metadata.client.KieEditor;
import org.kie.workbench.common.widgets.metadata.client.KieEditorView;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartTitleDecoration;
import org.uberfire.client.mvp.UpdatedLockStatusEvent;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.ext.editor.commons.client.file.ArchivePopup;
import org.uberfire.ext.editor.commons.client.file.MoveToProductionPopup;
import org.uberfire.ext.editor.commons.client.file.SaveOperationService;
import org.uberfire.ext.editor.commons.client.history.SaveButton;
import org.uberfire.ext.widgets.common.client.callbacks.DefaultErrorCallback;
import org.uberfire.mvp.Command;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.MenuItem;

/**
 * Created on 31-08-2017.
 */
public abstract class LPREditor extends KieEditor {

    private Command moveToProdCmdWaitingForValidation = null;
    private final static String VALIDATION_ERROR_MESSAGE_TYPE = "VALIDATION_ERROR_";
    private boolean lockedByOtherUser;

    @Inject
    private Caller<LPRManageProductionService> lprProdService;

    @Inject
    private SessionInfo sessionInfo;

    @Inject
    private Event<PublishMessagesEvent> publishMessages;

    @Inject
    private Event<UnpublishMessagesEvent> unpublishMessages;

    @Inject
    private LPRFileMenuBuilder lprMenuBuilder;

    public LPREditor() {
    }

    public LPREditor( KieEditorView baseView ) {
        super( baseView );
    }

    @Override
    protected void makeMenuBar() {
        menus = lprMenuBuilder
                .addSave( versionRecordManager.newSaveMenuItem( new Command() {
                    @Override
                    public void execute() {
                        onSave();
                    }
                } ) )
                .addDelete( versionRecordManager.getPathToLatest() )
                .addRename( versionRecordManager.getPathToLatest(), fileNameValidator )
                .addCopy( versionRecordManager.getCurrentPath(), fileNameValidator )
                .addMoveToProduction( new MoveToProductionCommand() )
                .addArchive( new ArchiveCommand() )
//                .addSimulate( new SimulateCommand() )
                .addValidate( onValidate() )
                .addVersionMenu( versionRecordManager.buildMenu() )
                .build();

    }

    @WorkbenchPartTitleDecoration
    public IsWidget getTitle() {
        return super.getTitle();
    }

    @WorkbenchPartTitle
    public String getTitleText() {
        String titleText = versionRecordManager.getCurrentPath().getFileName(); //filename
        titleText = titleText.substring( 0, titleText.lastIndexOf( '.' ) ); //strip extension
        if ( metadata != null ) {
            //add rule status
            DateTimeFormat dateFormatter = DateTimeFormat.getFormat( ApplicationPreferences.getDroolsDateFormat() );
            if ( metadata.getProductionDate() > 0 ) {
                titleText += " - Produktionssat d. " + dateFormatter.format( new Date( metadata.getProductionDate() ) );
            } else {
                titleText += " - Kladde";
            }
            if ( metadata.getArchivedDate() > 0 ) {
                titleText += " - Arkiveret d. " + dateFormatter.format( new Date( metadata.getArchivedDate() ) );
            }
        }
        return titleText;
    }

    @Override
    protected void resetEditorPages( Overview overview ) {
        super.resetEditorPages( overview );
        if ( metadata != null ) {
            metadata.setRuleType( LprRuleType.REPORT_VALIDATION ); //Mark resources managed by this editor as LPR rules
        }
        updateUI();
    }

    @Override
    protected RemoteCallback<Path> getSaveSuccessCallback( final int newHash ) {
        final RemoteCallback<Path> superSaveSuccess = super.getSaveSuccessCallback( newHash );
        return new RemoteCallback<Path>() {
            @Override
            public void callback( final Path path ) {
                superSaveSuccess.callback( path );
                updateUI();
                if ( metadata != null && metadata.getProductionDate() > 0 ) {
                    //rule was just put into production - copy the rule to prod branch so it is ready bo build & deploy
                    lprProdService.call(
                            new RemoteCallback<Path>() {
                                @Override
                                public void callback( Path path ) {
                                    notification.fire( new NotificationEvent( CommonConstants.INSTANCE.LPRItemMoveToProductionSuccessfully(), NotificationEvent.NotificationType.SUCCESS ) );
                                }
                            },
                            new ErrorCallback<Message>() {
                                @Override
                                public boolean error( Message message, Throwable throwable ) { //exception already logged by DroolsLoggingToDBInterceptor
                                    //todo ttn unit test this rollback behaviour
                                    metadata.setProductionDate( 0L );
                                    save( "Produktionsættelse rullet tilbage pga. systemfejl" );
                                    return new DefaultErrorCallback().error( message, throwable );
                                }
                            } ).copyToProductionBranch( path );
                }
            }
        };
    }

    private void updateUI() {
        updateEnabledStateOnMenuItems();
        baseView.refreshTitle( getTitleText() );
        changeTitleNotification.fire( new ChangeTitleWidgetEvent( place, getTitleText(), getTitle() ) );
        if ( metadata != null && metadata.getArchivedDate() > 0 ) {
            isReadOnly = true;
            reload(); //we have to reload to change the view to readonly mode
        }
    }

    private void onEditorLockInfo( @Observes UpdatedLockStatusEvent lockInfo ) {
        lockedByOtherUser = !(!lockInfo.isLocked() || lockInfo.isLockedByCurrentUser());
        if ( lockedByOtherUser ) {
            for ( MenuItem menuItem : lprMenuBuilder.getMenuItemsSyncedWithLockState() ) {
                menuItem.setEnabled( false );
            }
        } else {
            updateEnabledStateOnMenuItems();
        }
    }

    private void updateEnabledStateOnMenuItems() {
        if ( this.metadata != null ) {
            for ( MenuItem mi : menus.getItemsMap().values() ) {

                if ( mi instanceof SaveButton ) {
                    Button button = ( Button ) (( SaveButton ) mi).build();
                    if ( CommonConstants.INSTANCE.Save().equals( button.getText() ) ) {
                        //only allow save if rule is not archived
                        boolean enabled = !isReadOnly && this.metadata.getArchivedDate() == 0L && versionRecordManager.isCurrentLatest();
                        mi.setEnabled( enabled );
                    }
                    if ( CommonConstants.INSTANCE.Restore().equals( button.getText() ) ) {
                        //only allow restore if rule is not archived and restore should save the new version in draft status
                        //todo ttn restore is disabled until this is implemented - see issue LPR-1357
                        mi.setEnabled( false );
                    }
                }

                //only allow copy if rule is not archived
                if ( CommonConstants.INSTANCE.Copy().equals( mi.getCaption() ) ) {
                    boolean enabled = this.metadata.getArchivedDate() == 0L;
                    mi.setEnabled( enabled );
                }

                //only allow delete, rename or 'move to production' if rule is not in production and not archived
                if ( CommonConstants.INSTANCE.Delete().equals( mi.getCaption() ) ||
                        CommonConstants.INSTANCE.Rename().equals( mi.getCaption() ) ||
                        CommonConstants.INSTANCE.LPRMoveToProduction().equals( mi.getCaption() ) ) {
                    boolean enabled = !isReadOnly && this.metadata.getProductionDate() == 0 && this.metadata.getArchivedDate() == 0 && versionRecordManager.isCurrentLatest();
                    mi.setEnabled( enabled );
                }

                //only allow archive if rule is in production and not already archived
                if ( CommonConstants.INSTANCE.LPRArchive().equals( mi.getCaption() ) ) {
                    boolean enabled = !isReadOnly && this.metadata.getProductionDate() > 0 && this.metadata.getArchivedDate() == 0 && versionRecordManager.isCurrentLatest();
                    mi.setEnabled( enabled );
                }

            }
        }
    }

    @SuppressWarnings("unchecked")
    protected Command getValidationCallback( final Caller<? extends ValidationService> validationService, final KieEditorView view ) {
        return new Command() {
            @Override
            public void execute() {
                validationService.call( new RemoteCallback<List<ValidationMessage>>() {
                    @Override
                    public void callback( final List<ValidationMessage> results ) {
                        String fileName = versionRecordManager.getCurrentPath().getFileName();
                        unpublishErrorMessages( VALIDATION_ERROR_MESSAGE_TYPE + fileName ); //remove any old error logs about validation errors
                        if ( results == null || results.isEmpty() ) {
                            notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemValidatedSuccessfully(),
                                    NotificationEvent.NotificationType.SUCCESS ) );

                            if ( moveToProdCmdWaitingForValidation != null ) {
                                moveToProdCmdWaitingForValidation.execute();
                            }
                        } else {
                            moveToProdCmdWaitingForValidation = null;
                            for ( ValidationMessage result : results ) {
                                publishErrorMessage( result.getText(), VALIDATION_ERROR_MESSAGE_TYPE + fileName, result.getColumn(), result.getLine() );
                            }
                            ValidationPopup.showMessages( results );
                        }
                    }
                }, new DefaultErrorCallback() ).validate( versionRecordManager.getCurrentPath(), view.getContent() );
            }
        };
    }

    private void publishErrorMessage( String errorText, String messageType, int column, int line ) {
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setText( errorText );
        systemMessage.setColumn( column );
        systemMessage.setLine( line );
        systemMessage.setMessageType( messageType );
        systemMessage.setLevel( Level.ERROR );
        systemMessage.setPath( versionRecordManager.getCurrentPath() );
        PublishMessagesEvent event = new PublishMessagesEvent();
        event.setSessionId( sessionInfo.getId() );
        event.setMessagesToPublish( Collections.singletonList( systemMessage ) );
        publishMessages.fire( event );
    }

    /**
     * Deletes all published messages for the current session with the given type
     * @param messageType null to delete all messages regardless of type
     */
    private void unpublishErrorMessages( String messageType ) {
        UnpublishMessagesEvent event = new UnpublishMessagesEvent();
        event.setSessionId( sessionInfo.getId() );
        event.setUserId( sessionInfo.getIdentity().getIdentifier() );
        event.setMessageType( messageType );
        unpublishMessages.fire( event );
    }

    protected abstract Integer getCurrentHash();

    private class MoveToProductionCommand implements Command {
        private final String MOVE_TO_PROD_DIRTY_ERROR_MESSAGE_TYPE = "MOVE_TO_PROD_DIRTY_ERROR_";
        private final String MOVE_TO_PROD_SAVE_ERROR_MESSAGE_TYPE = "MOVE_TO_PROD_SAVE_ERROR_";

        @Override
        public void execute() {
            //check rule has no unsaved changes before moving to production (to prevent saving unintentional changes)
            String fileName = versionRecordManager.getCurrentPath().getFileName();
            if ( overviewWidget.isDirty() || isDirty( getCurrentHash() ) ) {
                String errorText = fileName + " kan ikke produktionssættes. Reglen har ikke-gemte ændringer. Gem eller fjern ændringerne.";
                NotificationEvent errorEvent = new NotificationEvent( errorText, NotificationEvent.NotificationType.ERROR );
                notification.fire( errorEvent );
                publishErrorMessage( errorText, MOVE_TO_PROD_DIRTY_ERROR_MESSAGE_TYPE + fileName, 0, 0 );
                return; //abort command
            }
            unpublishErrorMessages( MOVE_TO_PROD_DIRTY_ERROR_MESSAGE_TYPE + fileName ); //remove any old error logs about unsaved changes

            //check rule has no validation errors before moving to production
            moveToProdCmdWaitingForValidation = getShowPopupCommand();
            onValidate().execute();
        }

        private Command getShowPopupCommand() {
            return new Command() {
                @Override
                public void execute() {
                    moveToProdCmdWaitingForValidation = null; //we have executed, remove ourselves from the waiting position
                    MoveToProductionPopup popup = new MoveToProductionPopup( getSaveInProdCommand() );
                    popup.show();
                }
            };
        }

        private Command getSaveInProdCommand() {
            return new Command() {
                @Override
                public void execute() {
                    //check state is as expected
                    String fileName = versionRecordManager.getCurrentPath().getFileName();
                    String errorText = null;
                    if ( lockedByOtherUser ) {
                        errorText = fileName + " kan ikke produktionssættes: Reglen er låst af en anden bruger";
                    }
                    if ( concurrentUpdateSessionInfo != null ) {
                        errorText = fileName + " kan ikke produktionssættes: Reglen blev ændret samtidigt andetsteds fra";
                    }
                    if ( isReadOnly ) {
                        errorText = fileName + " kan ikke produktionssættes: Reglen er skrivebeskyttet";
                    }
                    if ( !versionRecordManager.isCurrentLatest() ) {
                        errorText = fileName + " kan ikke produktionssættes: Kun nyeste version kan produktionssættes";
                    }
                    if ( metadata.getProductionDate() > 0 ) {
                        errorText = fileName + " kan ikke produktionssættes: Reglen er allerede i produktion";
                    }
                    if ( metadata.getArchivedDate() > 0 ) {
                        errorText = fileName + " kan ikke produktionssættes: Reglen er arkiveret";
                    }
                    if ( errorText != null ) {
                        publishErrorMessage( errorText, MOVE_TO_PROD_SAVE_ERROR_MESSAGE_TYPE + fileName, 0, 0 );
                        notification.fire( new NotificationEvent( errorText, NotificationEvent.NotificationType.ERROR ) );
                        updateEnabledStateOnMenuItems();
                        return; //abort command
                    }
                    unpublishErrorMessages( MOVE_TO_PROD_SAVE_ERROR_MESSAGE_TYPE + fileName ); //remove any old error logs about save errors

                    //set metadata and save rule
                    metadata.setProductionDate( new Date().getTime() );
                    baseView.showSaving();
                    save( "Produktionsættelse" );
                    concurrentUpdateSessionInfo = null;
                    final SaveOperationService.SaveOperationNotifier notifier = IOC.getBeanManager().lookupBean( SaveOperationService.SaveOperationNotifier.class ).getInstance();
                    notifier.notify( versionRecordManager.getCurrentPath() );
                }
            };
        }
    }

    private class ArchiveCommand implements Command {
        private final String ARCHIVE_DIRTY_ERROR_MESSAGE_TYPE = "ARCHIVE_DIRTY_ERROR_";
        private final String ARCHIVE_SAVE_ERROR_MESSAGE_TYPE = "ARCHIVE_SAVE_ERROR_";

        @Override
        public void execute() {
            //check rule has no unsaved changes before moving to production (to prevent saving unintentional changes)
            String fileName = versionRecordManager.getCurrentPath().getFileName();
            if ( overviewWidget.isDirty() || isDirty( getCurrentHash() ) ) {
                String errorText = fileName + " kan ikke arkiveres. Reglen har ikke-gemte ændringer. Gem eller fjern ændringerne.";
                NotificationEvent errorEvent = new NotificationEvent( errorText, NotificationEvent.NotificationType.ERROR );
                notification.fire( errorEvent );
                publishErrorMessage( errorText, ARCHIVE_DIRTY_ERROR_MESSAGE_TYPE + fileName, 0, 0 );
                return; //abort command
            }
            unpublishErrorMessages( ARCHIVE_DIRTY_ERROR_MESSAGE_TYPE + fileName ); //remove old error logs about unsaved changes for this rule file

            ArchivePopup popup = new ArchivePopup( new Command() {
                @Override
                public void execute() {
                    //check state is as expected
                    String fileName = versionRecordManager.getCurrentPath().getFileName();
                    NotificationEvent errorEvent = null;
                    String errorMsg = null;
                    if ( lockedByOtherUser ) {
                        errorMsg = fileName + " kan ikke arkiveres: Reglen er låst af en anden bruger";
                    }
                    if ( concurrentUpdateSessionInfo != null ) {
                        errorMsg = fileName + " kan ikke arkiveres: Reglen blev ændret samtidigt andetsteds fra";
                    }
                    if ( isReadOnly ) {
                        errorMsg = fileName + " kan ikke arkiveres: Reglen er skrivebeskyttet";
                    }
                    if ( !versionRecordManager.isCurrentLatest() ) {
                        errorMsg = fileName + " kan ikke arkiveres: Kun nyeste version kan arkiveres";
                    }
                    if ( metadata.getProductionDate() == 0 ) {
                        errorMsg = fileName + " kan ikke arkiveres: Reglen er ikke i produktion";
                    }
                    if ( metadata.getArchivedDate() > 0 ) {
                        errorMsg = fileName + " kan ikke arkiveres: Reglen er allerede arkiveret";
                    }
                    if ( errorMsg != null ) {
                        publishErrorMessage( errorMsg, ARCHIVE_SAVE_ERROR_MESSAGE_TYPE + fileName, 0, 0 );
                        notification.fire( new NotificationEvent( errorMsg, NotificationEvent.NotificationType.ERROR ) );
                        updateEnabledStateOnMenuItems();
                        return; //abort command
                    }
                    unpublishErrorMessages( ARCHIVE_SAVE_ERROR_MESSAGE_TYPE + fileName ); //remove any old error logs about save errors

                    //set metadata and save rule
                    metadata.setArchivedDate( new Date().getTime() );
                    baseView.showSaving();
                    save( "Arkivering" );
                    concurrentUpdateSessionInfo = null;
                    final SaveOperationService.SaveOperationNotifier notifier = IOC.getBeanManager().lookupBean( SaveOperationService.SaveOperationNotifier.class ).getInstance();
                    notifier.notify( versionRecordManager.getCurrentPath() );
                }
            } );
            popup.show();
        }

    }

    private class SimulateCommand implements Command {
        @Override
        public void execute() {
            notification.fire( new NotificationEvent( "Not yet implemented",
                    NotificationEvent.NotificationType.ERROR ) );
        }

    }

}

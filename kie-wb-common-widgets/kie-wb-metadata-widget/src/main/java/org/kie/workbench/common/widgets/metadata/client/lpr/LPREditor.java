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
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.guvnor.common.services.shared.validation.ValidationService;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.guvnor.messageconsole.events.PublishMessagesEvent;
import org.guvnor.messageconsole.events.SystemMessage;
import org.guvnor.messageconsole.events.UnpublishMessagesEvent;
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
import org.uberfire.ext.editor.commons.client.file.LPRDeletePopup;
import org.uberfire.ext.editor.commons.client.file.MoveToProductionPopup;
import org.uberfire.ext.editor.commons.client.file.SaveOperationService;
import org.uberfire.ext.editor.commons.client.menu.MenuItems;
import org.uberfire.ext.editor.commons.service.DeleteService;
import org.uberfire.ext.editor.commons.version.VersionService;
import org.uberfire.ext.widgets.common.client.callbacks.DefaultErrorCallback;
import org.uberfire.ext.widgets.common.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.uberfire.mvp.Command;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.MenuItem;

/**
 * Created on 31-08-2017.
 */
public abstract class LPREditor extends KieEditor {

    private Command moveToProdCmdWaitingForValidation = null;
    public final static String VALIDATION_ERROR = "VALIDATION_ERROR_";
    private boolean lockedByOtherUser;

    @Inject
    protected Caller<LPRManageProductionService> lprProdService;

    @Inject
    private Caller<DeleteService> deleteService;

    @Inject
    private Caller<VersionService> versionService;

    @Inject
    protected SessionInfo sessionInfo;

    @Inject
    protected Event<PublishMessagesEvent> publishMessages;

    @Inject
    protected Event<UnpublishMessagesEvent> unpublishMessages;

    @Inject
    protected LPRFileMenuBuilder lprMenuBuilder;

    public LPREditor() {
    }

    public LPREditor( KieEditorView baseView ) {
        super( baseView );
    }

    protected abstract Integer getCurrentHash();

    protected abstract void setDroolsMetadata();

    @Override
    protected void makeMenuBar() {
        menus = lprMenuBuilder
                //restore is permanently disabled as restoring a prod version does not actually put it into production and restoring an archived rule is also messy
                //todo ttn Restore should be implemented, see LPR-1357
                .addSave( new Command() {
                    @Override
                    public void execute() {
                        metadata.setProductionDate( 0L ); //save as draft version
                        onSave();
                    }
                } )
                .addDelete( new DeleteCommand() )
                //rename is permanently disabled as it truncates version history and this screws up the the draft/prod/archive LPR rule lifecycle
                //.addRename( versionRecordManager.getPathToLatest(), fileNameValidator )
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
        int endIndex = titleText.lastIndexOf( '.' ); //get extension index
        titleText = endIndex < 0 ? titleText : titleText.substring( 0, endIndex ); //strip extension
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
        updateUI( false ); //no need to reload - we are in the process of loading
    }

    @Override
    protected RemoteCallback<Path> getSaveSuccessCallback( final int newHash ) {
        final RemoteCallback<Path> superSaveSuccess = super.getSaveSuccessCallback( newHash );
        return new RemoteCallback<Path>() {
            @Override
            public void callback( final Path path ) {
                superSaveSuccess.callback( path );
                if ( metadata != null && metadata.getProductionDate() > 0 && metadata.getArchivedDate() == 0 ) {
                    //rule was just put into production - copy the rule to prod branch so it is ready to build & deploy
                    copyToProd( path );

                } else if ( metadata != null && metadata.getArchivedDate() > 0 ) {
                    //rule was archived - delete the rule in prod branch
                    deleteFromProd( path );
                }
                updateUI( true ); //we have to reload after saving to change the view to readonly mode
            }

            private void copyToProd( Path path ) {
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
                                metadata.setProductionDate( 0L );
                                save( "Produktionsættelse rullet tilbage pga. systemfejl" );
                                return new DefaultErrorCallback().error( message, throwable );
                            }
                        } ).copyToProductionBranch( path );
            }

            private void deleteFromProd( Path path ) {
                lprProdService.call(
                        new RemoteCallback<Path>() {
                            @Override
                            public void callback( Path path ) {
                                notification.fire( new NotificationEvent( CommonConstants.INSTANCE.LPRItemArchivedSuccessfully(), NotificationEvent.NotificationType.SUCCESS ) );
                            }
                        },
                        new ErrorCallback<Message>() {
                            @Override
                            public boolean error( Message message, Throwable throwable ) { //exception already logged by DroolsLoggingToDBInterceptor
                                metadata.setArchivedDate( 0L );
                                save( "Arkivering rullet tilbage pga. systemfejl" );
                                return new DefaultErrorCallback().error( message, throwable );
                            }
                        } ).deleteFromProductionBranch( path );
            }
        };
    }

    @SuppressWarnings("unchecked")
    public Command getValidationCallback( final Caller<? extends ValidationService> validationService, final KieEditorView view ) {
        return new Command() {
            @Override
            public void execute() {
                validationService.call( new RemoteCallback<List<ValidationMessage>>() {
                    @Override
                    public void callback( final List<ValidationMessage> results ) {
                        String fileName = versionRecordManager.getCurrentPath().getFileName();
                        unpublishErrorMessages( VALIDATION_ERROR + fileName ); //remove any old error logs about validation errors
                        if ( results == null || results.isEmpty() ) {
                            notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemValidatedSuccessfully(),
                                    NotificationEvent.NotificationType.SUCCESS ) );

                            if ( moveToProdCmdWaitingForValidation != null ) {
                                moveToProdCmdWaitingForValidation.execute();
                            }
                        } else {
                            moveToProdCmdWaitingForValidation = null;
                            for ( ValidationMessage result : results ) {
                                publishErrorMessage( result.getText(), VALIDATION_ERROR + fileName, result.getColumn(), result.getLine() );
                            }
                            ValidationPopup.showMessages( results );
                        }
                    }
                }, new DefaultErrorCallback() ).validate( versionRecordManager.getCurrentPath(), view.getContent() );
            }
        };
    }


    /****************************************************************************************
     *                                  PRIVATE METHODS
     ****************************************************************************************/

    private void updateUI( boolean reloadIfArchived ) {
        updateEnabledStateOnMenuItems();
        changeTitleNotification.fire( new ChangeTitleWidgetEvent( place, getTitleText(), getTitle() ) );
        if ( metadata != null && metadata.getArchivedDate() > 0 && !isReadOnly ) {
            isReadOnly = true;
            if ( reloadIfArchived )
                reload();
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
        if ( this.metadata != null && menus != null ) {
            //only allow save if rule is not archived
            boolean isEnabled = !isReadOnly &&
                    this.metadata.getArchivedDate() == 0 &&
                    versionRecordManager.isCurrentLatest();
            menus.getItemsMap().get( MenuItems.SAVE ).setEnabled( isEnabled );

            //only allow delete and 'move to production' if rule is not in production and not archived
            isEnabled = !isReadOnly &&
                    this.metadata.getProductionDate() == 0 &&
                    this.metadata.getArchivedDate() == 0 &&
                    versionRecordManager.isCurrentLatest();
            menus.getItemsMap().get( MenuItems.DELETE ).setEnabled( isEnabled );
            menus.getItemsMap().get( MenuItems.MOVETOPRODUCTION ).setEnabled( isEnabled );

            //only allow archive if rule is in production and not already archived
            isEnabled = !isReadOnly &&
                    this.metadata.getProductionDate() > 0 &&
                    this.metadata.getArchivedDate() == 0 &&
                    versionRecordManager.isCurrentLatest();
            menus.getItemsMap().get( MenuItems.ARCHIVE ).setEnabled( isEnabled );
        }
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


    /****************************************************************************************
     *                                  UNIT TEST SUPPORT
     ****************************************************************************************/

    public void setMetadata( Metadata metadata ) {
        this.metadata = metadata;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public SaveOperationService.SaveOperationNotifier getSaveNotifier() {
        return IOC.getBeanManager().lookupBean( SaveOperationService.SaveOperationNotifier.class ).getInstance();
    }

    /****************************************************************************************
     *                                  PRIVATE CLASSES
     ****************************************************************************************/

    public class MoveToProductionCommand implements Command {
        public final static String MOVE_TO_PROD_DIRTY_ERROR_MESSAGE_TYPE = "MOVE_TO_PROD_DIRTY_ERROR_";
        public final static String MOVE_TO_PROD_SAVE_ERROR_MESSAGE_TYPE = "MOVE_TO_PROD_SAVE_ERROR_";

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
            moveToProdCmdWaitingForValidation = getShowPopupCommand( getSaveInProdCommand() );
            onValidate().execute();
        }

        public Command getShowPopupCommand( final Command popupOkCommand ) {
            return new Command() {
                @Override
                public void execute() {
                    moveToProdCmdWaitingForValidation = null; //we have executed, remove ourselves from the waiting position
                    MoveToProductionPopup popup = new MoveToProductionPopup( popupOkCommand );
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
                        return; //abort command
                    }
                    unpublishErrorMessages( MOVE_TO_PROD_SAVE_ERROR_MESSAGE_TYPE + fileName ); //remove any old error logs about save errors

                    //set metadata and save rule
                    metadata.setProductionDate( new Date().getTime() );
                    metadata.setHasProdVersion( true );
                    baseView.showSaving();
                    save( "Produktionsættelse" );
                    concurrentUpdateSessionInfo = null;
                    final SaveOperationService.SaveOperationNotifier notifier = getSaveNotifier();
                    notifier.notify( versionRecordManager.getCurrentPath() );
                }
            };
        }
    }

    public class ArchiveCommand implements Command {
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
                        return; //abort command
                    }
                    unpublishErrorMessages( ARCHIVE_SAVE_ERROR_MESSAGE_TYPE + fileName ); //remove any old error logs about save errors

                    //set metadata and save rule
                    metadata.setArchivedDate( new Date().getTime() );
                    baseView.showSaving();
                    save( "Arkivering" );
                    concurrentUpdateSessionInfo = null;
                    final SaveOperationService.SaveOperationNotifier notifier = getSaveNotifier();
                    notifier.notify( versionRecordManager.getCurrentPath() );
                }
            } );
            popup.show();
        }
    }

    private class DeleteCommand implements Command {
        @Override
        public void execute() {
            //check if a prod version exists
            if ( metadata != null && metadata.hasProdVersion() ) {
                //get prod version
                baseView.showBusyIndicator( CommonConstants.INSTANCE.Wait() );
                lprProdService.call(
                        getProdVersionCallback(),
                        new HasBusyIndicatorDefaultErrorCallback( baseView )
                ).getProdVersion( versionRecordManager.getPathToLatest() );
            } else {
                LPRDeletePopup deletePopup = new LPRDeletePopup( getDeleteCommand(), false );
                deletePopup.show();
            }
        }

        private RemoteCallback<Path> getProdVersionCallback() {
            return new RemoteCallback<Path>() {
                @Override
                public void callback( Path prodPath ) {
                    baseView.hideBusyIndicator();
                    boolean rollbackToProdVersion = prodPath != null;
                    Command action = rollbackToProdVersion ? getRestoreCommand( prodPath ) : getDeleteCommand();
                    LPRDeletePopup deletePopup = new LPRDeletePopup( action, rollbackToProdVersion );
                    deletePopup.show();
                }
            };
        }

        private Command getRestoreCommand( final Path prodPath ) {
            return new Command() {
                @Override
                public void execute() {
                    baseView.showBusyIndicator( CommonConstants.INSTANCE.Restoring() );
                    versionService.call( new RemoteCallback<Path>() {
                                             @Override
                                             public void callback( Path restored ) {
                                                 baseView.hideBusyIndicator();
                                                 versionRecordManager.reloadVersions( restored );
                                                 reload(); //reload to show the new restored version
                                             }
                                         },
                            new HasBusyIndicatorDefaultErrorCallback( baseView )
                    ).restore( prodPath, "Produktionsversion gendannet" );
                }
            };
        }

        private Command getDeleteCommand() {
            return new Command() {
                @Override
                public void execute() {
                    baseView.showBusyIndicator( CommonConstants.INSTANCE.Deleting() );
                    deleteService.call( new RemoteCallback<Void>() {
                                            @Override
                                            public void callback( final Void response ) {
                                                baseView.hideBusyIndicator();
                                                notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemDeletedSuccessfully() ) );
                                            }
                                        },
                            new HasBusyIndicatorDefaultErrorCallback( baseView )
                    ).delete( versionRecordManager.getPathToLatest(), "Regel slettet" );
                }
            };
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

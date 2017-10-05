package org.kie.workbench.common.widgets.metadata.client.lpr;

import java.util.Date;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.IsWidget;
import org.guvnor.common.services.shared.metadata.model.LprRuleType;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.gwtbootstrap3.client.ui.Button;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.container.IOC;
import org.kie.workbench.common.services.shared.preferences.ApplicationPreferences;
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
import org.uberfire.ext.editor.commons.client.resources.i18n.CommonConstants;
import org.uberfire.mvp.Command;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.MenuItem;

/**
 * Created on 31-08-2017.
 */
public abstract class LPREditor extends KieEditor {

    protected Command moveToProdCmdWaitingForValidation = null;
    private boolean lockedByOtherUser;

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


    private class MoveToProductionCommand implements Command {
        @Override
        public void execute() {
            //check rule has no validation errors before moving to production
            moveToProdCmdWaitingForValidation = getShowPopupCommand();
            onValidate().execute();
        }

        private Command getShowPopupCommand() {
            return new Command() {
                @Override
                public void execute() {
                    moveToProdCmdWaitingForValidation = null; //we have executed, remove ourselves from the waiting position
                    MoveToProductionPopup popup = new MoveToProductionPopup( getOKCommand() );
                    popup.show();
                }
            };
        }

        private Command getOKCommand() {
            return new Command() {
                @Override
                public void execute() {
                    //check state is as expected
                    String fileName = versionRecordManager.getCurrentPath().getFileName();
                    NotificationEvent errorEvent = null;
                    if ( lockedByOtherUser ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke produktionssættes. Reglen er låst af en anden bruger", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( concurrentUpdateSessionInfo != null ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke produktionssættes. Reglen blev ændret samtidigt andetsteds fra", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( isReadOnly ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke produktionssættes. Reglen er skrivebeskyttet", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( !versionRecordManager.isCurrentLatest() ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke produktionssættes i denne version. Kun nyeste version kan produktionssættes", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( metadata.getProductionDate() > 0 ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke produktionssættes. Reglen er allerede i produktion", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( metadata.getArchivedDate() > 0 ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke produktionssættes. Reglen er arkiveret", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( errorEvent != null ) {
                        notification.fire( errorEvent );
                        updateEnabledStateOnMenuItems();
                        return; //abort command
                    }

                    //set metadata and save rule
                    //TODO TTN: Revert all other changes before saving!
                    metadata.setProductionDate( new Date().getTime() );
                    final SaveOperationService.SaveOperationNotifier notifier = IOC.getBeanManager().lookupBean( SaveOperationService.SaveOperationNotifier.class ).getInstance();
                    baseView.showSaving();
                    save( "Produktionsættelse" );
                    concurrentUpdateSessionInfo = null;
                    notifier.notify( versionRecordManager.getCurrentPath() );
                }
            };
        }
    }

    private class ArchiveCommand implements Command {
        @Override
        public void execute() {
            ArchivePopup popup = new ArchivePopup( new Command() {
                @Override
                public void execute() {
                    //check state is as expected
                    String fileName = versionRecordManager.getCurrentPath().getFileName();
                    NotificationEvent errorEvent = null;
                    if ( lockedByOtherUser ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke arkiveres. Reglen er låst af en anden bruger", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( concurrentUpdateSessionInfo != null ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke arkiveres. Reglen blev ændret samtidigt andetsteds fra", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( isReadOnly ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke arkiveres. Reglen er skrivebeskyttet", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( !versionRecordManager.isCurrentLatest() ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke arkiveres i denne version. Kun nyeste version kan arkiveres", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( metadata.getProductionDate() == 0 ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke arkiveres. Reglen er ikke i produktion", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( metadata.getArchivedDate() > 0 ) {
                        errorEvent = new NotificationEvent( fileName + " kan ikke arkiveres. Reglen er allerede arkiveret", NotificationEvent.NotificationType.ERROR );
                    }
                    if ( errorEvent != null ) {
                        notification.fire( errorEvent );
                        updateEnabledStateOnMenuItems();
                        return; //abort command
                    }

                    //set metadata and save rule
                    //TODO TTN: Revert all other changes before saving!
                    metadata.setArchivedDate( new Date().getTime() );
                    final SaveOperationService.SaveOperationNotifier notifier = IOC.getBeanManager().lookupBean( SaveOperationService.SaveOperationNotifier.class ).getInstance();
                    baseView.showSaving();
                    save( "Arkivering" );
                    concurrentUpdateSessionInfo = null;
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

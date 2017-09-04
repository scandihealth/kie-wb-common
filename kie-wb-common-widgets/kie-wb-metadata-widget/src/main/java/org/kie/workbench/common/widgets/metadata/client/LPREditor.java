package org.kie.workbench.common.widgets.metadata.client;

import java.util.Date;

import org.uberfire.ext.editor.commons.client.file.ArchivePopup;
import org.uberfire.ext.editor.commons.client.file.MoveToProductionPopup;
import org.uberfire.mvp.Command;
import org.uberfire.workbench.events.NotificationEvent;

/**
 * Created on 31-08-2017.
 */
public abstract class LPREditor extends KieEditor {

    public LPREditor( KieEditorView baseView ) {
        super( baseView );
    }

    @Override
    protected void makeMenuBar() {
        super.makeMenuBar(); //build standard menus
        menus = menuBuilder
                .addMoveToProduction( new MoveToProductionCommand() )
                .addArchive( new ArchiveCommand() )
                .addSimulate( new SimulateCommand() )
                .build(); //build LPR menus

    }


    private class MoveToProductionCommand implements Command {
        @Override
        public void execute() {
            MoveToProductionPopup popup = new MoveToProductionPopup( new Command() {
                @Override
                public void execute() {
                    //the popup will have hidden itself
                    String fileName = versionRecordManager.getCurrentPath().getFileName();
                    //check metadata state (just to be safe)
                    if ( metadata.getProductionDate() > 0 ) {
                        updateEnabledStateOnMenuItems();
                        notification.fire( new NotificationEvent( fileName + " cannot be moved to production. It is already in production", NotificationEvent.NotificationType.ERROR ) );
                        return; //abort command
                    }
                    if ( metadata.getArchivedDate() > 0 ) {
                        updateEnabledStateOnMenuItems();
                        notification.fire( new NotificationEvent( fileName + " cannot be moved to production. It is already archived", NotificationEvent.NotificationType.ERROR ) );
                        return; //abort command
                    }
                    metadata.setProductionDate( new Date().getTime() );
                    //save rule with updated metadata
                    //(creates new popup, handles concurrency issues, handles busyIndicatorView, performs auditlogging, and performs success/error handling)
                    onSave();
                }
            } );
            popup.show();
        }
    }

    private class ArchiveCommand implements Command {
        @Override
        public void execute() {
            ArchivePopup popup = new ArchivePopup( new Command() {
                @Override
                public void execute() {
                    //the popup will have hidden itself
                    String fileName = versionRecordManager.getCurrentPath().getFileName();
                    //check metadata state (just to be safe)
                    if ( metadata.getProductionDate() == 0L ) {
                        notification.fire( new NotificationEvent( fileName + " cannot be archived. It is not in production", NotificationEvent.NotificationType.ERROR ) );
                        updateEnabledStateOnMenuItems();
                        return; //abort command
                    }
                    if ( metadata.getArchivedDate() > 0 ) {
                        notification.fire( new NotificationEvent( fileName + " cannot be archived. It is already archived", NotificationEvent.NotificationType.ERROR ) );
                        updateEnabledStateOnMenuItems();
                        return; //abort command
                    }
                    metadata.setArchivedDate( new Date().getTime() );
                    //save rule with updated metadata
                    //(creates new popup, handles concurrency issues, handles busyIndicatorView, performs auditlogging, and performs success/error handling)
                    onSave();
                }
            } );
            popup.show();
        }
    }

    private class SimulateCommand implements Command {
        @Override
        public void execute() {
            notification.fire( new NotificationEvent( "Not implemented!",
                    NotificationEvent.NotificationType.ERROR ) );
        }
    }

}

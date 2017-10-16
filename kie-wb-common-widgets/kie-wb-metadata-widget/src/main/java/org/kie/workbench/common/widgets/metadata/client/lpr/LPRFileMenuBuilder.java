package org.kie.workbench.common.widgets.metadata.client.lpr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.client.file.CommandWithFileNameAndCommitMessage;
import org.uberfire.ext.editor.commons.client.file.CopyPopup;
import org.uberfire.ext.editor.commons.client.file.CopyPopupView;
import org.uberfire.ext.editor.commons.client.file.DeletePopup;
import org.uberfire.ext.editor.commons.client.file.FileNameAndCommitMessage;
import org.uberfire.ext.editor.commons.client.file.RenamePopup;
import org.uberfire.ext.editor.commons.client.file.RenamePopupView;
import org.uberfire.ext.editor.commons.client.menu.MenuItems;
import org.uberfire.ext.editor.commons.client.validation.Validator;
import org.uberfire.ext.editor.commons.service.CopyService;
import org.uberfire.ext.editor.commons.service.DeleteService;
import org.uberfire.ext.editor.commons.service.RenameService;
import org.uberfire.ext.editor.commons.service.support.SupportsCopy;
import org.uberfire.ext.editor.commons.service.support.SupportsRename;
import org.uberfire.ext.widgets.common.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.uberfire.ext.widgets.common.client.common.BusyIndicatorView;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.ParameterizedCommand;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.MenuVisitor;
import org.uberfire.workbench.model.menu.Menus;

/**
 * Created on 05-09-2017.
 */
@Dependent
public class LPRFileMenuBuilder {

    private MenuItem saveMenuItem = null;
    private Command deleteCommand = null;
    private Command renameCommand = null;
    private Command copyCommand = null;
    private Command validateCommand = null;
    private MenuItem versionMenuItem = null;
    private Command moveToProductionCommand = null;
    private Command archiveCommand = null;
    private Command simulateCommand = null;
    private List<MenuItem> menuItemsSyncedWithLockState = new ArrayList<MenuItem>();

    @Inject
    private Caller<DeleteService> deleteService;
    @Inject
    private Caller<RenameService> renameService;
    @Inject
    private Caller<CopyService> copyService;
    @Inject
    private Event<NotificationEvent> notification;
    @Inject
    private BusyIndicatorView busyIndicatorView;


    public List<MenuItem> getMenuItemsSyncedWithLockState() {
        return menuItemsSyncedWithLockState;
    }

    public LPRFileMenuBuilder addSave( final MenuItem menuItem ) {
        saveMenuItem = menuItem;
        return this;
    }

    public LPRFileMenuBuilder addCopy( final Path path, final Validator validator ) {
        copyCommand = new Command() {
            @Override
            public void execute() {
                final CopyPopupView copyPopupView = CopyPopup.getDefaultView();
                final CopyPopup popup = new CopyPopup(
                        path,
                        validator,
                        getCopyPopupCommand( copyService, path, copyPopupView ), copyPopupView );
                popup.show();
            }
        };

        return this;
    }

    public LPRFileMenuBuilder addRename( final Path path, final Validator validator ) {
        renameCommand = new Command() {
            @Override
            public void execute() {
                final RenamePopupView renamePopupView = RenamePopup.getDefaultView();
                final RenamePopup popup = new RenamePopup( path,
                        validator,
                        getRenamePopupCommand( renameService, path, renamePopupView ), renamePopupView );

                popup.show();
            }
        };
        return this;
    }

    public LPRFileMenuBuilder addDelete( final Path path ) {
        deleteCommand = new Command() {
            @Override
            public void execute() {
                final DeletePopup popup = new DeletePopup( new ParameterizedCommand<String>() {
                    @Override
                    public void execute( final String comment ) {
                        busyIndicatorView.showBusyIndicator( CommonConstants.INSTANCE.Deleting() );
                        deleteService.call( new RemoteCallback<Void>() {
                                                @Override
                                                public void callback( final Void response ) {
                                                    busyIndicatorView.hideBusyIndicator();
                                                    notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemDeletedSuccessfully() ) );
                                                }
                                            },
                                new HasBusyIndicatorDefaultErrorCallback( busyIndicatorView ) ).delete( path, comment );
                    }
                } );
                popup.show();
            }
        };
        return this;
    }

    public LPRFileMenuBuilder addMoveToProduction( Command command ) {
        moveToProductionCommand = command;
        return this;
    }

    public LPRFileMenuBuilder addArchive( Command command ) {
        archiveCommand = command;
        return this;
    }

    public LPRFileMenuBuilder addSimulate( Command command ) {
        simulateCommand = command;
        return this;
    }

    public LPRFileMenuBuilder addValidate( final Command command ) {
        validateCommand = command;
        return this;
    }

    public LPRFileMenuBuilder addVersionMenu( final MenuItem menu ) {
        versionMenuItem = menu;
        return this;
    }

    public Menus build() {
        final Map<Object, MenuItem> menuItems = new LinkedHashMap<Object, MenuItem>();
        if ( saveMenuItem != null ) {
            menuItems.put( MenuItems.SAVE, saveMenuItem );
            menuItemsSyncedWithLockState.add( saveMenuItem );
        }
        if ( deleteCommand != null ) {
            MenuItem deleteMenuItem = MenuFactory.newTopLevelMenu( CommonConstants.INSTANCE.Delete() )
                    .respondsWith( deleteCommand )
                    .endMenu()
                    .build().getItems().get( 0 );
            menuItems.put( MenuItems.DELETE, deleteMenuItem );
            menuItemsSyncedWithLockState.add( deleteMenuItem );
        }
        if ( renameCommand != null ) {
            MenuItem renameMenuItem = MenuFactory.newTopLevelMenu( CommonConstants.INSTANCE.Rename() )
                    .respondsWith( renameCommand )
                    .endMenu()
                    .build().getItems().get( 0 );
            menuItems.put( MenuItems.RENAME, renameMenuItem );
            menuItemsSyncedWithLockState.add( renameMenuItem );
        }

        if ( copyCommand != null ) {
            menuItems.put( MenuItems.COPY, MenuFactory.newTopLevelMenu( CommonConstants.INSTANCE.Copy() )
                    .respondsWith( copyCommand )
                    .endMenu()
                    .build().getItems().get( 0 ) );
        }
        if ( moveToProductionCommand != null ) {
            MenuItem moveToProductionMenuItem = MenuFactory.newTopLevelMenu( CommonConstants.INSTANCE.LPRMoveToProduction() )
                    .respondsWith( moveToProductionCommand )
                    .endMenu()
                    .build().getItems().get( 0 );

            menuItems.put( MenuItems.MOVETOPRODUCTION, moveToProductionMenuItem );
            menuItemsSyncedWithLockState.add( moveToProductionMenuItem );
        }

        if ( archiveCommand != null ) {
            MenuItem archiveMenuItem = MenuFactory.newTopLevelMenu( CommonConstants.INSTANCE.LPRArchive() )
                    .respondsWith( archiveCommand )
                    .endMenu()
                    .build().getItems().get( 0 );
            menuItems.put( MenuItems.ARCHIVE, archiveMenuItem );
            menuItemsSyncedWithLockState.add( archiveMenuItem );
        }

        if ( simulateCommand != null ) {
            MenuItem simulateMenuItem = MenuFactory.newTopLevelMenu( CommonConstants.INSTANCE.LPRSimulate() )
                    .respondsWith( simulateCommand )
                    .endMenu()
                    .build().getItems().get( 0 );

            menuItems.put( MenuItems.SIMULATE, simulateMenuItem );
        }
        if ( validateCommand != null ) {
            menuItems.put( MenuItems.VALIDATE, MenuFactory.newTopLevelMenu( CommonConstants.INSTANCE.Validate() )
                    .respondsWith( validateCommand )
                    .endMenu()
                    .build().getItems().get( 0 ) );
        }
        if ( versionMenuItem != null ) {
            menuItems.put( MenuItems.HISTORY, versionMenuItem );
        }

        return new Menus() {

            @Override
            public List<MenuItem> getItems() {
                return new ArrayList<MenuItem>() {{
                    for ( final MenuItem menuItem : menuItems.values() ) {
                        add( menuItem );
                    }
                }};
            }

            @Override
            public Map<Object, MenuItem> getItemsMap() {
                return menuItems;
            }

            @Override
            public void accept( MenuVisitor visitor ) {
                if ( visitor.visitEnter( this ) ) {
                    for ( final MenuItem item : menuItems.values() ) {
                        item.accept( visitor );
                    }
                    visitor.visitLeave( this );
                }
            }

            @Override
            public int getOrder() {
                return 0;
            }
        };
    }

    private CommandWithFileNameAndCommitMessage getCopyPopupCommand( final Caller<? extends SupportsCopy> copyCaller,
                                                                     final Path path,
                                                                     final CopyPopupView copyPopupView ) {
        return new CommandWithFileNameAndCommitMessage() {
            @Override
            public void execute( final FileNameAndCommitMessage details ) {
                busyIndicatorView.showBusyIndicator( CommonConstants.INSTANCE.Copying() );
                copyCaller.call( getCopySuccessCallback( copyPopupView ),
                        getCopyErrorCallback( copyPopupView, busyIndicatorView ) ).copy( path,
                        details.getNewFileName(),
                        details.getCommitMessage() );
            }
        };
    }

    private RemoteCallback<Path> getCopySuccessCallback( final CopyPopupView copyPopupView ) {
        return new RemoteCallback<Path>() {

            @Override
            public void callback( final Path path ) {
                copyPopupView.hide();
                busyIndicatorView.hideBusyIndicator();
                notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemCopiedSuccessfully() ) );
            }
        };
    }

    private HasBusyIndicatorDefaultErrorCallback getCopyErrorCallback( final CopyPopupView copyPopupView,
                                                                       BusyIndicatorView busyIndicatorView ) {
        return new HasBusyIndicatorDefaultErrorCallback( busyIndicatorView ) {

            @Override
            public boolean error( final Message message,
                                  final Throwable throwable ) {
                if ( fileAlreadyExists( throwable ) ) {
                    hideBusyIndicator();
                    copyPopupView.handleDuplicatedFileName();
                    return false;
                }

                copyPopupView.hide();
                return super.error( message, throwable );
            }
        };
    }

    private CommandWithFileNameAndCommitMessage getRenamePopupCommand( final Caller<? extends SupportsRename> renameCaller,
                                                                       final Path path,
                                                                       final RenamePopupView renamePopupView ) {
        return new CommandWithFileNameAndCommitMessage() {
            @Override
            public void execute( final FileNameAndCommitMessage details ) {
                busyIndicatorView.showBusyIndicator( CommonConstants.INSTANCE.Renaming() );
                renameCaller.call( getRenameSuccessCallback( renamePopupView ),
                        getRenameErrorCallback( renamePopupView, busyIndicatorView ) ).rename( path,
                        details.getNewFileName(),
                        details.getCommitMessage() );
            }
        };
    }

    private RemoteCallback<Path> getRenameSuccessCallback( final RenamePopupView renamePopupView ) {
        return new RemoteCallback<Path>() {

            @Override
            public void callback( final Path path ) {
                renamePopupView.hide();
                busyIndicatorView.hideBusyIndicator();
                notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemRenamedSuccessfully() ) );
            }
        };
    }

    private HasBusyIndicatorDefaultErrorCallback getRenameErrorCallback( final RenamePopupView renamePopupView,
                                                                         BusyIndicatorView busyIndicatorView ) {
        return new HasBusyIndicatorDefaultErrorCallback( busyIndicatorView ) {

            @Override
            public boolean error( final Message message,
                                  final Throwable throwable ) {
                if ( fileAlreadyExists( throwable ) ) {
                    hideBusyIndicator();
                    renamePopupView.handleDuplicatedFileName();
                    return false;
                }

                renamePopupView.hide();
                return super.error( message, throwable );
            }
        };
    }


    private boolean fileAlreadyExists( final Throwable throwable ) {
        return throwable != null && throwable.getMessage() != null && throwable.getMessage().contains( "FileAlreadyExistsException" );
    }
}

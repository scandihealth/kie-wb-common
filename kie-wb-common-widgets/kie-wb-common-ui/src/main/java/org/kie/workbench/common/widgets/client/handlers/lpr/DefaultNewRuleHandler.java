/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.widgets.client.handlers.lpr;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import org.kie.workbench.common.widgets.client.handlers.DefaultNewResourceHandler;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.uberfire.client.mvp.PerspectiveActivity;
import org.uberfire.client.mvp.PerspectiveManager;
import org.uberfire.commons.data.Pair;

/**
 * Specialized new resource handler used when creating new rules in LPR
 */
public abstract class DefaultNewRuleHandler extends DefaultNewResourceHandler implements NewRuleHandler {

    @Inject
    private PerspectiveManager perspectiveManager;

    @Override
    public List<Pair<String, ? extends IsWidget>> getExtensions() {
        List<Pair<String, ? extends IsWidget>> extensions = super.getExtensions();
        PerspectiveActivity currentPerspective = perspectiveManager.getCurrentPerspective();
        if ( currentPerspective != null && "LPRPerspective".equals( currentPerspective.getIdentifier() ) ) {
            //we do not want to show the packages in LPR
            extensions = new ArrayList<Pair<String, ? extends IsWidget>>( extensions ); //create new copy of list to not affect other perspectives
            extensions.remove( Pair.newPair( CommonConstants.INSTANCE.ItemPathSubheading(), packagesListBox ) ); //remove packages list box
        }
        return extensions;
    }
}
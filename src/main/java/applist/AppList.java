package applist;

/*-
 * #%L
 * FOK Launcher
 * %%
 * Copyright (C) 2016 Frederik Kammel
 * %%
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
 * #L%
 */


import java.util.ArrayList;
import java.util.Collection;

/**
 * A list of apps. Based on a {@link ArrayList} but contains additional methods
 * specific for apps.
 *
 * @author Frederik Kammel
 */
public class AppList extends ArrayList<App> {

    /**
     * Creates a new app list
     */
    public AppList() {
        super();
    }

    /**
     * Creates a new app list and provisions enough memory to store the specified capacity
     *
     * @param capacity The capacity to provision
     */
    public AppList(int capacity) {
        super(capacity);
    }

    /**
     * Creates a new app list from the specified collection
     *
     * @param collection The collection whose elements are to be placed into this list
     */
    public AppList(Collection<? extends App> collection) {
        super(collection);
    }

    /**
     * Clears the version cache of all apps in this list
     */
    public void clearVersionCache() {
        for (App app : this) {
            app.clearCache();
        }
    }

    /**
     * Tells the context menu off all apps to reload its version lists
     *
     * @deprecated Use {@link #clearVersionCache()} instead
     */
    @Deprecated
    public void reloadContextMenuEntriesOnShow() {
        clearVersionCache();
    }

    /**
     * Searches this {@link AppList} for the specified app.
     *
     * @param mvnCoordinates The coordinates of the app to find
     * @return The first matching {@link App} or {@code null} if no app matches.
     */
    public App getAppByMavenCoordinates(MVNCoordinates mvnCoordinates) {
        for (App app : this) {
            if (app.getMvnCoordinates().equals(mvnCoordinates)) {
                return app;
            }
        }

        // We only arrived here if no match was found
        return null;
    }

    /**
     * Adds the specified app only if the app has not been added before already.
     *
     * @param appToAdd The app to add.
     * @return {@code true} if the app has been added successfully, {@code false} otherwise
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean addAndCheckForDuplicateImports(App appToAdd) {
        for (App app : this) {
            if (app.getImportFile().equals(appToAdd.getImportFile())) {
                return false;
            }
        }

        // no duplicate found
        this.add(appToAdd);
        return true;
    }
}

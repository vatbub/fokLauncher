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

    private static final long serialVersionUID = -1460089544427389007L;

    public AppList() {
        super();
    }

    @SuppressWarnings("unused")
    public AppList(int arg0) {
        super(arg0);
    }

    @SuppressWarnings("unused")
    public AppList(Collection<? extends App> arg0) {
        super(arg0);
    }

    /**
     * Clears the version cache of all apps in this list
     */
    @SuppressWarnings("unused")
    public void clearVersionCache() {
        for (App app : this) {
            app.latestOnlineSnapshotVersion = null;
            app.latestOnlineVersion = null;
            app.onlineVersionList = null;
        }

        reloadContextMenuEntriesOnShow();
    }

    /**
     * Tells the context menu off all apps to reload its version lists
     */
    public void reloadContextMenuEntriesOnShow() {
        for (App app : this) {
            app.setDeletableVersionListLoaded(false);
            app.setSpecificVersionListLoaded(false);
        }
    }

    /**
     * Searches this {@link AppList} for the specified app.
     *
     * @param mavenGroupId    The maven groupId of the app to find
     * @param mavenArtifactId The maven artifactId of the app to find
     * @return The first matching {@link App} or {@code null} if no app matches.
     */
    @SuppressWarnings("unused")
    public App getAppByMavenCoordinates(String mavenGroupId, String mavenArtifactId) {
        return getAppByMavenCoordinates(mavenGroupId, mavenArtifactId, null);
    }

    /**
     * Searches this {@link AppList} for the specified app.
     *
     * @param mavenGroupId    The maven groupId of the app to find
     * @param mavenArtifactId The maven artifactId of the app to find
     * @param mavenClassifier The maven artifactId of the app to find
     * @return The first matching {@link App} or {@code null} if no app matches.
     */
    @SuppressWarnings("unused")
    public App getAppByMavenCoordinates(String mavenGroupId, String mavenArtifactId, String mavenClassifier) {
        for (App app : this) {
            if (app.getMvnCoordinates().getGroupId().equals(mavenGroupId) && app.getMvnCoordinates().getArtifactId().equals(mavenArtifactId)) {
                // ArtifactId and groupId match, check if classifier matches too
                // if required
                if (mavenClassifier != null) {
                    // We need to check the classifier too
                    if (app.getMvnCoordinates().getClassifier().equals(mavenClassifier)) {
                        // Everything matches
                        return app;
                    }
                } else {
                    // Only artifactId and groupId are required to match
                    return app;
                }
            }
        }

        // We only arrived here if no match was found
        return null;
    }
}

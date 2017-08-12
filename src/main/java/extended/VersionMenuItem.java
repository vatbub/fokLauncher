package extended;

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


import com.github.vatbub.common.updater.Version;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;

public class VersionMenuItem extends MenuItem {

    private Version version;

    public VersionMenuItem() {
        super();
    }

    @SuppressWarnings("unused")
    public VersionMenuItem(String arg0) {
        super(arg0);
    }

    @SuppressWarnings("unused")
    public VersionMenuItem(String arg0, Node arg1) {
        super(arg0, arg1);
    }

    /**
     * @return the version
     */
    public Version getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(Version version) {
        this.version = version;
    }

}

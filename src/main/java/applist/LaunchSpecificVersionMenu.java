package applist;

/*-
 * #%L
 * FOK Launcher
 * %%
 * Copyright (C) 2016 - 2017 Frederik Kammel
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


import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class LaunchSpecificVersionMenu extends Menu {
    private boolean cancelled;

    /**
     * Constructs a Menu with an empty string for its display text.
     *
     * @since JavaFX 2.2
     */
    public LaunchSpecificVersionMenu() {
    }

    /**
     * Constructs a Menu and sets the display text with the specified text.
     *
     * @param text the text to display on the menu button
     */
    public LaunchSpecificVersionMenu(String text) {
        super(text);
    }

    /**
     * Constructs a Menu and sets the display text with the specified text
     * and sets the graphic {@link Node} to the given node.
     *
     * @param text    the text to display on the menu button
     * @param graphic the graphic to display on the menu button
     */
    public LaunchSpecificVersionMenu(String text, Node graphic) {
        super(text, graphic);
    }

    /**
     * Constructs a Menu and sets the display text with the specified text,
     * the graphic {@link Node} to the given node, and inserts the given items
     * into the {@link #getItems() items} list.
     *
     * @param text    the text to display on the menu button
     * @param graphic the graphic to display on the menu button
     * @param items   The items to display in the popup menu.
     * @since JavaFX 8u40
     */
    public LaunchSpecificVersionMenu(String text, Node graphic, MenuItem... items) {
        super(text, graphic, items);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

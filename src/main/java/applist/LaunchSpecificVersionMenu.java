package applist;

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

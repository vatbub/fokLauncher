package extended;

import javafx.scene.Node;
import javafx.scene.control.ListCell;

public class CustomListCell<T> extends ListCell<T> {
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
 
        if (empty || item==null) {
            setText(null);
            setGraphic(null);
        } else if (item instanceof Node) {
            setText(null);
            Node currentNode = getGraphic();
            Node newNode = (Node) item;
            if (currentNode == null || ! currentNode.equals(newNode)) {
                setGraphic(newNode);
            }
        } else {
            setText(item.toString());
            setGraphic(null);
        }
    }
}

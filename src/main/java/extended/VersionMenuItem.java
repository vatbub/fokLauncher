package extended;

import common.Version;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;

public class VersionMenuItem extends MenuItem {
	
	private Version version;

	public VersionMenuItem() {
		super();
	}

	public VersionMenuItem(String arg0) {
		super(arg0);
	}

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

package applist;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list of apps. Based on a {@link ArrayList} but contains additional methods
 * specific for apps.
 * 
 * @author Frederik Kammel
 *
 */
public class AppList extends ArrayList<App> {

	private static final long serialVersionUID = -1460089544427389007L;

	public AppList() {
		super();
	}

	public AppList(int arg0) {
		super(arg0);
	}

	public AppList(Collection<? extends App> arg0) {
		super(arg0);
	}

	/**
	 * Clears the version cache of all apps in this list
	 */
	public void clearVersionCache() {
		for(App app:this){
			app.latestOnlineSnapshotVersion = null;
			app.latestOnlineVersion = null;
			app.onlineVersionList = null;
		}
		
		reloadContextMenuEntriesOnShow();
	}
	
	/**
	 * Tells the context menu off all apps to reload its version lists
	 */
	public void reloadContextMenuEntriesOnShow(){
		for(App app:this){
			app.setDeletableVersionListLoaded(false);
			app.setSpecificVersionListLoaded(false);
		}
	}
}

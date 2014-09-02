package tfsAccess;

import java.util.ArrayList;

public class ChangeSet {
	@SuppressWarnings("unused")
	private Integer ID; // changeSet id
	private ArrayList <String> changeSetFiles = new ArrayList <String> ();
	void setID (Integer id) {
		ID = id;
	}
	void addFileName (String filename) {
		changeSetFiles.add(filename);
	}
	public ArrayList <String> getChangeSetFileArray() {
		return this.changeSetFiles;
	}
}

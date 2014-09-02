package tfsAccess;
import java.util.Date;
import java.util.HashMap;


public class WorkItemFilesRecord {
	private Date createdDate;
	private Integer workItemId;
	private HashMap<Integer, ChangeSet> csMap = new HashMap<Integer, ChangeSet>(); //Work item can contain several change sets - key is CS id
	private String workItemType;
	private String workItemAgreedRelease;
	private String workItemTeamLeader;

	public void setWorkItemId(Integer wid) {
		workItemId = wid;
	}
	public Integer getWorkItemId() {
		return this.workItemId;
	}
	public HashMap<Integer, ChangeSet> getcsMap() {
		return this.csMap;
	}
	public void setRecordDate (Date date){
		createdDate = date;
	}
	public Date getRecordDate() {
		return this.createdDate;
	}
	public void addRecordFile (Integer id, String file){
		ChangeSet changeset;
		if (!csMap.containsKey(id)){
			changeset = new ChangeSet();
			changeset.setID(id);
		}
		else changeset = csMap.get(id);
		changeset.addFileName(file);
		csMap.put(id, changeset);
	}
	
    public HashMap <Integer, ChangeSet> getRecordFiles() {
    	return this.csMap;
    }
	public void setWorkItemType(String type) {
		workItemType = new String(type);
	}
	public String getWorkItemType() {
		return this.workItemType;
	}
	public void setWorkItemAgreedRelease (String agreedRelease) {
		workItemAgreedRelease = agreedRelease; 
	}
	public String getWorkItemAgreedRelease () {
		return this.workItemAgreedRelease;
	}
	public void setWorkItemTeamLeader (String teamLeader) {
		workItemTeamLeader = teamLeader; 
	}
	public String getWorkItemTeamLeader () {
		return this.workItemTeamLeader;
	}

}

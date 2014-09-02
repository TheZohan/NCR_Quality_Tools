package changeSetMap;
import java.util.Date;
import java.util.HashMap;

public class WorkItemChangeSetRecord {
	class ChangeSetRecord {
		Integer ID; // changeSet id
		private Date createdDate;
		private Integer workItemId;
		private String workItemType = new String();
		HashMap <Integer, String> changeSetFiles = new HashMap <Integer, String> ();
		void setID (Integer id) {
			ID = id;
		}
		public void addFileToMap (Integer id, String file){
			changeSetFiles.put(id, file);
		}
		public HashMap <Integer, String> getChangeSetFileMap() {
			return this.changeSetFiles;
		}
		public void setCsdDate (Date date){
			createdDate = date;
		}
		public Date getChangeSetDate() {
			return this.createdDate;
		}
		public void setWorkItemId(Integer wid) {
			workItemId = wid;
		}
		public Integer getWorkItemId() {
			return this.workItemId;
		}
		public void setWorkItemType (String type){
			workItemType = type;
		}
		public String getWorkItemType () {
			return this.workItemType;
		}
	}
	
	private HashMap<Integer, ChangeSetRecord> csMap = new HashMap<Integer, ChangeSetRecord>(); //Work item can contain several change sets - key is CS id
    private Integer workItemId;
    private String workItemType;
	public HashMap<Integer, ChangeSetRecord> getcsMap() {
		return this.csMap;
	}
	public void addChangeSetToRecord (Integer csId, Date date, Integer fileID, String file){
		ChangeSetRecord changeset;
		if (!csMap.containsKey(csId)){
			changeset = new ChangeSetRecord();
			changeset.setID(csId);
			changeset.setCsdDate(date);
			changeset.setWorkItemType(workItemType);
			changeset.setWorkItemId(workItemId);
		}
		else changeset = csMap.get(csId);
		
		changeset.addFileToMap(fileID, file);
		csMap.put(csId, changeset);
	}
	
    public HashMap <Integer, ChangeSetRecord> getChangeSetRecord() {
    	return this.csMap;
    }
	public void setWorkItemId(Integer wid) {
		workItemId = wid;
	}
	public Integer getWorkItemId() {
		return this.workItemId;
	}
	public void setWorkItemType(String type) {
		workItemType = type;
	}
	public String getWorkItemType() {
		return this.workItemType;
	}
}

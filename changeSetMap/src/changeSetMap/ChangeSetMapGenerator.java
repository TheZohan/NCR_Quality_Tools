package changeSetMap;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

//import reopenReport.TFSException;

import changeSetMap.WorkItemChangeSetRecord.ChangeSetRecord;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;


public class ChangeSetMapGenerator {
	File queryFile, output_file;	
	FileIO fileIO;
	private WorkItemClient workItemClient;
	private VersionControlClient versionControlClient;
	WorkItemCollection workItemCollection=null;
    private HashMap <Integer, ChangeSetRecord> changeSetMap = new HashMap <Integer, ChangeSetRecord>(); //key is changeSet id
   
	ChangeSetMapApp appWinRef;
	 int querySize;
	 int bugCount = 0;
	 int workItemssWithChangesetsCount = 0;
		
	public ChangeSetMapGenerator (File query, File output, ChangeSetMapApp appWin) {
		   appWinRef = appWin;
		   queryFile =  query;
		   output_file = output;
		   fileIO = new FileIO(output_file);
	}

	public void runReports () throws IOException, TFSException {
		       fileIO.writeLinenToFile("transaction_id,item_id,transaction_date,item_category,item_name,item_volume,item_price, workItem_id");
			   appWinRef.progressCallBack("CONNECTING TO TFS....");
			   TFSAccess tmp=null;
			   tmp = new TFSAccess(queryFile, appWinRef);
			   workItemCollection = tmp.getBugCollection();
			   workItemClient = tmp.getWorkItemClient();
			   versionControlClient = tmp.getVersionControlClient();
			   appWinRef.progressCallBack("PROCESSING WORK ITEM DATA....");

			   processWorkItems();
			   writeOutputToCSV();
			   
		       appWinRef.progressCallBack("DONE: Processed "+querySize+"/"+querySize+" work items");
		       fileIO.closeFile();

		       Object msg[] = {"SUMMARY:\nProcessed a total of:"+querySize+" work items\n",
                       "of which:" +workItemssWithChangesetsCount+" had associated changesets"};
 		       JOptionPane.showMessageDialog(null, msg);
 		       cleanup();
		}
	
	private void processWorkItems() {
		 querySize = workItemCollection.size();
		 Integer wid;
		 String state, typeName;
		 WorkItem workItem;
		 workItemssWithChangesetsCount = 0;
		 ChangeSetClient csClient = new ChangeSetClient();
		 
		 csClient.setWorkItemClient(workItemClient);
		 csClient.setVersionControlClient(versionControlClient);
		 
		 for (int index = 0; index < querySize; index++) {	
			 csClient.resetClientFields();
			 workItem = workItemCollection.getWorkItem(index);
			 if (workItem == null) continue;

			 wid = (Integer) workItem.getFields().getField("ID").getOriginalValue(); 
			 
			 state = (String) workItem.getFields().getField("State").getOriginalValue();
			 typeName = (String) workItem.getFields().getField("Work Item Type").getOriginalValue();
			 
			 if (stateAndTypeCriterion(typeName, state) == false) continue;
			 
			 workItem = workItemClient.getWorkItemByID(wid);
			 csClient.setWorkItemID(wid.intValue());
			 
			 if (csClient.hasChangeset (workItem) == true){			
				workItemssWithChangesetsCount++;             
				WorkItemChangeSetRecord record = new WorkItemChangeSetRecord(); 
                record.setWorkItemId(wid); 
                record.setWorkItemType(typeName);
				csClient.addWorkItemChangeSetsToRecord(record);  //record contains all changesets associated to current work item, including related links
																 // they are in the csMap data structure of the record
				
				Iterator <Entry<Integer, ChangeSetRecord>> recordItr = record.getcsMap().entrySet().iterator();
				while (recordItr.hasNext()){		//iterate through csMap of record
					Entry <Integer, ChangeSetRecord> csElement = recordItr.next(); 
				    changeSetMap.put(csElement.getKey(), csElement.getValue()); 
				}
			}
	
			if (index != 0 && index % 5 == 0) appWinRef.progressCallBack("Processed "+index+"/"+querySize+" work items and identified "+workItemssWithChangesetsCount+" change sets");
		  }
	}
	  
	 boolean stateAndTypeCriterion(String typeName, String state) {
		 boolean retval = false;
		 if (!typeName.equalsIgnoreCase("User Story") && !typeName.equalsIgnoreCase("Task") &&  !typeName.equalsIgnoreCase("Bug")) retval = false;
		 else if ((typeName.equalsIgnoreCase("User Story") && !state.equalsIgnoreCase("Done") && !state.equalsIgnoreCase("Verification")) || (typeName.equalsIgnoreCase("Bug") && !state.equalsIgnoreCase("Fixed") && !state.equalsIgnoreCase("Pending QA") && !state.equalsIgnoreCase("Passed QA"))) retval = false;
		 
		 else retval = true;
		
		 return retval;
	 }
	
	 
	static String getChangesetID(String changeSetIdPath) {
			String tokens[] = changeSetIdPath.split("[/]");
			return tokens[tokens.length -1];
	}	 
		
	private void writeOutputToCSV () {
		 Date date;
		 ChangeSetRecord changeSetRecord;
		 HashMap <Integer, String> csFiles;
		 Integer changesetID;
		 Integer workItemID;
		 Integer fileID;
	     String path = new String();
	     String category = new String();
	     int itemCount = 0;

		 Iterator<Entry<Integer, ChangeSetRecord>> itr = changeSetMap.entrySet().iterator();
		 while (itr.hasNext()) {
				Entry<Integer, ChangeSetRecord> csElement = itr.next();
				changeSetRecord = csElement.getValue();
				changesetID = csElement.getKey();
			  	date = changeSetRecord.getChangeSetDate();
			  	csFiles = changeSetRecord.getChangeSetFileMap();
			  	category = changeSetRecord.getWorkItemType();
			  	workItemID = changeSetRecord.getWorkItemId();
			  	Iterator<Entry<Integer, String>> csFilesItr = csFiles.entrySet().iterator();
			  	while (csFilesItr.hasNext()){ 
			  			Entry<Integer, String> csFilesElement = csFilesItr.next();
			  			fileID = csFilesElement.getKey();
			  			path = csFilesElement.getValue();
			  			if (path == null) {
			  				path = " ";
			  			}
			  			addRowToCSV(changesetID, date, fileID, path, category, workItemID);
			  	}
			  	if (++itemCount % 5 == 0) appWinRef.progressCallBack("wrote "+itemCount+"records to file");
		}
    }
	
	private void addRowToCSV(Integer csID, Date date, Integer itemID, String path, String category, Integer workItemID) {
		String row = new String();
	    String DATE_FORMAT = "MM/dd/yyyy";
	    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		//row += csID+","+itemID+","+date.toString()+","+category+","+path+",1,0";
	    row += csID+","+itemID+","+sdf.format(date)+","+category+","+path+",1,0,"+workItemID;
		fileIO.writeLinenToFile(row);
	}

	private void cleanup() {
		changeSetMap.clear();
		changeSetMap = null;
		 queryFile = null;
		 output_file = null;	
		 workItemClient = null;
		 versionControlClient = null;
		 workItemCollection=null;
		 appWinRef = null;
	}

}

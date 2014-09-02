package mergeChangeSetReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.swing.JOptionPane;


import tfsAccess.TFSAccess;
import tfsAccess.TFSException;
import tfsAccess.TFSProgressCallBack;
import tfsAccess.WorkItemFilesRecord;

import tfsReports.TFSReportsGenerator;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;

import tfsAccess.ChangeSet;
import excelServices.ExcelServices;

public class MergeChangeSetReportGenerator implements TFSReportsGenerator{
	final int ID_COL = 0;
	final int AGREED_RELEASE_COL = 1;
	final int TEAM_LEADER_COL = 2;
	final int CS_ID_COL = 3;
	final int FILENAME_COL = 4;
	private File queryFile, output_file;	
	private WorkItemClient workItemClient;
	private VersionControlClient versionControlClient;
	private WorkItemCollection workItemCollection=null;
    private ArrayList <WorkItemFilesRecord> changeSetArray = new ArrayList <WorkItemFilesRecord>();   
    private TFSProgressCallBack callBack;
    private int querySize, processedItems, NABs, NRs, notFixed;
    private int workItemssWithChangesetsCount = 0;
    private int outputRow = 0;
	private ExcelServices excelAccess = new ExcelServices();

	public void ReportGenerator (File query, File coreBugsQuery, File userStoryQuery, File output, TFSProgressCallBack callback) {
		   callBack = callback;
		   queryFile =  query;
		   output_file = output;
	}

	public void runReports () throws IOException, TFSException {
	   	       excelAccess.openFiles(output_file);
	   	       excelAccess.createSheet("Merge changesets report", 0);
	   	       callBack.progressCallBack("CONNECTING TO TFS....");
			   TFSAccess tmp=null;
			   tmp = new TFSAccess(queryFile, null, null, callBack);

			   workItemCollection = tmp.getWorkItemCollection();
			   workItemClient = tmp.getWorkItemClient();
			   versionControlClient = tmp.getVersionControlClient();
			   callBack.progressCallBack("PROCESSING WORK ITEM DATA....");

			   processWorkItems();
		       addHeaderToFile();
		       addChangeSetListToFile();			   
		       callBack.progressCallBack("DONE: Processed "+querySize+"/"+querySize+" work items");
		       excelAccess.closeAll();

		       Object msg[] = {"SUMMARY:\nProcessed a total of:"+querySize+" work items\n",
                       "of which:" +workItemssWithChangesetsCount+" had associated changesets"};
 		       JOptionPane.showMessageDialog(null, msg);
 		       cleanup();
		}
	
	private void processWorkItems() {
		 querySize = workItemCollection.size();
		 Integer wid;
		 String state, typeName;
		 querySize = workItemCollection.size();
		 processedItems = querySize;
		 NABs = 0;
		 NRs = 0;
		 notFixed = 0;
	 	 String reason = new String();
	 	 String qaResolution = new String();
	 	 String agreedRelease = new String();
	 	 String teamLeader = new String();
		 WorkItem workItem;
		 workItemssWithChangesetsCount = 0;
		 MergeChangeSetClient csClient = new MergeChangeSetClient();
		 
		 csClient.setVersionControlClient(versionControlClient);
		 
		 for (int index = 0; index < querySize; index++) {	
			 workItem = workItemCollection.getWorkItem(index);
			 if (workItem == null) continue;

			 wid = (Integer) workItem.getFields().getField("ID").getOriginalValue(); 
			 
			 state = (String) workItem.getFields().getField("State").getOriginalValue();
			 typeName = (String) workItem.getFields().getField("Work Item Type").getOriginalValue();
			 reason = (String)  workItem.getFields().getField("Reason").getOriginalValue();
			 qaResolution = (String)  workItem.getFields().getField("QA Resolution").getOriginalValue();
			 agreedRelease = (String)  workItem.getFields().getField("Agreed Release").getOriginalValue().toString();
			 teamLeader = (String)  workItem.getFields().getField("Team Leader").getOriginalValue();
			 			 
			 if (!typeName.equalsIgnoreCase("Bug")){
				 processedItems--;
				 continue;
			 }
			 if (isNotABug (state, reason, qaResolution)){
				 NABs++;
				 processedItems--;
				 continue;
			 }
			 if (isNotReproduced(state, reason, qaResolution)){
				 NRs++;
				 processedItems--;
				 continue;
			 }

			 if (!isFixed (state)) {
				 processedItems--;
				 notFixed++;
				 continue; // filter out defects that are sure not to have change sets				 
			 }
			 
			 workItem = workItemClient.getWorkItemByID(wid);
			 csClient.resetExternalLinks();
			 
			 if (csClient.hasChangeset (workItem) == true){			
				workItemssWithChangesetsCount++;
                WorkItemFilesRecord record = new WorkItemFilesRecord(); 
                record.setWorkItemId(wid); 
                record.setWorkItemType(typeName);
                record.setWorkItemAgreedRelease(agreedRelease);
                record.setWorkItemTeamLeader(teamLeader);
                csClient.addWorkItemChangeSetsToRecord(record);
                changeSetArray.add(record);
			}
	
			if (index != 0 && index % 5 == 0) callBack.progressCallBack("Processed "+index+"/"+querySize+" work items and identified "+workItemssWithChangesetsCount+" change sets");
		  }
	}
	  
	   protected final boolean isNotABug(String state, String reason, String qaResolution) {
	    	boolean retval = false;
	    	if (state == null) state = "NA";
	    	if (reason == null) reason = "NA";
	    	if (qaResolution == null) qaResolution = "NA";
	    	if ((state.equalsIgnoreCase("Closed") && reason.equalsIgnoreCase("Not a Bug")) || (state.equalsIgnoreCase("Not a Bug"))) retval = true;
	    	return retval;
	    }
	    
	    protected final boolean isNotReproduced(String state, String reason, String qaResolution) {
	    	boolean retval = false;
	    	if (state == null) state = "NA";
	    	if (reason == null) reason = "NA";
	    	if (qaResolution == null) qaResolution = "NA";
	    	
	    	if ((state.equalsIgnoreCase("Closed") && reason.equalsIgnoreCase("Not Reproduced")) || (state.equalsIgnoreCase("Not Reproduced"))) retval = true;
	    	return retval;
	    }
	    
	    protected final boolean isFixed(String state) {
	    	boolean retval = false;
	    	if (state == null) state = "NA"; 
	        if (state.equalsIgnoreCase("Passed QA") || state.equalsIgnoreCase("Fixed") || state.equalsIgnoreCase("Pending QA")) retval = true;
	  
	    	return retval;
	    }
			
	private void addChangeSetListToFile () {
		 ChangeSet changeSet;
		 Integer changesetID;
		 jxl.write.Number value = new jxl.write.Number (0,0,0);
		 int sheetIndex = 0;

		 for (WorkItemFilesRecord record : changeSetArray) {
			 value.setValue(record.getWorkItemId());
			 excelAccess.writeNumberCell (ID_COL, outputRow, value, sheetIndex);
             excelAccess.addLabel(AGREED_RELEASE_COL, record.getWorkItemAgreedRelease(), outputRow, sheetIndex);
             excelAccess.addLabel(TEAM_LEADER_COL, record.getWorkItemTeamLeader(), outputRow, sheetIndex);
			 Iterator<Entry<Integer, ChangeSet>> itr = record.getcsMap().entrySet().iterator();
			 while (itr.hasNext()) {
				 Entry<Integer, ChangeSet> element = itr.next();
				 changesetID = element.getKey();
				 changeSet = element.getValue();
				 value.setValue(changesetID);
				 excelAccess.writeNumberCell (CS_ID_COL, outputRow++, value, sheetIndex);
				 for (String fileName : changeSet.getChangeSetFileArray()) {
					 excelAccess.addLabel(FILENAME_COL, fileName, outputRow++, sheetIndex);
				 }		 
			 }
		 }
		 excelAccess.setColumnWidth(AGREED_RELEASE_COL, 15, sheetIndex);
		 excelAccess.setColumnWidth(TEAM_LEADER_COL, 25, sheetIndex);
	}
	
	private final void addHeaderToFile(){
		 String msg = new String();
		 msg = "Summary:";
		 excelAccess.addLabel (0, msg, outputRow++, 0);
		 msg = "Processed a total of "+querySize+" bugs";
		 excelAccess.addLabel (0, msg, outputRow++, 0);
		 msg = "There were " +processedItems+" bugs analyzed after filter (NAB/NR/Open/In Progress)";
		 excelAccess.addLabel (0, msg, outputRow++, 0);
		 msg = "There were: " +NABs+" Not A Bug defects";
		 excelAccess.addLabel (0, msg, outputRow++, 0);
		 msg = "There were: " +NRs+" Not Reproduced defects";
		 excelAccess.addLabel (0, msg, outputRow++, 0);
		 msg = "There were: " +notFixed+" Bugs in either Open or In progress states";
		 excelAccess.addLabel (0, msg, outputRow++, 0);	
		 msg = "There were: " +workItemssWithChangesetsCount+" bugs with changesets";
		 excelAccess.addLabel (0, msg, outputRow++, 0);
		 msg = "There were: "+(processedItems-workItemssWithChangesetsCount)+" bugs without changesets";
		 excelAccess.addLabel (0, msg, outputRow++, 0);
		 outputRow++;

		 msg = "List of bugs ID's with change sets";
		 excelAccess.addLabel (ID_COL, msg, outputRow, 0);
		 msg = "Agreed Release";
		 excelAccess.addLabel (AGREED_RELEASE_COL, msg, outputRow, 0);
		 msg = "Team Leader";
		 excelAccess.addLabel (TEAM_LEADER_COL, msg, outputRow, 0);
		 msg = "Changeset IDs associated with bug";
		 excelAccess.addLabel (CS_ID_COL, msg, outputRow, 0);
		 msg = "Files in changeset";
		 excelAccess.addLabel(FILENAME_COL, msg, outputRow++, 0);
	}

	private void cleanup() {
		 changeSetArray.clear();
		 changeSetArray = null;
		 queryFile = null;
		 output_file = null;	
		 workItemClient = null;
		 versionControlClient = null;
		 workItemCollection=null;
		 callBack = null;
	}

}

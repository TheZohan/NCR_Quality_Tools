package bugChangeSetReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import tfsAccess.ChangeSetClient;
import tfsAccess.TFSAccess;
import tfsAccess.TFSProgressCallBack;
import tfsReports.TFSReportsGenerator;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;

import excelServices.ExcelServices;

public class BugChangeSetReport implements TFSReportsGenerator {
	class BugIDRecord {
		Integer ID;
		boolean isCustomerDefect;
		String teamLeader = new String();
	}
	 File queryFile, coreBugQueryFile, output_file;	
	 WorkItemCollection workItemCollection=null;

	 private ArrayList<BugIDRecord> bugIDsWithoutChangesets = new ArrayList <BugIDRecord>();

	 TFSProgressCallBack callBack;
	 int querySize, processedItems, coreBugs, NABs, NRs, notFixed;
	 int bugsWithChangesets = 0;
	 TFSAccess tfsAccess=null;	 
     private ExcelServices excelAccess = new ExcelServices();
		
 	public void ReportGenerator (File query, File coreQuery, File userStoryQuery, File output, TFSProgressCallBack callback) {
		callBack = callback;
		   queryFile = query;
		   coreBugQueryFile = coreQuery;
		   output_file = output;
	}

	public void runReports () throws IOException, tfsAccess.TFSException {
		   	   excelAccess.openFiles(output_file);
			   excelAccess.createSheet("Changeset report", 0);
		   	   callBack.progressCallBack("CONNECTING TO TFS....");
			   tfsAccess = new TFSAccess(queryFile, coreBugQueryFile, null,  callBack);
			   workItemCollection = tfsAccess.getWorkItemCollection();
			   callBack.progressCallBack("PROCESSING WORK ITEM DATA....");

			   processWorkItems();
			   	
			   callBack.progressCallBack("DONE: Processed "+querySize+"/"+querySize+" bugs");

		       addHeaderToFile();
		       addListToFile();
		       excelAccess.closeAll();
		       bugIDsWithoutChangesets.clear();
		       tfsAccess = null;
		   
		       Object msg[] = {"SUMMARY:\nProcessed a total of:"+querySize+" bugs\n",
                       "Of which: " +processedItems+" bugs were not filtered out (Core/NAB/NR/Open/In Progress)\n",
                       "There were: " +bugsWithChangesets+" bugs with changesets\n",
                       "There were: "+(processedItems-bugsWithChangesets)+" bugs without changesets"};
		       JOptionPane.showMessageDialog(null, msg);
		}
	
	private void processWorkItems() {
		 querySize = workItemCollection.size();
		 processedItems = querySize;
		 NABs = 0;
		 NRs = 0;
		 coreBugs = 0;
		 notFixed = 0;
		 Integer wid;
	 	 String state = new String ();
	 	 String reason = new String();
	 	 String qaResolution = new String();
	 	 String teamLeader = new String();
		 WorkItem workItem;
		 bugsWithChangesets = 0;
		 WorkItemClient workItemClient = tfsAccess.getWorkItemClient();
		 ChangeSetClient csClient = new ChangeSetClient();
		 
		 csClient.setWorkItemClient(workItemClient);
		 
		 for (int index = 0; index < querySize; index++) {			
			 workItem = workItemCollection.getWorkItem(index);
			 if (workItem == null) {
				 continue;
			 }

			 wid = (Integer) workItem.getFields().getField("ID").getOriginalValue();
						 
			 state = (String)  workItem.getFields().getField("State").getOriginalValue();
			 reason = (String)  workItem.getFields().getField("Reason").getOriginalValue();
			 qaResolution = (String)  workItem.getFields().getField("QA Resolution").getOriginalValue();
			 teamLeader = (String)  workItem.getFields().getField("Team Leader").getOriginalValue();
			 			 
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

			 if (tfsAccess.hasCoreLink(wid)) {
				 processedItems--;
				 coreBugs++;
				 continue; // filter out core links if they exist and if such a query was requested
			 }
			 if (!isFixed (state)) {
				 processedItems--;
				 notFixed++;
				 continue; // filter out defects that are sure not to have change sets				 
			 }
			 
			 workItem = workItemClient.getWorkItemByID(wid);
			
			 csClient.setRecursion();
			 csClient.setWorkItemID(wid.intValue());
			 csClient.setIsCustomerDefect();
			 if (csClient.hasChangeset (workItem) == true) bugsWithChangesets++;
			 else {
				 BugIDRecord bugIDRecord = new BugIDRecord();
				 bugIDRecord.ID = wid;
				 bugIDRecord.teamLeader = teamLeader;
				 if (csClient.getIsCustomerDefect()) bugIDRecord.isCustomerDefect = true;
				 else bugIDRecord.isCustomerDefect = false;
				 bugIDsWithoutChangesets.add(bugIDRecord);
			 }
			 if (index != 0 && index % 5 == 0) callBack.progressCallBack("Processed "+index+"/"+querySize+" work items");
						
		    
		  }
		 csClient = null;
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

	
	private final void addHeaderToFile(){
		 String msg = new String();
		 msg = "Summary:";
		 excelAccess.addLabel (0, msg, 0, 0);
		 msg = "Processed a total of "+querySize+" bugs";
		 excelAccess.addLabel (0, msg, 1, 0);
		 msg = "There were " +processedItems+" bugs analyzed after filter (Core/NAB/NR/Open/In Progress)";
		 excelAccess.addLabel (0, msg, 2, 0);
		 msg = "There were: " +NABs+" Not A Bug defects";
		 excelAccess.addLabel (0, msg, 3, 0);
		 msg = "There were: " +NRs+" Not Reproduced defects";
		 excelAccess.addLabel (0, msg, 4, 0);
		 msg = "There were: " +coreBugs+" Bugs associated to Core";
		 excelAccess.addLabel (0, msg, 5, 0);	
		 msg = "There were: " +notFixed+" Bugs in either Open or In progress states";
		 excelAccess.addLabel (0, msg, 6, 0);	
		 msg = "There were: " +bugsWithChangesets+" bugs with changesets";
		 excelAccess.addLabel (0, msg, 7, 0);
		 msg = "There were: "+(processedItems-bugsWithChangesets)+" bugs without changesets";
		 excelAccess.addLabel (0, msg, 8, 0);
		 msg = "List of bugs ID's without change sets:";
		 excelAccess.addLabel (0, msg, 10, 0);
		 msg = "Customer Defect";
		 excelAccess.addLabel (1, msg, 10, 0);
		 msg = "Team Leader";
		 excelAccess.addLabel (2, msg, 10, 0);
	}
	private final void addListToFile() {
		int sheetIndex = 0;
		int row = 11;
		jxl.write.Number value = new jxl.write.Number (0,0,0);

		for (BugIDRecord arrayElement : bugIDsWithoutChangesets){
				value.setValue(arrayElement.ID.floatValue()); 
				excelAccess.writeNumberCell (0, row, value, sheetIndex);
				if (arrayElement.isCustomerDefect) excelAccess.addLabel(1, "Customer Defect", row, sheetIndex);
	            excelAccess.addLabel(2, arrayElement.teamLeader, row, sheetIndex);
				row++;
		 }
		excelAccess.setColumnWidth(2, 25, sheetIndex);
	}

}

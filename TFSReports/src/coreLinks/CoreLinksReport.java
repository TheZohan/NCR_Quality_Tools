package coreLinks;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import tfsAccess.TFSAccess;
import tfsAccess.TFSException;
import tfsAccess.TFSProgressCallBack;
import tfsAccess.WorkItemData;
import tfsReports.TFSReportsGenerator;
import excelServices.ExcelServices;

public class CoreLinksReport implements TFSReportsGenerator {

	private File allBugsQueryFile, coreBugsQueryFile, output_file;	
	private List <WorkItemData> bugItemList = new ArrayList<>();
	private HashMap <Integer, WorkItemData> bugItemMap = new HashMap<Integer, WorkItemData> ();
	private ArrayList <Integer> extBugs = new ArrayList <Integer>();
	private ArrayList <Integer> coreBugs = new ArrayList <Integer>();

	private TFSProgressCallBack callBack;
	private int querySize, startRow;
	private int extBugsCounter=0, coreBugsCounter=0, coreNabCounter=0, coreNRCounter=0; 
	private ExcelServices excelAccess = new ExcelServices();
		
	public void ReportGenerator (File allBugsQuery, File coreBugsQuery, File userStoryQuery, File output, TFSProgressCallBack callback) {
		callBack = callback;
		   allBugsQueryFile =  allBugsQuery;
		   coreBugsQueryFile = coreBugsQuery;
		   output_file = output;
	}
	
	public void runReports () throws IOException, TFSException {
		   excelAccess.openFiles(output_file);
		   excelAccess.createSheet("Core & Ext Bugs", 0);
		   callBack.progressCallBack("CONNECTING TO TFS....");
		   TFSAccess tmp=null;;
		   tmp = new TFSAccess(allBugsQueryFile, coreBugsQueryFile, null, callBack);
		   bugItemList = tmp.getBugDataList();
		   Collections.sort(bugItemList);
		   callBack.progressCallBack("PROCESSING BUGS DATA....");
		   processList();
		   
		   addVectorsToFile();
		   	   
		   excelAccess.closeAll();
		   callBack.progressCallBack("DONE: Processed "+querySize+"/"+querySize+" bugs");
	       Object msg[] = {"SUMMARY:\nProcessed a total of:"+querySize+" bugs\n",
                   "Of which there were: " +extBugsCounter+" extension bugs\n",
                   +coreBugsCounter+" core bugs\n", coreNabCounter+" Core Not a Bug\n", coreNRCounter+" Core Not reproduced\n"};
	       JOptionPane.showMessageDialog(null, msg);
	}
	

	private final void processList() {
		querySize =  bugItemList.size();
		for (WorkItemData wid: bugItemList) {
			Integer bugID = wid.getId();
			bugItemMap.put(bugID, wid);
			if (wid.getHasCoreLink()) {
				coreBugs.add(bugID);
				coreBugsCounter++;
			}
			else {
				extBugs.add(bugID);
				extBugsCounter++;
			}
		}
	}
	
   	private final void addVectorsToFile() {
	   startRow = 8;

	   addRows (coreBugs, true);
	   
	   addHeader();
	   startRow++;
	   excelAccess.addLabel(0, "Extension Bugs without core links", startRow, 0);
       excelAccess.addLabel(1, "Severity", startRow, 0);
	   excelAccess.addLabel(2, "State", startRow++, 0);
	   
	   addRows (extBugs, false);
   
	   excelAccess.setColumnWidth (0, 30, 0);
	   excelAccess.setColumnWidth (1, 15, 0);
	   excelAccess.setColumnWidth (2, 20, 0);
	}
   	
   	private final void addRows (ArrayList <Integer> array, boolean isCore){
 	   String severity = new String ();
 	   String state = new String ();
 	   String reason = new String();
 	   String qaResolution = new String();
 	   Number bugID;
  	   Iterator<Entry<Integer, WorkItemData>> itr = bugItemMap.entrySet().iterator();
 	   Entry<Integer, WorkItemData> wid;
 	   while (itr.hasNext()) {
 		   wid = itr.next();
 		   bugID = wid.getKey();
 		   if (array.contains(bugID)) {
 			   severity = wid.getValue().getSeverity();
 			   state = wid.getValue().getState();
 			   qaResolution = wid.getValue().getQAResolution();
 			   reason = wid.getValue().getReason();
 			   
 			   if (!isFixed(state)){
 				   if (isNotABug (state, reason, qaResolution)){
 					   state = "Not a bug";
 					   if (isCore) coreNabCounter++;
 				   }
 				   else if (isNotReproduced (state, reason, qaResolution)){
 					   state = "Not reproduced";
 					   if (isCore) coreNRCounter++;
 				   }
 			   }
 			   addRowToFile (excelAccess, startRow++, bugID, severity, state);
 		   }
 	   }	
   	}
	
	private final void addHeader(){
		 String msg = new String();
		 msg = "Summary:";
		 excelAccess.addLabel (0, msg, 0, 0);
		 msg = "Processed a total of "+querySize+" bugs";
		 excelAccess.addLabel (0, msg, 1, 0);
		 msg = "Of which there were: " +extBugsCounter+" extension bugs";
		 excelAccess.addLabel (0, msg, 2, 0);
		 msg = coreBugsCounter+" core bugs";
		 excelAccess.addLabel (0, msg, 3, 0);
		 msg = coreNabCounter+" Core Not a Bug";
		 excelAccess.addLabel (0, msg, 4, 0);
		 msg = coreNRCounter+" Core Not reproduced"; 
		 excelAccess.addLabel (0, msg, 5, 0);
		 
		 msg = "Extension bugs with core links";
		 excelAccess.addLabel(0, msg, 7, 0);
		 excelAccess.addLabel(1, "Severity", 7, 0);
		 excelAccess.addLabel(2, "State", 7, 0);

	}
	
	private final void addRowToFile (ExcelServices excelAccess, int row, Number wid, String severity, String state) {
		int sheetIndex = 0;
		int col = 0;
		jxl.write.Number value = new jxl.write.Number (0,0,0);
		value.setValue(wid.floatValue());
		excelAccess.writeNumberCell (col++, row, value, sheetIndex);
		excelAccess.addLabel(col++, severity, row, sheetIndex);
		excelAccess.addLabel(col, state, row, sheetIndex);		
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

	@Override
	public void setMaxRecursion(int maxDepth) {
		// TODO Auto-generated method stub
		
	}
	
}


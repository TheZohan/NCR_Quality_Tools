package reopenReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JOptionPane;

import tfsAccess.TFSAccess;
import tfsAccess.TFSProgressCallBack;
import tfsReports.TFSReportsGenerator;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;
import com.microsoft.tfs.core.clients.workitem.revision.Revision;

public class ReopenReportGenerator implements TFSReportsGenerator{
    public enum workItemStates {
 	   OPEN,
 	   IN_PROGRESS,
 	   FIXED,
 	   PENDING_QA,
 	   PASSED_QA,
 	   NOT_A_BUG,
 	   NOT_REPRODUCED,
 	   CLOSED;
    }
	File allBugsQueryFile, coreBugsQueryFile, output_file;	
	private WorkItemClient workItemClient;
	private VersionControlClient versionControlClient;
	WorkItemCollection bugWorkItems=null;
	WorkItemLinkInfo[] linkItems=null;
    private ArrayList<Integer> defectIDsWithoutChangesets = new ArrayList <Integer>();
    private TFSProgressCallBack callBack;
	//ReopenReportApp appWinRef;
	int querySize, remainingBugs;
	 int hitCount = 0;
	 int bugCount = 0;
	 int bugCountWithoutCoreLink = 0;
	 int bingoCount = 0;
	 int bugsWithChangesetsCount = 0;
	 int globalBugsWithChangesetsCount = 0;
	 boolean processCore = true;
		
		public void ReportGenerator (File allBugsQuery, File coreBugsQuery, File userStoryQuery, File output, TFSProgressCallBack callback) {
			callBack = callback;
		   allBugsQueryFile =  allBugsQuery;
		   coreBugsQueryFile = coreBugsQuery;
		   output_file = output;
		   if (coreBugsQuery == null) processCore = false;
	}

	public void runReports () throws IOException, tfsAccess.TFSException {
		       FileIO fileIO = new FileIO(output_file);
		       callBack.progressCallBack("CONNECTING TO TFS....");
			   TFSAccess tmp=null;
			   tmp = new TFSAccess(allBugsQueryFile, coreBugsQueryFile, null, callBack);
			   bugWorkItems = tmp.getWorkItemCollection();
			   linkItems = tmp.getLinkCollection ();
			   workItemClient = tmp.getWorkItemClient();
			   versionControlClient = tmp.getVersionControlClient();
			   callBack.progressCallBack("PROCESSING BUGS DATA....");

			   processWorkItems();
			   	   			   
			   callBack.progressCallBack("DONE: Processed "+querySize+"/"+querySize+" bugs");
		       fileIO.writeLinenToFile("SUMMARY:");
		       fileIO.writeLinenToFile("Processed a total of:"+querySize+" Defects");
		       fileIO.writeLinenToFile("of which:" +bugCountWithoutCoreLink+" items without core links");
		       fileIO.writeLinenToFile("of which:" +bugCount+" items that were in Fixed state at some time");
		       fileIO.writeLinenToFile("Detected:" +hitCount+" bugs with Reverse transitions");
		       fileIO.writeLinenToFile("of which:" +bugsWithChangesetsCount+" had associated changesets");
		       fileIO.writeLinenToFile("of which: " +bingoCount+" had changesets associated after the reverse transition");
		       fileIO.writeLinenToFile("From all post-core-filter bugs there were: " +globalBugsWithChangesetsCount+" Defects with changesets");
		       fileIO.writeLinenToFile("");
		       fileIO.writeLinenToFile("List of Defect ID's (that were fixed at some point in their lifecycle) without change sets:");
		       for (Integer arrayElement : defectIDsWithoutChangesets) fileIO.writeLinenToFile(arrayElement.toString());
			   fileIO.closeFile();
			   
		       Object msg[] = {"SUMMARY:\nProcessed a total of:"+querySize+" Defects\n",
                       "of which:" +bugCountWithoutCoreLink+" items without core links\n",
                       "of which:" +bugCount+" items that were in Fixed state at some time\n",
                       "Detected:" +hitCount+" bugs with Reverse transitions\n",
                       "of which:" +bugsWithChangesetsCount+" had associated changesets\n",
                       "of which: " +bingoCount+" had changesets associated after the reverse transition\n",
                       "From all post-core-filter bugs there were: " +globalBugsWithChangesetsCount+" Defects with changesets"};
		       JOptionPane.showMessageDialog(null, msg);
		}
	
	private void processWorkItems() {
		 querySize = bugWorkItems.size();
		 remainingBugs = querySize;
		 Integer wid;
		 WorkItem workItem;
		 hitCount = 0;
		 bugCount = 0;
		 bugCountWithoutCoreLink = 0;
		 bingoCount = 0;
		 bugsWithChangesetsCount = 0;
		 globalBugsWithChangesetsCount = 0;
		 
		 for (int index = 0; index < querySize; index++) {
			 boolean hasChangeset=false, changesetFound = false, isHit=false, isBingo=false, wasEverFixed = false;
			
			 workItem = bugWorkItems.getWorkItem(index);
			 if (workItem == null) continue;

			 wid = (Integer) workItem.getFields().getField("ID").getOriginalValue(); 
			 workItem = workItemClient.getWorkItemByID(wid);
			 if (processCore == true) {
		       if (hasCoreLink (wid, workItem) == true){
				 remainingBugs--;
				 continue;  // filter out irrelevant bugs
			   }
			   bugCountWithoutCoreLink++;
			 }
			
			if (hasChangeset (workItem) == true) {
				globalBugsWithChangesetsCount++;
				changesetFound = true;
			}
			if (bugCount > 0 && bugCount % 20 == 0) callBack.progressCallBack("Processed "+bugCount+"/"+remainingBugs+" bugs");
						
		    Revision prevRev = null;
		    for (Revision rev : workItem.getRevisions()){
		       if (prevRev == null) {
		          prevRev = rev;
		    	  continue;
		       }
		    
		       for (int i=0; i < rev.getFields().length; i++) {
	    		  if (rev.getFields()[i].getName().equalsIgnoreCase("State")){
	    			if (rev.getFields()[i].getValue() != prevRev.getFields()[i].getValue()) {
		    			workItemStates state = setState ((String) rev.getFields()[i].getValue());
		    			if (state == workItemStates.FIXED) wasEverFixed = true;
	    				workItemStates prevState = setState ((String) prevRev.getFields()[i].getValue());

	    				if (isReverseTransition (state, prevState)){
	    				  Date date1 = rev.getRevisionDate();
	    				  if (isHit == false) {
	    					 hitCount++;
	    					 isHit = true;
	    			 	  }

	    				for (Link link : workItem.getLinks()) {			
			    		    String linkType = link.getLinkType().getName();
   				            if (link instanceof ExternalLink && linkType.equalsIgnoreCase("Fixed in Changeset"))
   				            {
  				            	if (hasChangeset == false){
   				            		bugsWithChangesetsCount++;
   				            		hasChangeset = true;
   				            	}
   				                ExternalLink externalLink = (ExternalLink) link;	            
   				                Changeset changeset = versionControlClient.getChangeset(Integer.parseInt(getChangesetID(externalLink.getURI())));
   				                Date date2 = changeset.getDate().getTime();
   				                if (date2.getTime() > date1.getTime()){
 		    					   if (isBingo == false) {
   		    							bingoCount++;
		    							isBingo = true;
		    					   }
			    				}
			    			}
			    		  }
	    				}
	    		     }
	    		  }
	           }	    			       
		       prevRev = rev; 
		     }
		     if (wasEverFixed == true){
		    	 bugCount++;
		    	 if (changesetFound == false) defectIDsWithoutChangesets.add(wid);
		     }
		     else remainingBugs--;
		  }
	}
	
	public boolean hasCoreLink (Integer id, WorkItem workItem) {
		boolean retVal = false;
		if (linkItems == null) return false;
		
		int numLinkedRows = linkItems.length;
		if (numLinkedRows == 0) retVal = false;
		else {
			for (int row = 1; row < numLinkedRows; row++) {
				WorkItemLinkInfo link = linkItems[row];
				if ((id == link.getTargetID()) && (link.getLinkTypeID() == 0) && (link.getSourceID() == 0)) {
					retVal = true;
					break;
				}
			}
		}
		return retVal;
	}
	 public boolean hasChangeset (WorkItem workitem) {
         boolean retVal = false;
		 for (Link link : workitem.getLinks())
	        {
		
	            String linkType = link.getLinkType().getName();

	            if (link instanceof ExternalLink && linkType.equalsIgnoreCase("Fixed in Changeset")) {
	            	retVal = true;
	            	break;
	            }
	           
	        }
		 return retVal;
	 }
	 
		static String getChangesetID(String changeSetIdPath) {
			String tokens[] = changeSetIdPath.split("[/]");
			return tokens[tokens.length -1];
		}
		

		 private workItemStates setState (String state) {
			 workItemStates retVal = null;
			 switch (state) {
			 case "Open": retVal = workItemStates.OPEN;
			 break;
			 case "Fixed": retVal = workItemStates.FIXED;
			 break;
			 case "In Progress": retVal = workItemStates.IN_PROGRESS;
			 break;
			 case "Pending QA": retVal = workItemStates.PENDING_QA;
			 break;
			 case "Passed QA": retVal = workItemStates.PASSED_QA;
			 break;
			 case "Closed": retVal = workItemStates.CLOSED;
			 break;
			 case "Not a bug": retVal = workItemStates.NOT_A_BUG;
			 break;
			 case "Not Reproduced": retVal = workItemStates.NOT_REPRODUCED;
			 break;
			 }
			 
			 return retVal;
		 }

		 private boolean isReverseTransition (workItemStates currentState, workItemStates previousState) {
			 boolean retVal = false;
			 if (currentState == null || previousState == null) {
				 retVal = false;
			 }
			 else {
			   switch (currentState) {
			   case OPEN:
				 if (previousState == workItemStates.FIXED || previousState == workItemStates.IN_PROGRESS || previousState == workItemStates.PENDING_QA || 
				 previousState == workItemStates.PASSED_QA || previousState == workItemStates.CLOSED) retVal = true;
				 break;
			   case IN_PROGRESS:
			 	 if (previousState == workItemStates.FIXED || previousState == workItemStates.PENDING_QA || previousState == workItemStates.PASSED_QA || previousState == workItemStates.CLOSED) retVal = true;
			   case PENDING_QA:
				 if (previousState == workItemStates.CLOSED || previousState == workItemStates.PASSED_QA) retVal = true;
				 break;
			   default: retVal = false;
				 break;
			  }
			 }
			 
			 return retVal;
		 }
		 
}

package changeSetMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ResourceAccessException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;

public class ChangeSetClient {
	private WorkItemClient workItemClient;
    int workItemID;
    int recursionDepth = 0;
    static final int  MAX_RECURSION_DEPTH = 3;
    ArrayList <ExternalLink> externalLinks = new ArrayList<ExternalLink>();
    private HashSet<Integer> relatedWorkItemLinks = new HashSet<Integer>();
    int currentLink;
    private VersionControlClient versionControlClient;

    public void resetClientFields () {
    	externalLinks.clear();
    	recursionDepth = 0;
    }
 
    public void setWorkItemID(int wid) {
    	workItemID = wid;
    }
    public void setVersionControlClient (VersionControlClient vcc){
    	versionControlClient = vcc;
    }
    public void setWorkItemClient(WorkItemClient wiClient) {
    	workItemClient = wiClient;
    }
    
    public boolean hasChangeset (WorkItem workitem) {  
         boolean retVal = false;
         String typeName;
                
       int currentWid = workitem.getID();
       if (currentWid == workItemID && recursionDepth > 0) return false;
       recursionDepth++;
       
       if (recursionDepth > MAX_RECURSION_DEPTH) {
    	   recursionDepth--;
    	   return false;
       }
       
       typeName = workitem.getType().getName();        	     
       if (!typeName.equalsIgnoreCase("Bug") && !typeName.equalsIgnoreCase("User Story") && !typeName.equalsIgnoreCase("Task")){
    	   recursionDepth--;
    	   return false;
       }
       
       if (relatedWorkItemLinks.contains(currentWid) && currentLink != currentWid) {
    	   recursionDepth--;
    	   return false;
       }

       	   
 		 for (Link link : workitem.getLinks())
	        { 
	            String linkType = link.getLinkType().getName();
	            if (link instanceof ExternalLink && linkType.equalsIgnoreCase("Fixed in Changeset")) {
	            	externalLinks.add((ExternalLink) link);
	            	retVal = true;
	            }
	            else if (link instanceof RelatedLink) {
	             	RelatedLink rLink = (RelatedLink) link;
	             	int linkID = rLink.getTargetWorkItemID();
	             	if (!relatedWorkItemLinks.contains(linkID)){
		              relatedWorkItemLinks.add(linkID);      	//avoid looping back to a work item already tested
		              currentLink = linkID;
	             	  WorkItem linkedItem = workItemClient.getWorkItemByID(linkID);
	            	  if (linkedItem != null && hasChangeset (linkedItem) == true) {
	            		 retVal = true;
	            	  }
	             	}
	             }    		
	        }
 		 recursionDepth--;
		 return retVal;
	 }
    
	 public void addWorkItemChangeSetsToRecord(WorkItemChangeSetRecord record){
		 Integer csID;
		 for (ExternalLink link: externalLinks){
			  	csID = Integer.parseInt(getChangesetID(link.getURI()));
			  	Changeset changeset;
			  	try {
		  		   changeset = versionControlClient.getChangeset(csID);  // occasional changeset read access denials in TFS.need to ignore  
			  	} catch (ResourceAccessException e) {
			  		continue;
			  	}
			  	Date date = changeset.getDate().getTime();
			  	Change changes[] = changeset.getChanges();			  	
                for (Change changeItem : changes) {
                  String changeName = changeItem.getItem().getServerItem();
                  Integer fileID = changeItem.getItem().getItemID();
                  record.addChangeSetToRecord(csID, date, fileID, changeName);
                }	    
		 }
	 }

	 static String getChangesetID(String changeSetIdPath) {
			String tokens[] = changeSetIdPath.split("[/]");
			return tokens[tokens.length -1];
	}	  
}

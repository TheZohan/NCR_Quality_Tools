package tfsAccess;

import java.util.HashSet;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;

public class ChangeSetClient {
	private WorkItemClient workItemClient;
	private static int MAX_RECURSION_DEPTH = 3;
	private int maxRecursionDepth = MAX_RECURSION_DEPTH;
	private int recursionDepth;
	private int workItemID;
    private HashSet<Integer> relatedWorkItemLinks = new HashSet<Integer>();
 
    private boolean isCustomerDefect;

    public void setMaxRecursion(int depth) {
    	maxRecursionDepth = depth;
    }
    public void setRecursion() {
    	recursionDepth = 0;
    }
    public void setWorkItemClient(WorkItemClient wiClient) {
    	workItemClient = wiClient;
    }
    public void setWorkItemID(int wid) {
    	workItemID = wid;
    }
    public boolean getIsCustomerDefect() {
    	return this.isCustomerDefect;
    }
    public void setIsCustomerDefect() {
    	isCustomerDefect = false;
    }
 
    
    public boolean hasChangeset (WorkItem workitem) {  //Recursive method with limit on recursion depth
         boolean retVal = false;
         String typeName;
         
         typeName = workitem.getType().getName();
         if (typeName.equalsIgnoreCase("Customer defect")) isCustomerDefect = true; // set it only once per work item
        	     
         if ((!typeName.equalsIgnoreCase("Customer Defect") && !typeName.equalsIgnoreCase("Bug")) || typeName.equalsIgnoreCase("Test case")) return false;
         
         if (workitem.getID() == workItemID) { // avoid circular links to original work item
        	 if (recursionDepth > 0) {
        		 return false;
        	 }
         }
         if (recursionDepth > maxRecursionDepth) {
         	   recursionDepth--;
         	   return false;
          }
          else recursionDepth++;
        
		 for (Link link : workitem.getLinks())
	        {
			 
	            String linkType = link.getLinkType().getName();
	            if (link instanceof ExternalLink && linkType.equalsIgnoreCase("Fixed in Changeset")) {
	            	retVal = true;
	            	break;
	            }
	            
	            if (maxRecursionDepth == 0) continue;
         	
	            else if (link instanceof RelatedLink && recursionDepth <= maxRecursionDepth-1) {
	            	RelatedLink rLink = (RelatedLink) link;
	            	int linkID = rLink.getTargetWorkItemID();
	             	if (!relatedWorkItemLinks.contains(linkID)){
	            	   WorkItem linkedItem = workItemClient.getWorkItemByID(linkID);
	            	   if (linkedItem != null && hasChangeset (linkedItem) == true) {
	            		  retVal = true;
	            		  break;
	            	   }
	            	   relatedWorkItemLinks.add(linkID);      	//avoid looping back to a work item already tested
	             	}
	            }
	        }
		 recursionDepth--;
		 return retVal;
	 }
    
	 static String getChangesetID(String changeSetIdPath) {
			String tokens[] = changeSetIdPath.split("[/]");
			return tokens[tokens.length -1];
	}
}

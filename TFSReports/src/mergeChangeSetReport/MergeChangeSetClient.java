package mergeChangeSetReport;

import java.util.ArrayList;
import java.util.Date;

import tfsAccess.WorkItemFilesRecord;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ResourceAccessException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Link;

public class MergeChangeSetClient {
	private ArrayList <ExternalLink> externalLinks = new ArrayList<ExternalLink>();
    private VersionControlClient versionControlClient;

    public void resetExternalLinks() {
    	externalLinks.clear();
    }

    public void setVersionControlClient (VersionControlClient vcc){
    	versionControlClient = vcc;
    }
    
    public boolean hasChangeset (WorkItem workitem) {  
         boolean retVal = false;
         String typeName = new String();
         
         typeName = workitem.getType().getName();        	     
         if (typeName.equalsIgnoreCase("Test case")) return false;
        
		 for (Link link : workitem.getLinks())
	        {
			 
	            String linkType = link.getLinkType().getName();
	            if (link instanceof ExternalLink && linkType.equalsIgnoreCase("Fixed in Changeset")) {
	            	externalLinks.add((ExternalLink) link);
	            	retVal = true;
	            }
	        }
		 return retVal;
	 }
    
	 public void addWorkItemChangeSetsToRecord(WorkItemFilesRecord record){
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
			  	record.setRecordDate(date);
			  	Change changes[] = changeset.getChanges();			  	
                for (Change changeItem : changes) {
                  String changeName = changeItem.getItem().getServerItem();
                 //String test = versionControlClient.getItem(itemID, ID).getServerItem(); //DEBUG
                  record.addRecordFile(csID, changeName);
                }	    
		 }
	 }

	 static String getChangesetID(String changeSetIdPath) {
			String tokens[] = changeSetIdPath.split("[/]");
			return tokens[tokens.length -1];
	}	  
}

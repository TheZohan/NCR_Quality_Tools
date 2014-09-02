package tfsAccess;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.exceptions.TFSAccessException;
import com.microsoft.tfs.core.exceptions.TFSUnauthorizedException;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;

public class TFSAccess {
	private List<WorkItemData> bugItemDataList = new ArrayList<>();
	private List<WorkItemData> userStoryItemDataList = new ArrayList<>();
	private Project project;
	private WorkItemClient workItemClient;
	private String tmpProjectNameSubstring, tmpWiqlQuery;
	private String wiqlQuery=null;
	private String wiqlCoreLinksQuery=null;
	private String wiqlEscapingBugsQuery=null;
	private String wiqlUSQuery = null;
	private String projectName=null;
	private WorkItemCollection bugWorkItems=null;
	private WorkItemCollection storyWorkItems=null;
	private WorkItemLinkInfo[] LinkItems=null;
	private WorkItemCollection escapingDefectsItems = null;
	private VersionControlClient versionControlClient;
	private TFSProgressCallBack callBack;
	
	     
	public TFSAccess (File bugsQueryFile, File controlQueryFile, File userStoryQueryFile, TFSProgressCallBack callback) throws TFSException {
		callBack = callback;
		 setupTFSQueries (bugsQueryFile, controlQueryFile, userStoryQueryFile);
		 callBack.progressCallBack("CONNECTING TO TFS....");
		 try {
			 tfsConnect();
		 } catch (TFSException e) {
			 throw e;
		 }
		 callBack.progressCallBack("GETTING TFS QUERIES....");
		 runTFSQueries ();
		 callBack.progressCallBack("COLLECTING BUG DATA....");
		 getBugWorkItemData();
		 if (userStoryQueryFile != null) getUserStoryWorkItemData();
	}
	
	 public List<WorkItemData> getBugDataList() {
		 return bugItemDataList;
	 }
	 public List<WorkItemData> getUSDataList() {
		 return userStoryItemDataList;
	 }
	 public WorkItemCollection getWorkItemCollection() {
		return bugWorkItems;
	 }
	 public WorkItemClient getWorkItemClient() {
		return workItemClient;
	 }
	 public VersionControlClient getVersionControlClient() {
		return versionControlClient;
	 }
	 public WorkItemLinkInfo[] getLinkCollection() {
		 return LinkItems;
	 }
	
	private void tfsConnect() throws TFSException
	{    		    	
		java.net.URI uri=null;
		TFSTeamProjectCollection tpc = null;
		try {
			uri = new URI ("http://srraatfsapp:8080/tfs/defaultcollection");
			tpc =  new TFSTeamProjectCollection(uri, null);
			project = tpc.getWorkItemClient().getProjects().get(projectName);  
			workItemClient = project.getWorkItemClient();
			versionControlClient = tpc.getVersionControlClient();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			TFSException t = new TFSException();
			throw t;
		}catch (TECoreException te) {
			if (te instanceof TFSUnauthorizedException || te instanceof TFSAccessException){
				LoginDialog login = new LoginDialog();
				String username = login.getUserName();
				String password = login.getPassword();
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username,password);
				tpc =  new TFSTeamProjectCollection(uri, credentials);
				project = tpc.getWorkItemClient().getProjects().get(projectName);  
				workItemClient = project.getWorkItemClient();
			}
			else {
				te.printStackTrace();
			    TFSException t = new TFSException();
			    throw t;
			}
		}
	 }  
	 	 
	 private void setupTFSQueries (File bugsQueryFile, File controlQueryFile, File userStoryQueryFile) {
		 final String selectAllString = "SELECT [System.Id], [System.State], [Microsoft.VSTS.Common.StateChangeDate],"+
                 "[Retalix.CustomTemplate.ActualEffort], [Retalix.CustomTemplate.BugType],"+ 
                 "[Microsoft.VSTS.Common.ResolvedDate], [Retalix.CustomTemplate.AgreedRelease], [System.CreatedDate],[Microsoft.VSTS.Common.Severity],"+
                 "[Retalix.CustomTemplate.SoftwareDomain], [System.Reason], [Retalix.CustomTemplate.TeamLeadName], [Retalix.CustomTemplate.QAResolution]";
		 final String selectControlQueryString = "SELECT [System.Id]";
		 final String selectUSString = "SELECT [System.Id], [System.State], [Retalix.CustomTemplate.ActualEffort], [Microsoft.VSTS.Common.StateChangeDate], [Microsoft.VSTS.Scheduling.OriginalEstimate]";

		 String projectNameSubstring = "'";	    			
		 getTFSQuery (bugsQueryFile);
		 projectName = tmpProjectNameSubstring;
		 projectNameSubstring += tmpProjectNameSubstring+"'";

		 wiqlQuery = tmpWiqlQuery.replaceAll("@project", projectNameSubstring);
		 wiqlQuery = replaceSubString (wiqlQuery, selectAllString);
		 
		 if (controlQueryFile != null) {
		   getTFSQuery (controlQueryFile);
		   if (isLinkQuery(tmpWiqlQuery)){
			   wiqlCoreLinksQuery = tmpWiqlQuery.replaceAll("@project", projectNameSubstring);
			   wiqlCoreLinksQuery = replaceSubString (wiqlCoreLinksQuery, selectControlQueryString);
		   }
		   else {
			   wiqlEscapingBugsQuery = tmpWiqlQuery.replaceAll("@project", projectNameSubstring);
			   wiqlEscapingBugsQuery = replaceSubString (wiqlEscapingBugsQuery, selectControlQueryString);
		   }
		 }
		 
		 if (userStoryQueryFile != null) {
			   getTFSQuery (userStoryQueryFile);
			   wiqlUSQuery = tmpWiqlQuery.replaceAll("@project", projectNameSubstring);	 
			   wiqlUSQuery = replaceSubString (wiqlUSQuery, selectUSString);
		 }
	 }
	 
	 private void runTFSQueries() {
		 bugWorkItems = workItemClient.query(wiqlQuery);
		 if (wiqlCoreLinksQuery != null) {
	         Query linkQuery = workItemClient.createQuery(wiqlCoreLinksQuery);
	         LinkItems = linkQuery.runLinkQuery();
		 }
		 if (wiqlUSQuery != null) {
			   storyWorkItems = workItemClient.query(wiqlUSQuery);
		 }
		 if (wiqlEscapingBugsQuery != null) {
			   escapingDefectsItems = workItemClient.query(wiqlEscapingBugsQuery);
		 }
	 }
	 
	 private void getTFSQuery (File queryFile) {
		 TFSQueryReader tmp;
			try {
				tmp = new TFSQueryReader (queryFile);
				tmpWiqlQuery = new String (tmp.getWiqlQuery()); 
				tmpProjectNameSubstring = new String (tmp.getProjectName());
             
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}          
     }

	 
	 private void getBugWorkItemData() {
		   int querySize = bugWorkItems.size();
		   int tmpCounter = 0;
	       for (int i = 0; i < querySize; i++)  
	       { 	
	         WorkItemData wid = new WorkItemData();
	         WorkItem workItem = bugWorkItems.getWorkItem(i);

	         wid.setId((Integer) workItem.getFields().getField("ID").getOriginalValue());
	         wid.setState((String) workItem.getFields().getField("State").getOriginalValue());
	         wid.setQAResolution((String) workItem.getFields().getField("QA Resolution").getOriginalValue());
	         wid.setReason((String) workItem.getFields().getField("Reason").getOriginalValue());
	         wid.setStateChangeDate((Date) workItem.getFields().getField("State Change Date").getOriginalValue());
	         wid.setCreateDate((Date) workItem.getFields().getField("Created Date").getOriginalValue());
	         wid.setBugType((String) workItem.getFields().getField("Bug Type").getOriginalValue());
	         wid.setResolvedDate((Date) workItem.getFields().getField("Resolved Date").getOriginalValue());
	         wid.setSWDomain((String) workItem.getFields().getField("Software Domain").getOriginalValue());
	         wid.setSeverity((String) workItem.getFields().getField("Severity").getOriginalValue());
	         wid.setIsEscapingDefect(isEscapingDefect (wid.getId(), escapingDefectsItems));
	         wid.setHasCoreLink(hasCoreLink (wid.getId(), LinkItems));
	         wid.setSizeField(1);
	            
	         bugItemDataList.add(wid);   
	         if (++tmpCounter % 100 == 0)  callBack.progressCallBack("Processed "+tmpCounter+"/"+querySize+" bugs");
	        }
	 }
	 private void getUserStoryWorkItemData() {
	       for (int i = 0; i < storyWorkItems.size(); i++)  
	       { 	
	         WorkItemData wid = new WorkItemData();
	         WorkItem workItem = storyWorkItems.getWorkItem(i);

	         wid.setId((Integer) workItem.getFields().getField("ID").getOriginalValue());
	         wid.setState((String) workItem.getFields().getField("State").getOriginalValue());
	         wid.setStateChangeDate((Date) workItem.getFields().getField("State Change Date").getOriginalValue());
	         wid.setReadyEstimate((Number) (workItem.getFields().getField("Ready Estimate").getOriginalValue()));
	         wid.setOriginalEstimate((Number) workItem.getFields().getField("Original Estimate").getOriginalValue());

	         if (wid.getReadyEstimate() != null) wid.setSizeField(wid.getReadyEstimate());
	         else if (wid.getOriginalEstimate() != null) wid.setSizeField(wid.getOriginalEstimate());
	         else wid.setSizeField(4); // use nominal average value for US's for those that haven't been estimated yet
	            
	         userStoryItemDataList.add(wid);      
	        }
	 }
	 	 
	 private boolean hasCoreLink (int id, WorkItemLinkInfo[] linkItems) {
			boolean retval = false;
			if (linkItems == null) return false;
			
			int numLinkedRows = linkItems.length;
			if (numLinkedRows == 0) retval = false;
			else {
				for (int row = 1; row < numLinkedRows; row++) {
					WorkItemLinkInfo link = linkItems[row];
					if ((id == link.getTargetID()) && (link.getLinkTypeID() == 0) && (link.getSourceID() == 0)) {
						retval = true;
						break;
					}
				}
			}
			return retval;
		}
	 ////For compatibility with bugChangeSetReport:
	 public boolean hasCoreLink (Integer wid) {
		 return hasCoreLink (wid.intValue(), LinkItems);
	 }

	 private boolean isEscapingDefect (int id, WorkItemCollection escapingItems) {
			boolean retval = false;
			if (escapingItems == null) return false;
			
			int numEscapingRows = escapingItems.size();
			if (numEscapingRows == 0) retval = false;
			else {
				for (int row = 1; row < numEscapingRows; row++) {
					WorkItem wid = escapingItems.getWorkItem(row);
					if (id == wid.getID()){
						retval = true;
						break;
					}
				}
			}
			return retval;
	}
	 
	private boolean isLinkQuery (String wiqlQuery) {
		boolean retval = false;
		if (wiqlQuery.toLowerCase().contains("workitemlinks")) retval = true;
		return retval;
		
	}
	 
	private String replaceSubString (String str, String repStr) {
		 String splitString[];
		 splitString = str.split("FROM");
		 String newString = new String (repStr+" FROM "+splitString[1]);
		 return newString;
	 }
}
	 
	 


package changeSetMap;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import tfsAccess.TFSQueryReader;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;
import com.microsoft.tfs.core.exceptions.TECoreException;


public class TFSAccess {
    Project project;
	WorkItemClient workItemClient;
	VersionControlClient versionControlClient;
	String tmpProjectNameSubstring, tmpWiqlQuery;
	String wiqlQuery=null;
	String wiqlCoreLinksQuery=null;
	String projectName=null;
	WorkItemCollection bugWorkItems=null;
	WorkItemLinkInfo[] LinkItems=null;
	ChangeSetMapApp appWinRef;
		     
	public TFSAccess (File queryFile,  ChangeSetMapApp appWin) throws TFSException {
		 appWinRef = appWin;
		 setupTFSQueries (queryFile);
		 appWinRef.progressCallBack("CONNECTING TO TFS....");
		 tfsConnect();
		 appWinRef.progressCallBack("GETTING TFS QUERIES....");
		 runTFSQueries ();
		 appWinRef.progressCallBack("COLLECTING BUG DATA....");
	}
	
	 public WorkItemCollection getBugCollection() {
		 return bugWorkItems;
	 }
	 public WorkItemClient getWorkItemClient (){
		 return workItemClient;
	 }
	 public WorkItemLinkInfo[] getLinkCollection() {
		 return LinkItems;
	 }
	 public VersionControlClient getVersionControlClient (){
		 return versionControlClient;
	 }
	 
	
	private void tfsConnect() throws TFSException
	{    		    	
		java.net.URI uri=null;
		try {
			uri = new URI ("http://srraatfsapp:8080/tfs/defaultcollection");
	    
			TFSTeamProjectCollection tpc =  new TFSTeamProjectCollection(uri, null);

			project = tpc.getWorkItemClient().getProjects().get(projectName);  
			workItemClient = project.getWorkItemClient();
			versionControlClient = tpc.getVersionControlClient();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			TFSException t = new TFSException();
			throw t;
		}catch (TECoreException te) {
			te.printStackTrace();
			TFSException t = new TFSException();
			throw t;
		}
	 }  
	 	 
	 private void setupTFSQueries (File bugsQueryFile) {
		 String projectNameSubstring = "'";	    			
		 getTFSQuery (bugsQueryFile);
		 projectName = tmpProjectNameSubstring;
		 projectNameSubstring += tmpProjectNameSubstring+"'";

		 wiqlQuery = tmpWiqlQuery.replaceAll("@project", projectNameSubstring);
		 
	 }
	 
	 private void runTFSQueries() {
		   bugWorkItems = workItemClient.query(wiqlQuery);
		   if (wiqlCoreLinksQuery != null) {
	         Query linkQuery = workItemClient.createQuery(wiqlCoreLinksQuery);
	         LinkItems = linkQuery.runLinkQuery();
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
}
	 
	 

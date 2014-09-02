package defectDistribution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import tfsAccess.TFSException;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;

import excelServices.ExcelServices;
import tfsAccess.TFSAccess;
import tfsAccess.TFSProgressCallBack;

public class DefectDistribution {
    private static final int MAX_LEVELS = 20;
    private static final int MAX_BUG_LIST_SIZE = 100;
    private int finalNumberOfLevels = 0;
	private File bugsQueryFile, outputFile; 
	private WorkItemCollection bugWorkItems=null;
	private WorkItemClient workItemClient;
	private VersionControlClient versionControlClient;
	private long changeCounter = 0;
	TFSProgressCallBack callBack;
	
    @SuppressWarnings("unchecked")
	private SortableHashMap<String, Integer> distributionByPath[] = new SortableHashMap [MAX_LEVELS];           // Each level of depth into the changeset  file structure will get its own map
	@SuppressWarnings("unchecked")
	private ArrayList <HashPairs<String, Integer>> distributionByPathMap[] = new ArrayList[MAX_LEVELS];
    @SuppressWarnings("unchecked")
	private SortableHashMap<String, Integer> distributionByBug[] = new SortableHashMap [MAX_LEVELS];            // Each level of depth into the changeset  file structure will get its own map
	@SuppressWarnings("unchecked")
	private ArrayList <HashPairs<String, Integer>> distributionByBugMap[] = new ArrayList[MAX_LEVELS];
    @SuppressWarnings("unchecked")
	private ArrayList<String> changePaths[] = new ArrayList[MAX_LEVELS];
    @SuppressWarnings("unchecked")
	private ArrayList<Number> changePathCounts[] = new ArrayList[MAX_LEVELS];
    @SuppressWarnings("unchecked")
	private ArrayList<String> bugPaths[] = new ArrayList[MAX_LEVELS];
    @SuppressWarnings("unchecked")
	private ArrayList<Number> bugPathCounts[] = new ArrayList[MAX_LEVELS];
    @SuppressWarnings("unchecked")
	private HashMap <String, SortableHashMap<Integer, Integer>> masterMap[] = new HashMap [MAX_LEVELS];  
    private HashMap <Integer, String> bugTitles = new HashMap <Integer, String>();  							// a single global map of id's to titles
    @SuppressWarnings("unchecked")
	private ArrayList <Number> idList[] = new ArrayList[MAX_LEVELS];
	@SuppressWarnings("unchecked")
	private ArrayList <String> titleList[] = new ArrayList[MAX_LEVELS];
    private ExcelServices excelAccess = new ExcelServices();
		 
	
	public DefectDistribution(TFSProgressCallBack callback) {
		 callBack = callback;
		 for (int i = 0; i < MAX_LEVELS; i++) {
			 distributionByPath[i] = new SortableHashMap<String, Integer>();
			 distributionByBug[i] = new SortableHashMap<String, Integer>();
			 distributionByPathMap[i] = new ArrayList <HashPairs<String, Integer>>();
			 distributionByBugMap[i] = new ArrayList <HashPairs<String, Integer>>();
			 changePaths[i] = new ArrayList <String>();
			 changePathCounts[i] = new ArrayList <Number>();
			 bugPaths[i] = new ArrayList <String>();
			 bugPathCounts[i] = new ArrayList <Number>();
			 masterMap[i] = new HashMap <String,SortableHashMap<Integer, Integer>>();
			 idList[i] = new ArrayList<Number> ();
			 titleList[i] = new ArrayList<String>();
		 }
	}
	
	public void defectDistributionSetup (File QueryFile, File outFile){
		 bugsQueryFile = QueryFile;
		 outputFile = outFile;
	}

	public void runAnalysis() throws IOException, TFSException{
		 excelAccess.openFiles(outputFile);
		 callBack.progressCallBack("CONNECTING TO TFS....");
		 TFSAccess tmp = new TFSAccess(bugsQueryFile, null, null, callBack);
		 bugWorkItems = tmp.getWorkItemCollection();
		 workItemClient = tmp.getWorkItemClient();
		 versionControlClient = tmp.getVersionControlClient();
		 callBack.progressCallBack("PROCESSING PROJECT BUG COLLECTION....");
		 Integer wid;
		 int querySize =  bugWorkItems.size();
		 WorkItem workItem;
		 int tmpCounter = 0;
		 for (int i = 0; i < querySize; i++) {
			 wid = bugWorkItems.getWorkItem(i).getID();
			 workItem = workItemClient.getWorkItemByID(wid);
		     getChangeSets(workItem, wid);
		     if (++tmpCounter % 50 == 0)  callBack.progressCallBack("Identified "+changeCounter+" changesets from "+tmpCounter+"/"+querySize+" bugs");
		 }
		 createFinalMaps();
         createSortedMapArrays();
         createSoftwareDomainVectors(distributionByPathMap, changePaths, changePathCounts);
         createSoftwareDomainVectors(distributionByBugMap, bugPaths, bugPathCounts);
         createBugListVectors();
         writeOutputToExcel();
         excelAccess.closeAll();
         callBack.progressCallBack("DONE: Identified "+changeCounter+" changesets from "+tmpCounter+"/"+querySize+" bugs");
         JOptionPane.showMessageDialog(null, "DONE: Identified "+changeCounter+" changesets from "+tmpCounter+"/"+querySize+" bugs");
	}
	
	private void getChangeSets(WorkItem workItem, Integer wid)
    {
        for (Link link : workItem.getLinks())
        {
            String linkType = link.getLinkType().getName();

            if (link instanceof ExternalLink && linkType.equalsIgnoreCase("Fixed in Changeset"))
            {
                ExternalLink externalLink = (ExternalLink) link;   
                String title = workItem.getTitle();
                bugTitles.put(wid,  title);
                Changeset changeset = versionControlClient.getChangeset(Integer.parseInt(getChangesetID(externalLink.getURI())));
                Change changes[] = changeset.getChanges();
                for (Change changeItem : changes) {
                  String changeName = changeItem.getItem().getServerItem();
                  populateDomainMaps (changeName, wid);
                }
                changeCounter++;
            }
        }
    }
	
	private String getChangesetID(String changeSetIdPath) {
		String tokens[] = changeSetIdPath.split("[/]");
		return tokens[tokens.length -1];
	}
	
	private void populateDomainMaps (String changedItem, Integer wid) {
		String tokens[] = changedItem.split("[/]");
		String concat = new String();
		int numberOfLevels = tokens.length;
		if (numberOfLevels > finalNumberOfLevels) finalNumberOfLevels = numberOfLevels;
		for (int i=1; i < numberOfLevels; i++) {		//start from level 1. Level 0 contains only "/$"
			SortableHashMap<Integer, Integer> bugCount;
			concat += ("/"+tokens[i]);				
			if (!masterMap[i-1].containsKey(concat)) bugCount = new SortableHashMap<Integer, Integer>(); 
			else bugCount = masterMap[i-1].get(concat);	
			bugCount.put(wid, 1);
			masterMap[i-1].put(concat, bugCount);		
		}
	}
	
	private void createFinalMaps() {
		for (int i=0; i < finalNumberOfLevels; i++) {
			HashMap <String, SortableHashMap<Integer, Integer>> tmpMap = masterMap[i];
			Iterator<Entry<String, SortableHashMap<Integer, Integer>>> itr = tmpMap.entrySet().iterator();
			while (itr.hasNext()) {
				Entry<String, SortableHashMap<Integer, Integer>> element = itr.next();
			  	String strVal = element.getKey();
			  	SortableHashMap<Integer, Integer> bugCount = element.getValue();
			  	distributionByPath[i].put(strVal, totalCount(bugCount)); 
			  	distributionByBug[i].put(strVal, bugCount.size());
			}
		}
	}
	
	private Integer totalCount (SortableHashMap<Integer, Integer> mapVal) {
		Integer sum = 0;
		for (Entry<Integer, Integer> elem : mapVal.entrySet()){
			sum += elem.getValue();
		}
		return (sum);
	}
	
	private void createSortedMapArrays() {
		SortableHashMap<String, Integer> tmpdistributionByPath, tmpdistributionByBug;
		for (int i=0; i < finalNumberOfLevels; i++) {
			tmpdistributionByPath = distributionByPath[i];
			tmpdistributionByBug = distributionByBug[i];
			distributionByPathMap[i] = tmpdistributionByPath.sortMapArray();
			distributionByBugMap[i] = tmpdistributionByBug.sortMapArray();
		}
	}
	
	private void createSoftwareDomainVectors(ArrayList <HashPairs<String, Integer>>[] map, ArrayList<String> paths[], ArrayList<Number> counts[]) {
		for (int i = 0; i < finalNumberOfLevels; i++) {
			for (HashPairs<String, Integer> hp : map[i]) {
				paths[i].add(hp.getPairkey());
				counts[i].add(hp.getPairValue());
			}
		}
	}
	
	private void createBugListVectors() {
		int limit;
		for (int i = 0; i < finalNumberOfLevels; i++) {
			limit = 0;
			ArrayList<String> levelPaths = bugPaths[i];
			for (int pathIndex=0; pathIndex < levelPaths.size(); pathIndex++) {
				if (limit >= MAX_BUG_LIST_SIZE) break;
				String path = levelPaths.get(pathIndex);
				SortableHashMap<Integer, Integer> masterMapElement = masterMap[i].get(path);
				for (Integer key : masterMapElement.keySet()) {
					idList[i].add(key);
					titleList[i].add(bugTitles.get(key));
					if (++limit == MAX_BUG_LIST_SIZE) break;
				}
			}
		}
	}
	
	private void writeOutputToExcel() {
		for (int i=0; i < finalNumberOfLevels; i++) {
			excelAccess.createSheet("Level "+i, i);
			excelAccess.addLabel (0, "ID", 1, i);
			excelAccess.addLabel (1, "Title", 1, i);
			excelAccess.addLabel (2, "Software Path", 1, i);
			excelAccess.addLabel (3, "Number of changes", 1, i);
			excelAccess.addLabel (5, "Software Path", 1, i);
			excelAccess.addLabel (6, "Number of bugs", 1, i);
			
			addVectorToFile (excelAccess, 0, idList[i], 0, i);
			addStringVectorToFile (excelAccess, 1, titleList[i],0,i);
			addStringVectorToFile (excelAccess, 2, changePaths[i],0,i);
		    addVectorToFile (excelAccess, 3, changePathCounts[i], 0, i);
		    addStringVectorToFile (excelAccess, 5, bugPaths[i],0,i);
		    addVectorToFile (excelAccess, 6, bugPathCounts[i], 0, i);
		}		
	}
	
   private void addVectorToFile (ExcelServices excelAccess, int col, ArrayList<Number> bugArray, int start, int sheetIndex ) {

		int bugArrayIndex=0, row = 2+start;
		int limit = bugArray.size();
		jxl.write.Number value = new jxl.write.Number (0,0,0);
		
		while (bugArrayIndex < limit) {
			value.setValue(bugArray.get(bugArrayIndex).floatValue());
			excelAccess.writeNumberCell (col, row, value, sheetIndex);
		    bugArrayIndex++;
   	    row++;
		  }
   }
	   
   private void addStringVectorToFile (ExcelServices excelAccess, int col, ArrayList<String> strArray, int start, int sheetIndex ) {

		int arrayIndex=0, row = 2+start;
		int limit = strArray.size();
		String str = new String();
		
		while (arrayIndex < limit) {
			str = strArray.get(arrayIndex);
			if (str == null) {
				str = " ";
			}
			excelAccess.addLabel (col, str, row, sheetIndex);
		    arrayIndex++;
   	    row++;
		  }
   }


}


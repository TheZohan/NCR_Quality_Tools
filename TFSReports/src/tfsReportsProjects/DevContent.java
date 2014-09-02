package tfsReportsProjects;

import java.util.ArrayList;
//import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.lang.Number;

import excelServices.ExcelServices;

import tfsAccess.WorkItemData;


public class DevContent {

	long globalBaseTime;
	private HashMap<Integer, Number> userStoryDateVectorFullDone = new HashMap<Integer, Number>();
	private ArrayList <Number> cumulativeUserStoryDone = new ArrayList <Number> ();
	private ArrayList <Number> defectDensity = new ArrayList <Number> ();
	long conversion = 7*24*3600*1000; // default is weekly
	private ProjectsReportGenerator rGen;
	private ExcelServices excelAccess;
	private List <WorkItemData> userStoryItemList;
	
	public DevContent (List <WorkItemData> userStoryList, ProjectsReportGenerator rGenRef, ExcelServices eAccess, long conv) {
		   conversion = conv;
		   rGen = rGenRef;
		   excelAccess = eAccess;
		   userStoryItemList = userStoryList;	   	   
	 }
	
	public void runDevContentReport() {
		   globalBaseTime = rGen.getGlobalBaseTime();
	   	   initDateVectors(userStoryItemList);
		   
	   	   if (userStoryDateVectorFullDone.size() != 0) {
		     createCumulativeVector(userStoryDateVectorFullDone, cumulativeUserStoryDone);
		   
		     extendUserStoryArray(rGen.getMaxWeekIndex());
		     extendSingleArray (rGen.getCumulativeExtBugs());
		   
		     createDefectDensityVector (rGen.getCumulativeExtBugs(), cumulativeUserStoryDone);
	   	     addVectorsToFile(excelAccess);
	   	   }
	   	userStoryDateVectorFullDone.clear();
	   	cumulativeUserStoryDone.clear();
	   	defectDensity.clear();
	   	userStoryItemList.clear();
	   	rGen = null;
	   	excelAccess = null;   	
	}
	
	private void addLabels(ExcelServices excelAccess){
		  excelAccess.addLabel (1, "Cumulative done story points", 1, 1);
		  excelAccess.addLabel (2, "Defect Density", 1, 1);
	}
	
	private void addVectorsToFile(ExcelServices excelAccess) {
		   addLabels(excelAccess);
	   	   addVectorToFile(1, cumulativeUserStoryDone, 0, excelAccess);
	   	   addVectorToFile(2, defectDensity, 0, excelAccess);
	}

	private void initDateVectors(List <WorkItemData> userStoryItemList) {
		Number sizeVal=null;
		String status;
	    
		for (WorkItemData wid: userStoryItemList) { 
			setWeekIndices(wid);
			status = wid.getState();
			sizeVal = wid.getSizeField().floatValue();
	    	
			if (status.equalsIgnoreCase("Done")) {
				if ( userStoryDateVectorFullDone.get(wid.getWeekIndexStateChange()) == null) userStoryDateVectorFullDone.put(wid.getWeekIndexStateChange(), sizeVal);
				else userStoryDateVectorFullDone.put(wid.getWeekIndexStateChange(), userStoryDateVectorFullDone.get(wid.getWeekIndexStateChange()).floatValue()+sizeVal.floatValue());
			}
		
	    }
		validateSingleMap (userStoryDateVectorFullDone);
		userStoryItemList.clear();
		userStoryItemList = null;
	}
	private void setWeekIndices (WorkItemData wid){
		long dateDiff;
		Integer temp;
		
		dateDiff = (wid.getStateChangeDate().getTime() - globalBaseTime)/ conversion; 
		if (dateDiff < 0) dateDiff = 0;
		temp = (Integer) (int) dateDiff;
		wid.setWeekIndexStatChange(temp);
	}

    private void validateSingleMap (HashMap<Integer, Number> map){ //make sure all map indices are populated
    	for (int i=0; i < map.size(); i++) // note: map size might change during loop
    	  if (map.get(i) == null) map.put(i, 0);		
    }
    
	private void createCumulativeVector (HashMap<Integer, Number> extBugs2, ArrayList<Number> cumulativeBugs) {
		float value, sum;
		int limit = extBugs2.size();
		cumulativeBugs.add(0, extBugs2.get(0).intValue());

		for (int i=1; i < limit; i++) {
			Number num1;
			value = extBugs2.get(i).floatValue();
			sum = (int) cumulativeBugs.get(i-1).floatValue() + value;
			num1 = sum;
			cumulativeBugs.add (i, num1);
		}
	};

	private void extendUserStoryArray(int limit) {
		int index = cumulativeUserStoryDone.size();
		Number element = cumulativeUserStoryDone.get(index-1);
		while (cumulativeUserStoryDone.size() < limit ) { 
			cumulativeUserStoryDone.add(element);
		}
	}
	
	private void extendSingleArray(ArrayList<Number> array) {
		int index = array.size();
		int limit = rGen.getMaxWeekIndex();
		Number element = array.get(index-1);
		while (array.size() < limit ) {
			array.add(element);
		}
	}

	 private void createDefectDensityVector (ArrayList <Number> bugs, ArrayList <Number> content) {
		int count = 0;
		for (Number cVal : content) {
			double valBugs=0, valContent=0;
			valBugs = bugs.get(count).doubleValue();
			valContent = cVal.doubleValue();
			if (valContent == 0) defectDensity.add(100);
			else defectDensity.add (valBugs / valContent);
			count++;
			if (count == rGen.getMaxWeekIndex()) break; 
		}
	 }
 
	 private void addVectorToFile (int col, ArrayList<Number> bugArray, int start, ExcelServices excelAccess ) {
	    	
   		int bugArrayIndex=0, row = 2+start;
   		int limit = bugArray.size();
   		jxl.write.Number value = new jxl.write.Number (0,0,0);
   		
   		while (bugArrayIndex < limit) {
   			value.setValue (bugArray.get(bugArrayIndex).floatValue());
   			excelAccess.writeNumberCell (col, row, value, 1);
   		    bugArrayIndex++;
       	    row++;
   		  }
   }
	   
}



package tfsReportsCore;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import excelServices.ExcelServices;

import tfsAccess.TFSAccess;
import tfsAccess.TFSException;
import tfsAccess.TFSProgressCallBack;
import tfsAccess.WorkItemData;
import tfsReports.SortableDate;
import tfsReports.TFSReportsGenerator;
import tfsReportsProjects.DevContent;
import tfsReportsProjects.ProjectsReportGenerator;

public class CoreReportGenerator extends ProjectsReportGenerator implements TFSReportsGenerator {

	private File allBugsQueryFile, escapingBugsQueryFile, userStoryQueryFile, output_file;
	
	private List <WorkItemData> bugItemList = new ArrayList<>();
	private List <WorkItemData> userStoryItemList = new ArrayList<>(); 
	
	protected long globalBaseTime;
	protected Date globalBaseDate = new Date();
	private long conversion = 7*24*3600*1000; // default is weekly
	private int maxWeekIndex = 0;
	private double medianAge=0;
	private TFSProgressCallBack callBack;
	private int querySize;
	
	private ArrayList<Number> cumulativeEscapingBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeCoreBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeNABs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeNRBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeFilteredBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeCoreRegressions = new ArrayList <Number>();
	private ArrayList<Number> openHistogram = new ArrayList <Number>();
    private ArrayList<Number> regressionsCorePercentage = new ArrayList <Number>();
    private ArrayList<Number> escapingDefectsPct = new ArrayList <Number>();
    private ArrayList<Number> NABPercentage = new ArrayList <Number>();
    private ArrayList<Number> NRPercentage = new ArrayList <Number>();
    
    
    private HashMap<Integer, Number> escapingBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugs = new HashMap<Integer, Number>();
    
    private HashMap<Integer, Number> coreBugsCritical = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugsMajor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugsMinor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugsModerate = new HashMap<Integer, Number>();
    
    private HashMap<Integer, Number> fixedcoreBugsCritical = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedcoreBugsMajor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedcoreBugsMinor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedcoreBugsModerate = new HashMap<Integer, Number>();
    
    private ArrayList<Number> openMinusFixedCoreCritical = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedCoreMajor = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedCoreMinor = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedCoreModerate = new ArrayList <Number>();
    
    private HashMap<Integer, Number> regressionCoreBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> openBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> notABug = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> notReproducedBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> filteredBugs = new HashMap<Integer, Number>();
   
	private ExcelServices excelAccess = new ExcelServices();
		
	public void ReportGenerator (File allBugsQuery, File escapingBugsQuery, File userStoryQuery, File output, TFSProgressCallBack callback) {
		   callBack = callback;
		   allBugsQueryFile =  allBugsQuery;
		   escapingBugsQueryFile = escapingBugsQuery;
		   userStoryQueryFile = userStoryQuery;
		   output_file = output;
	}
	
	public void runReports () throws  IOException, TFSException {
		   excelAccess.openFiles(output_file);
		   excelAccess.createSheet("Defect open vs close trends", 0);
		   excelAccess.createSheet("Defect Density & Regressions", 1);
		   excelAccess.createSheet("Defect Aging", 2);
		   excelAccess.createSheet("NAB & NR", 3);
		   callBack.progressCallBack("CONNECTING TO TFS....");
		   TFSAccess tmp = new TFSAccess(allBugsQueryFile, escapingBugsQueryFile, userStoryQueryFile, callBack);
		   bugItemList = tmp.getBugDataList();
		   Collections.sort(bugItemList);
		   callBack.progressCallBack("PROCESSING BUGS DATA....");

		   getGlobalbaseTimes();  // set globalBaseTime & globalBaseDate as first bug fixed
		   getBugDatesVectors();
		   		   
    	   processVectors();
		   
		   addVectorsToFile();
		   	   
		   if (userStoryQueryFile != null) {
			   callBack.progressCallBack("PROCESSING USER STORY DATA....");
			  userStoryItemList = tmp.getUSDataList();
		      DevContent devC = new DevContent (userStoryItemList, this, excelAccess, conversion);
		      devC.runDevContentReport();
		      devC = null;
		   }
		   excelAccess.closeAll();
		   callBack.progressCallBack("DONE: Processed "+querySize+"/"+querySize+" bugs");
	       JOptionPane.showMessageDialog(null, "DONE: Processed "+querySize+"/"+querySize+" bugs");	
	       tmp = null;
	}
	
	public long getGlobalBaseTime() {
		return this.globalBaseTime;
	}
	public Date getGlobalBaseDate() {
		return this.globalBaseDate;
	}
	public int getMaxWeekIndex() {
		return this.maxWeekIndex;
	}

	public ArrayList<Number> getCumulativeExtBugs() {// stupid trick in order to use generic devContent class - should be changed
		
		return this.cumulativeFilteredBugs;
	}

	private void processVectors() {
	 	createOpenMinusFixed(coreBugsCritical, fixedcoreBugsCritical, openMinusFixedCoreCritical);
	 	createOpenMinusFixed(coreBugsMajor, fixedcoreBugsMajor, openMinusFixedCoreMajor);
	 	createOpenMinusFixed(coreBugsMinor, fixedcoreBugsMinor, openMinusFixedCoreMinor);
	 	createOpenMinusFixed(coreBugsModerate, fixedcoreBugsModerate, openMinusFixedCoreModerate);
	 	
	 	if (coreBugs.size() != 0) createCumulativeVector(coreBugs, cumulativeCoreBugs);
	 	if (escapingBugs.size() != 0) createCumulativeVector (escapingBugs, cumulativeEscapingBugs);
	 	if (regressionCoreBugs.size() != 0) createCumulativeVector (regressionCoreBugs, cumulativeCoreRegressions);
	 	
	 	if (openMinusFixedCoreCritical.size() != 0) createCumulativeVector (openMinusFixedCoreCritical);
	 	if (openMinusFixedCoreMajor.size() != 0) createCumulativeVector (openMinusFixedCoreMajor);
	 	if (openMinusFixedCoreMinor.size() != 0) createCumulativeVector (openMinusFixedCoreMinor);
	 	if (openMinusFixedCoreModerate.size() != 0) createCumulativeVector (openMinusFixedCoreModerate);
	 	
	 	if (notABug.size() != 0) createCumulativeVector(notABug, cumulativeNABs);
	 	if (notReproducedBugs.size() != 0) createCumulativeVector(notReproducedBugs, cumulativeNRBugs);
	 	if (filteredBugs.size() != 0) createCumulativeVector(filteredBugs, cumulativeFilteredBugs);
	 	
	 	extendArrays();
	 	
    	createEscapingPctArray (cumulativeEscapingBugs, cumulativeFilteredBugs, escapingDefectsPct);
	 	createPctArray (cumulativeNABs, cumulativeCoreBugs, NABPercentage);
	 	createPctArray (cumulativeNRBugs, cumulativeCoreBugs, NRPercentage);
	 	
	 	if (openBugs.size() != 0) createOpenBugHistogram(openBugs, openHistogram);
	    if (cumulativeCoreRegressions.size() != 0) createRegressionsTrend(cumulativeCoreRegressions, cumulativeFilteredBugs, regressionsCorePercentage);
	    if (openBugs.size() != 0) medianAge = getMedianAge(openHistogram);
	}
		

	private void getBugDatesVectors() {
		Integer tmpWeekIndex;
		querySize =  bugItemList.size();
		
		for (WorkItemData wid: bugItemList) {
			setWeekIndices(wid);
			String state = wid.getState();
			String severity = wid.getSeverity();
			Integer createDate = wid.getWeekIndexCreate();
			String bugType = wid.getBugType();
			Number sizeVal = wid.getSizeField();
			String reason = wid.getReason();
			String qaResolution = wid.getQAResolution();
			boolean notABugFlag = isNotABug (state, reason, qaResolution);
			boolean notReproducedFlag = isNotReproduced (state, reason, qaResolution);
			boolean isFixedFlag = isFixed (state);
			//Calendar cal = Calendar.getInstance();
			//cal.set(2013, 10, 7);  // October 7 2013 (start date of regressions field)
			//Date testDate = cal.getTime();
			
			addMapElement (coreBugs,createDate, sizeVal);
			addMapElement (getMapBySeverity (severity, false), createDate, sizeVal); // fixed = false
			if ((notABugFlag == false) && (notReproducedFlag == false) ) addMapElement(filteredBugs,createDate, sizeVal);
			else {
			   if (notABugFlag == true) addMapElement (notABug, createDate, sizeVal);
			   if (notReproducedFlag == true) addMapElement (notReproducedBugs, createDate, sizeVal); 
			}
			
			if (wid.getIsEscapingDefect() == true && (notABugFlag == false) && (notReproducedFlag == false)) {
			   addMapElement (escapingBugs, createDate, sizeVal);
			}

			if ((state.equalsIgnoreCase("Open") || state.equalsIgnoreCase("In Progress"))) {
				addMapElement (openBugs, createDate, sizeVal);
			}
			
			if (bugType != null) {
				if (bugType.equalsIgnoreCase("Regression") && notABugFlag == false && notReproducedFlag == false && isFixedFlag == true) {
					addMapElement (regressionCoreBugs, createDate, sizeVal);
				}
			}
			
			if (isFixed (state)){
				if (wid.getResolvedDate() == null) tmpWeekIndex = wid.getWeekIndexStateChange();
				else tmpWeekIndex = wid.getWeekIndexResolved();
				addMapElement (getMapBySeverity (severity, true),tmpWeekIndex, sizeVal); // fixed = true 

			} 			

		}
		
 		validateMaps(); 
 		maxWeekIndex++;
	};
	
	private HashMap<Integer, Number> getMapBySeverity(String severity, boolean fixed) {
		HashMap<Integer, Number> map = null;
		
		switch (severity) {
		case "1 - Critical": 
			if (fixed == false) map = coreBugsCritical;
			else if (fixed == true) map = fixedcoreBugsCritical;
			break;
    	case "2 - Major": 
			if (fixed == false) map = coreBugsMajor;
			else if (fixed == true) map = fixedcoreBugsMajor;
   		break;
    	case "3 - Moderate": 
			if (fixed == false) map = coreBugsModerate;
			else if (fixed == true) map = fixedcoreBugsModerate;
    		break;
    	case "4 - Minor": 
			if (fixed == false) map = coreBugsMinor;
			else if (fixed == true) map = fixedcoreBugsMinor;
     		break;   	
		}

		return map;
	}

	
	private void setWeekIndices (WorkItemData wid){
		long dateDiff;
		Integer temp;
		
		dateDiff = (wid.getCreateDate().getTime() - globalBaseTime)/ conversion; 
		if (dateDiff < 0) dateDiff = 0;
		temp = (Integer) (int) dateDiff;
		if (temp > maxWeekIndex) maxWeekIndex = temp;
		wid.setWeekIndexCreate(temp);
			 
		dateDiff = (wid.getStateChangeDate().getTime() - globalBaseTime)/ conversion; 
		if (dateDiff < 0) dateDiff = 0;
		temp = (Integer) (int) dateDiff;
		if (temp > maxWeekIndex) maxWeekIndex = temp;
		wid.setWeekIndexStatChange(temp);
			 
		if (wid.getResolvedDate() != null) {
			dateDiff = (wid.getResolvedDate().getTime() - globalBaseTime)/ conversion;
			if (dateDiff < 0) dateDiff = 0;
		    temp = (Integer) (int) dateDiff;
		    if (temp > maxWeekIndex) maxWeekIndex = temp;
		    wid.setWeekIndexResolved(temp);
		}
	}

	private void validateMaps() {
		validateSingleMap (coreBugs);
		validateSingleMap (escapingBugs);
		validateSingleMap (openBugs);
		validateSingleMap (regressionCoreBugs);
		validateSingleMap (notABug);
		validateSingleMap (notReproducedBugs); 
		validateSingleMap (filteredBugs);
		
		validateSingleMap (coreBugsCritical);
		validateSingleMap (coreBugsMajor);
		validateSingleMap (coreBugsMinor);
		validateSingleMap (coreBugsModerate);
		
		validateSingleMap (fixedcoreBugsCritical);
		validateSingleMap (fixedcoreBugsMajor);
		validateSingleMap (fixedcoreBugsMinor);
		validateSingleMap (fixedcoreBugsModerate);
	}

	private void createEscapingPctArray (ArrayList<Number> escapingArray, ArrayList<Number> coreArray, ArrayList<Number> escapingPctArray) {
		int i = 0;
		for (Number val: escapingArray){
			Number pctVal = val.floatValue() / (coreArray.get(i).floatValue()) * 100.0;
			escapingPctArray.add(pctVal);
			i++;
		}
	}
       	private void addVectorsToFile() {
		 
		   addLabels();
		   addWeekColumn();
		   if (openMinusFixedCoreCritical.size() != 0) addVectorToFile (excelAccess, 1, openMinusFixedCoreCritical, 0, 0);
		   if (openMinusFixedCoreMajor.size() != 0) addVectorToFile (excelAccess, 2, openMinusFixedCoreMajor, 0, 0);
		   if (openMinusFixedCoreMinor.size() != 0) addVectorToFile (excelAccess, 3, openMinusFixedCoreMinor, 0, 0);
		   if (openMinusFixedCoreModerate.size() != 0) addVectorToFile (excelAccess, 4, openMinusFixedCoreModerate, 0, 0);
		   
		   if (openHistogram.size() != 0) addVectorToFile (excelAccess, 1, openHistogram, 0, 2);
		   if (regressionsCorePercentage.size() != 0) addVectorToFile (excelAccess, 4, regressionsCorePercentage, 0, 1);
		   if (escapingDefectsPct.size() != 0) addVectorToFile (excelAccess, 6, escapingDefectsPct, 0, 1);
		   if (NABPercentage.size() != 0) addVectorToFile (excelAccess, 1, NABPercentage, 0, 3);
		   if (NRPercentage.size() != 0) addVectorToFile (excelAccess, 2, NRPercentage, 0, 3);

		   jxl.write.Number value = new jxl.write.Number (0,0,medianAge);
		   excelAccess.writeNumberCell (4, 10, value,2);
		   
	}
	
	private void addLabels(){
		 excelAccess.addLabel (1, "Critical open-fixed Core bugs", 1, 0);
		 excelAccess.addLabel (2, "Major open-fixed Core bugs", 1, 0);
		 excelAccess.addLabel (3, "Minor open-fixed Core bugs", 1, 0);
		 excelAccess.addLabel (4, "Moderate open-fixed Core bugs", 1, 0);
		 
		 excelAccess.addLabel (1, "Number of open bugs", 1, 2);
		 excelAccess.addLabel (3, "Median Age", 10, 2);
		 excelAccess.addLabel(4, "Core Regression %", 1, 1);
		 excelAccess.addLabel (6, "Escaping Defects", 1, 1);
		 excelAccess.addLabel (1, "Not a Bug %", 1, 3);
		 excelAccess.addLabel (2, "Not Reproduced %", 1, 3);
	}

	private void addWeekColumn() {
		excelAccess.addLabel (0, "Week starting", 1, 0);
		excelAccess.addLabel (0, "Week starting", 1, 1);
		excelAccess.addLabel (0, "Week starting", 1, 3);
		excelAccess.addLabel(0, "Age in weeks", 1, 2);
		int weekArrayIndex=1, row = 2;
		int limit = coreBugs.size();
		final long weekDelta = conversion;
		long time;
		SortableDate date = new SortableDate();
		date.setDate(new Date(globalBaseTime));
		while (weekArrayIndex <= limit) {
			jxl.write.Number num = new jxl.write.Number(0,0, weekArrayIndex-1);
			excelAccess.writeDateCell (0, row, date, 0);
			excelAccess.writeDateCell (0, row, date, 1);
			excelAccess.writeDateCell (0, row, date, 3);
			excelAccess.writeNumberCell(0, row, num, 2);
 	        row++;
 	        time = globalBaseTime + weekArrayIndex * weekDelta;
 	        Date d = new Date(time);
 	        date.setDate (d);
 	        weekArrayIndex++;
	     }
	}
	
	private void extendArrays(){
		extendSingleArray (cumulativeCoreBugs);
		extendSingleArray (openMinusFixedCoreCritical);
		extendSingleArray (openMinusFixedCoreMajor);
		extendSingleArray (openMinusFixedCoreMinor);
		extendSingleArray (openMinusFixedCoreModerate);
		extendSingleArray (cumulativeFilteredBugs);
		extendSingleArray (cumulativeNABs);
		extendSingleArray (cumulativeNRBugs);
		extendSingleArray (cumulativeCoreRegressions);
	}
	private void extendSingleArray(ArrayList<Number> array) {
		int index = array.size();
		if (index == 0) return;
		Number element = array.get(index-1);
		while (array.size() < maxWeekIndex ) {
			array.add(element);
		}
	}
	
	
    private void getGlobalbaseTimes() {
    	globalBaseTime = bugItemList.get(0).getStateChangeDate().getTime();
    	for (WorkItemData wid : bugItemList) {
    		String state = wid.getState();
    		long tmp;
    		Date tmpDate;
    		if (isFixed (state) && (wid.getHasCoreLink() != true)){
    			if (wid.getResolvedDate() != null) {
    				tmp = wid.getResolvedDate().getTime();
    				tmpDate = wid.getResolvedDate();
    			}
    			else{
    				tmp = wid.getStateChangeDate().getTime();
    				tmpDate = wid.getStateChangeDate();
    			}
    			if (tmp < globalBaseTime){
    				globalBaseTime = tmp;
    				globalBaseDate = tmpDate;
    			}
    		}
    	}
    }
}




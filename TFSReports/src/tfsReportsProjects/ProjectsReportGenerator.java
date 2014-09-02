package tfsReportsProjects;

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
import tfsAccess.TFSProgressCallBack;
import tfsAccess.WorkItemData;
import tfsReports.SortableDate;
import tfsReports.TFSReportsGenerator;

public class ProjectsReportGenerator implements TFSReportsGenerator {

	private File allBugsQueryFile, coreBugsQueryFile, userStoryQueryFile, output_file;	
	private List <WorkItemData> bugItemList = new ArrayList<>();
	private List <WorkItemData> userStoryItemList = new ArrayList<>(); 
	
	protected long globalBaseTime;
	protected Date globalBaseDate = new Date();
	private long conversion = 7*24*3600*1000; // default is weekly
	private int maxWeekIndex = 0;
	private double medianAge=0;
	private TFSProgressCallBack callBack;
	private int querySize;
	
	private ArrayList<Number> cumulativeExtBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeNABExtBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeNRExtBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeFilteredExtBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeCoreBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeNABCoreBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeNRCoreBugs = new ArrayList <Number>();
	private ArrayList<Number> cumulativeFilteredCoreBugs = new ArrayList <Number>();
	private ArrayList<Number> corePctArray = new ArrayList <Number>();

	private ArrayList<Number> cumulativeCoreRegressions = new ArrayList <Number>();
	private ArrayList<Number> cumulativeExtRegressions = new ArrayList<Number>();

	private ArrayList<Number> openHistogram = new ArrayList <Number>();
    private ArrayList<Number> regressionsCorePercentage = new ArrayList <Number>();
    private ArrayList<Number> regressionsExtPercentage = new ArrayList <Number>();
    private ArrayList<Number> extNABPercentage = new ArrayList <Number>();
    private ArrayList<Number> extNRPercentage = new ArrayList <Number>();
    private ArrayList<Number> coreNABPercentage = new ArrayList <Number>();
    private ArrayList<Number> coreNRPercentage = new ArrayList <Number>();
    
    private HashMap<Integer, Number> extBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugs = new HashMap<Integer, Number>();
    
    private HashMap<Integer, Number> extBugsCritical = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugsCritical = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> extBugsMajor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugsMajor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> extBugsMinor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugsMinor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> extBugsModerate = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> coreBugsModerate = new HashMap<Integer, Number>();
    
    private HashMap<Integer, Number> fixedextBugsCritical = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedcoreBugsCritical = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedextBugsMajor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedcoreBugsMajor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedextBugsMinor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedcoreBugsMinor = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedextBugsModerate = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> fixedcoreBugsModerate = new HashMap<Integer, Number>();
    
	private ArrayList<Number> openMinusFixedExtCritical = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedExtMajor = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedExtMinor = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedExtModerate = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedCoreCritical = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedCoreMajor = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedCoreMinor = new ArrayList <Number>();
	private ArrayList<Number> openMinusFixedCoreModerate = new ArrayList <Number>();
	
    private HashMap<Integer, Number> regressionCoreBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> regressionExtBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> openBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> notABugCoreBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> notABugExtBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> notReproducedCoreBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> notReproducedExtBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> filteredCoreBugs = new HashMap<Integer, Number>();
    private HashMap<Integer, Number> filteredExtBugs = new HashMap<Integer, Number>();
    
	private ExcelServices excelAccess = new ExcelServices();
		
	public void ReportGenerator (File allBugsQuery, File coreBugsQuery, File userStoryQuery, File output, TFSProgressCallBack callback) {
		   callBack = callback;
		   allBugsQueryFile =  allBugsQuery;
		   coreBugsQueryFile = coreBugsQuery;
		   userStoryQueryFile = userStoryQuery;
		   output_file = output;
	}
	
	public void runReports () throws IOException, tfsAccess.TFSException {
		   excelAccess.openFiles(output_file);
		   excelAccess.createSheet("Defect open vs close trends", 0);
		   excelAccess.createSheet("Defect Density & Regressions", 1);
		   excelAccess.createSheet("Defect Aging", 2);
		   excelAccess.createSheet("NAB & NR", 3);
		   callBack.progressCallBack("CONNECTING TO TFS....");
		   TFSAccess tmp=null;;
		   tmp = new TFSAccess(allBugsQueryFile, coreBugsQueryFile, userStoryQueryFile, callBack);
		   bugItemList = tmp.getBugDataList();
		   Collections.sort(bugItemList);
		   callBack.progressCallBack("PROCESSING BUGS DATA....");
		   getGlobalbaseTimes();  // set globalBaseTime & globalBaseDate as first ext bug fixed
		   getBugDatesVectors();
    	   processVectors();
		   
		   addVectorsToFile();
		   	   
		   if (userStoryQueryFile != null) {
			   callBack.progressCallBack("PROCESSING USER STORY DATA....");
			  userStoryItemList = tmp.getUSDataList();
		      DevContent devC = new DevContent (userStoryItemList, this, excelAccess, conversion);
		      devC.runDevContentReport();
		      devC = null;
		      userStoryItemList.clear();
		      userStoryItemList = null;
		   }
		   excelAccess.closeAll();
		   callBack.progressCallBack("DONE: Processed "+querySize+"/"+querySize+" bugs");
	       JOptionPane.showMessageDialog(null, "DONE: Processed "+querySize+"/"+querySize+" bugs");		
	       
	       tmp = null;
	       excelAccess = null;
	}
	
	public long getGlobalBaseTime() {
		return this.globalBaseTime;
	}
	public Date getGlobalBaseDate() {
		return this.globalBaseDate;
	}
	public ArrayList<Number> getCumulativeExtBugs() {
		return this.cumulativeFilteredExtBugs;
	}
	public ArrayList<Number> getCumulativeCoreBugs() {
		return this.cumulativeFilteredCoreBugs;
	}
	public int getMaxWeekIndex() {
		return this.maxWeekIndex;
	}
	
	private void processVectors() {
	 	createOpenMinusFixed(extBugsCritical, fixedextBugsCritical, openMinusFixedExtCritical);
	 	createOpenMinusFixed(extBugsMajor, fixedextBugsMajor, openMinusFixedExtMajor);
	 	createOpenMinusFixed(extBugsMinor, fixedextBugsMinor, openMinusFixedExtMinor);
	 	createOpenMinusFixed(extBugsModerate, fixedextBugsModerate, openMinusFixedExtModerate);
	 	
	 	createOpenMinusFixed(coreBugsCritical, fixedcoreBugsCritical, openMinusFixedCoreCritical);
	 	createOpenMinusFixed(coreBugsMajor, fixedcoreBugsMajor, openMinusFixedCoreMajor);
	 	createOpenMinusFixed(coreBugsMinor, fixedcoreBugsMinor, openMinusFixedCoreMinor);
	 	createOpenMinusFixed(coreBugsModerate, fixedcoreBugsModerate, openMinusFixedCoreModerate);
	 	
	 		 	
	 	if (extBugs.size() != 0) createCumulativeVector(extBugs, cumulativeExtBugs);
	 	if (coreBugs.size() != 0) createCumulativeVector(coreBugs, cumulativeCoreBugs);
	 	
	 	if (notABugCoreBugs.size() != 0) createCumulativeVector(notABugCoreBugs, cumulativeNABCoreBugs);
	 	if (notABugExtBugs.size() != 0) createCumulativeVector(notABugExtBugs, cumulativeNABExtBugs);
	 	if (notReproducedCoreBugs.size() != 0) createCumulativeVector(notReproducedCoreBugs, cumulativeNRCoreBugs);
	 	if (notReproducedExtBugs.size() != 0) createCumulativeVector(notReproducedExtBugs, cumulativeNRExtBugs);
	 	if (filteredCoreBugs.size() != 0) createCumulativeVector(filteredCoreBugs, cumulativeFilteredCoreBugs);
	 	if (filteredExtBugs.size() != 0) createCumulativeVector(filteredExtBugs, cumulativeFilteredExtBugs);

	 	if (regressionCoreBugs.size() != 0) createCumulativeVector (regressionCoreBugs, cumulativeCoreRegressions);
	 	if (regressionExtBugs.size() != 0) createCumulativeVector (regressionExtBugs, cumulativeExtRegressions);
	 	
	 	if (openMinusFixedExtCritical.size() != 0) createCumulativeVector (openMinusFixedExtCritical);
	 	if (openMinusFixedExtMajor.size() != 0) createCumulativeVector (openMinusFixedExtMajor);
	 	if (openMinusFixedExtMinor.size() != 0) createCumulativeVector (openMinusFixedExtMinor);
	 	if (openMinusFixedExtModerate.size() != 0) createCumulativeVector (openMinusFixedExtModerate);
	 	
	 	if (openMinusFixedCoreCritical.size() != 0) createCumulativeVector (openMinusFixedCoreCritical);
	 	if (openMinusFixedCoreMajor.size() != 0) createCumulativeVector (openMinusFixedCoreMajor);
	 	if (openMinusFixedCoreMinor.size() != 0) createCumulativeVector (openMinusFixedCoreMinor);
	 	if (openMinusFixedCoreModerate.size() != 0) createCumulativeVector (openMinusFixedCoreModerate);


	 	extendArrays ();
	    createCorePctArray (cumulativeFilteredExtBugs, cumulativeFilteredCoreBugs, corePctArray);
	 	createPctArray (cumulativeNABCoreBugs, cumulativeCoreBugs, coreNABPercentage);
	 	createPctArray (cumulativeNABExtBugs, cumulativeExtBugs, extNABPercentage);
	 	createPctArray (cumulativeNRCoreBugs, cumulativeCoreBugs, coreNRPercentage);
	 	createPctArray (cumulativeNRExtBugs, cumulativeExtBugs, extNRPercentage);
	
	 	if (openBugs.size() != 0) createOpenBugHistogram(openBugs, openHistogram);
	    if (cumulativeCoreRegressions.size() != 0) createRegressionsTrend(cumulativeCoreRegressions, cumulativeFilteredCoreBugs, regressionsCorePercentage);
	    if (cumulativeExtRegressions.size() != 0)createRegressionsTrend(cumulativeExtRegressions, cumulativeFilteredExtBugs, regressionsExtPercentage);
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
			boolean hasCoreLink = wid.getHasCoreLink();
			//Calendar cal = Calendar.getInstance();
			//cal.set(2013, 10, 7);  // October 7 2013 (start date of regressions field)
			//Date testDate = cal.getTime();

			if (hasCoreLink == true) {
				addMapElement (coreBugs, createDate, sizeVal);
				addMapElement (getMapBySeverity (severity, true, false), createDate, sizeVal); // core = true, fixed = false
				if ((notABugFlag == false) && (notReproducedFlag == false)) addMapElement(filteredCoreBugs,createDate, sizeVal);
				else {
				   if (notABugFlag == true) addMapElement (notABugCoreBugs, createDate, sizeVal);
				   if (notReproducedFlag == true) addMapElement (notReproducedCoreBugs, createDate, sizeVal); 
				}
			}
			else {
				addMapElement (extBugs,createDate, sizeVal);
				addMapElement (getMapBySeverity (severity, false, false), createDate, sizeVal); // core = false, fixed = false
				if ((notABugFlag == false) && (notReproducedFlag == false)) addMapElement(filteredExtBugs,createDate, sizeVal);
				else {
				   if (notABugFlag == true) addMapElement (notABugExtBugs, createDate, sizeVal);
				   if (notReproducedFlag == true) addMapElement (notReproducedExtBugs, createDate, sizeVal); 
				}
			}
			
			if (hasCoreLink == false && (state.equalsIgnoreCase("Open") || state.equalsIgnoreCase("In Progress"))) {
			   addMapElement (openBugs, createDate, sizeVal);
			}
			if (bugType != null) {
				if (bugType.equalsIgnoreCase("Regression") && notABugFlag == false && notReproducedFlag == false && isFixed(state) == true) { 
					if (hasCoreLink == false) addMapElement (regressionExtBugs, createDate, sizeVal);
					else addMapElement (regressionCoreBugs, createDate, sizeVal);
				}
			}
			
			if (isFixed (state) == true){  // Don't care about NAB's & NR's for resolved bugs. We count them all
				if (wid.getResolvedDate() == null) tmpWeekIndex = wid.getWeekIndexStateChange();
				else tmpWeekIndex = wid.getWeekIndexResolved();
				
				if (hasCoreLink == true){
					addMapElement (getMapBySeverity (severity, true, true),tmpWeekIndex, sizeVal); // core = true, fixed = true 
				}
				else{
					addMapElement (getMapBySeverity (severity, false, true),tmpWeekIndex, sizeVal); // core = false, fixed = true 
				}
			}
		}
		
 		validateMaps(); 
 		bugItemList.clear();
 		bugItemList = null;
 		maxWeekIndex++;
	};
	
	private HashMap<Integer, Number> getMapBySeverity(String severity, boolean core, boolean fixed) {
		HashMap<Integer, Number> map = null;
		
		switch (severity) {
		case "1 - Critical": 
			if (core == true && fixed == false) map = coreBugsCritical;
			else if (core == false && fixed == false) map = extBugsCritical;
			else if (core == true && fixed == true) map = fixedcoreBugsCritical;
			else if (core == false && fixed == true) map = fixedextBugsCritical;
			break;
    	case "2 - Major": 
			if (core == true && fixed == false) map = coreBugsMajor;
			else if (core == false && fixed == false) map = extBugsMajor;
			else if (core == true && fixed == true) map = fixedcoreBugsMajor;
			else if (core == false && fixed == true) map = fixedextBugsMajor;
    		break;
    	case "3 - Moderate": 
			if (core == true && fixed == false) map = coreBugsModerate;
			else if (core == false && fixed == false) map = extBugsModerate;
			else if (core == true && fixed == true) map = fixedcoreBugsModerate;
			else if (core == false && fixed == true) map = fixedextBugsModerate;    		
    		break;
    	case "4 - Minor": 
			if (core == true && fixed == false) map = coreBugsMinor;
			else if (core == false && fixed == false) map = extBugsMinor;
			else if (core == true && fixed == true) map = fixedcoreBugsMinor;
			else if (core == false && fixed == true) map = fixedextBugsMinor;
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
		validateSingleMap (extBugs);
		validateSingleMap (openBugs);
		validateSingleMap (regressionCoreBugs);
		validateSingleMap (regressionExtBugs);
		validateSingleMap (notABugCoreBugs);
		validateSingleMap (notABugExtBugs);
		validateSingleMap (notReproducedCoreBugs); 
		validateSingleMap (notReproducedExtBugs);
		validateSingleMap (filteredCoreBugs);
		validateSingleMap (filteredExtBugs);
		
		validateSingleMap (extBugsCritical);
		validateSingleMap (coreBugsCritical);
		validateSingleMap (extBugsMajor);
		validateSingleMap (coreBugsMajor);
		validateSingleMap (extBugsMinor);
		validateSingleMap (coreBugsMinor);
		validateSingleMap (extBugsModerate);
		validateSingleMap (coreBugsModerate);
		
		validateSingleMap (fixedextBugsCritical);
		validateSingleMap (fixedcoreBugsCritical);
		validateSingleMap (fixedextBugsMajor);
		validateSingleMap (fixedcoreBugsMajor);
		validateSingleMap (fixedextBugsMinor);
		validateSingleMap (fixedcoreBugsMinor);
		validateSingleMap (fixedextBugsModerate);
		validateSingleMap (fixedcoreBugsModerate);
	}
    protected void validateSingleMap (HashMap<Integer, Number> map){ //make sure all map indices are populated
    	for (int i=0; i < map.size(); i++) // note: map size might change during loop
    	  if (map.get(i) == null) map.put(i, 0);		
    }
    
    protected void addMapElement (HashMap<Integer, Number> map, Integer key, Number sizeVal)
    {
        if (map != null){
        	if ( map.get(key) == null) map.put(key, 1);
			else map.put(key, map.get(key).floatValue()+sizeVal.floatValue());
    	}
    }
    protected boolean isNotABug(String state, String reason, String qaResolution) {
    	boolean retval = false;
    	if (state == null) state = "NA";
    	if (reason == null) reason = "NA";
    	if (qaResolution == null) qaResolution = "NA";
    	if ((state.equalsIgnoreCase("Closed") && reason.equalsIgnoreCase("Not a Bug")) || (state.equalsIgnoreCase("Not a Bug"))) retval = true;
    	return retval;
    }
    
    protected boolean isNotReproduced(String state, String reason, String qaResolution) {
    	boolean retval = false;
    	if (state == null) state = "NA";
    	if (reason == null) reason = "NA";
    	if (qaResolution == null) qaResolution = "NA";
    	
    	if ((state.equalsIgnoreCase("Closed") && reason.equalsIgnoreCase("Not Reproduced")) || (state.equalsIgnoreCase("Not Reproduced"))) retval = true;
    	return retval;
    }
    protected boolean isFixed(String state) {
    	boolean retval = false;
    	if (state == null) state = "NA"; 
        if (state.equalsIgnoreCase("Closed") || state.equalsIgnoreCase("Passed QA") || state.equalsIgnoreCase("Fixed") || 
    			state.equalsIgnoreCase("Pending QA") || state.equalsIgnoreCase("Not Reproduced") || state.equalsIgnoreCase("Not a Bug")) retval = true;
  
    	return retval;
    }

    protected void createCumulativeVector (HashMap<Integer, Number> extBugs2, ArrayList<Number> cumulativeBugs) {
		int value, sum;
		int limit = extBugs2.size();
		cumulativeBugs.add(0, extBugs2.get(0).intValue());

		for (int i=1; i < limit; i++) {
			Number num1;
			value = (int) extBugs2.get(i).intValue();
			sum = (int) cumulativeBugs.get(i-1).intValue() + value;
			num1 = sum;
			cumulativeBugs.add (i, num1);
		}
	};

	protected void createCumulativeVector(ArrayList<Number> openMinusFixed) {
		int value, sum;
		int limit = openMinusFixed.size();

		for (int i=1; i < limit; i++) {
			Number num1;
			value = (int) openMinusFixed.get(i).intValue();
			sum = (int) openMinusFixed.get(i-1).intValue() + value;
			num1 = sum;
			openMinusFixed.set (i, num1);
		}
		
	}

	protected void createOpenBugHistogram(HashMap<Integer, Number> openBugs, ArrayList<Number> openHistogram){
		ArrayList <Number> temp = new ArrayList <Number>();
		int limit = openBugs.size();
		for (int i = 0; i < limit; i++) {
			temp.add(openBugs.get(i).intValue());
		}
		limit = temp.size()-1;
		for (int i = 0; i <= limit; i++) {
			openHistogram.add(i, temp.get(limit-i));
		}		
	}
	
	protected void createRegressionsTrend(ArrayList <Number> regBugs, ArrayList <Number> allBugs, ArrayList <Number> regPct) { 
		                                            //cumulative regression bugs / cumulative extensions bugs
		int limit = allBugs.size();
		if (limit > regBugs.size()) limit = regBugs.size();
		
		for (int i=0; i< limit; i++){
			Number num = regBugs.get(i).floatValue() / allBugs.get(i).floatValue() * (float) 100.0;
			regPct.add(num);
		}
	}
	
	protected void createOpenMinusFixed (HashMap<Integer, Number> extBugs2, HashMap<Integer, Number> fixedExtBugs2, ArrayList<Number> openMinusClosed) {
		syncArrays (extBugs2, fixedExtBugs2);
		int limit = extBugs2.size();
		int value;
		for (int i=0; i <limit; i++) {
			Number num1;
			value = (int) (extBugs2.get(i).intValue() - fixedExtBugs2.get(i).intValue());
			num1 = value;
			openMinusClosed.add(num1);
		}		
	}
	protected void syncArrays(HashMap<Integer, Number> extBugs2, HashMap<Integer, Number> fixedExtBugs2){
		Number element = 0;
		if (extBugs2.size() > fixedExtBugs2.size()) {
			int index = fixedExtBugs2.size();
			while (fixedExtBugs2.size() < extBugs2.size() ) {
				addMapElement(fixedExtBugs2, index++, element);
			}
		}
		else if (extBugs2.size() < fixedExtBugs2.size()) {
			int index = extBugs2.size();
			while (extBugs2.size() < fixedExtBugs2.size() ) {
				addMapElement(extBugs2, index++, element);
			}
			
		}
		
	}
	private void createCorePctArray (ArrayList<Number> extArray, ArrayList<Number> coreArray, ArrayList<Number> corePctArray) {
		int i = 0;
		int maxIndex = extArray.size();
		for (Number val: coreArray){
			Number pctVal = val.floatValue() / (val.floatValue() + extArray.get(i).floatValue()) * 100.0;
			corePctArray.add(pctVal);
			i++;
			if (i == maxIndex) break;
		}
	}
	protected void createPctArray (ArrayList<Number> targetArray, ArrayList<Number> baseArray, ArrayList<Number> pctArray) {
		int i = 0;
		int maxIndex = baseArray.size();
		for (Number val: targetArray){
			Number pctVal = 0.0;
			float base = baseArray.get(i).floatValue();
			if (base == 0.0) pctVal = 0.0;
			else pctVal = val.floatValue() / base * 100.0;
			pctArray.add(pctVal);
			if (++i == maxIndex) break;
		}
	}
	protected double getMedianAge (ArrayList<Number> openArray) {
		int testSum = sumArray(openArray)/2;
    	int sum = 0;
    	int saveSum = 0;
    	double median = 0;
    	int index=0;
    	
    	while (sum <= testSum){
    		saveSum = sum;
    		sum += openArray.get(index++).intValue();
    	}
    	index -= 1;// count starts from 1, so it's index-1 (increment) -1 for index of saveSum and + 1 for count starting at 1
    	median = (double) index + (double) (testSum - saveSum) / (double) (sum - saveSum);
    	
    	return median;	
	}	
	private int sumArray(ArrayList<Number> array) {
		int sum = 0;
		for (Number element: array) sum += element.intValue();
		return sum;
	}

       	private void addVectorsToFile() {
		 
		   addLabels();
		   addWeekColumn();
		   
		   if (openMinusFixedExtCritical.size() != 0) addVectorToFile (excelAccess, 1, openMinusFixedExtCritical, 0, 0);
		   if (openMinusFixedExtMajor.size() != 0) addVectorToFile (excelAccess, 2, openMinusFixedExtMajor, 0, 0);
		   if (openMinusFixedExtMinor.size() != 0) addVectorToFile (excelAccess, 3, openMinusFixedExtMinor, 0, 0);
		   if (openMinusFixedExtModerate.size() != 0) addVectorToFile (excelAccess, 4, openMinusFixedExtModerate, 0, 0);
		  
		   if (openMinusFixedCoreCritical.size() != 0) addVectorToFile (excelAccess, 5, openMinusFixedCoreCritical, 0, 0);
		   if (openMinusFixedCoreMajor.size() != 0) addVectorToFile (excelAccess, 6, openMinusFixedCoreMajor, 0, 0);
		   if (openMinusFixedCoreMinor.size() != 0) addVectorToFile (excelAccess, 7, openMinusFixedCoreMinor, 0, 0);
		   if (openMinusFixedCoreModerate.size() != 0) addVectorToFile (excelAccess, 8, openMinusFixedCoreModerate, 0, 0);
	   
		   if (openHistogram.size() != 0) addVectorToFile (excelAccess, 1, openHistogram, 0, 2);
		   if (regressionsExtPercentage.size() != 0) addVectorToFile (excelAccess, 4, regressionsExtPercentage, 0, 1);
		   if (regressionsCorePercentage.size() != 0) addVectorToFile (excelAccess, 5, regressionsCorePercentage, 0, 1);
		   if (corePctArray.size() != 0) addVectorToFile (excelAccess, 7, corePctArray, 0, 1);
		   if (extNABPercentage.size() != 0) addVectorToFile (excelAccess, 1, extNABPercentage, 0, 3);
		   if (extNRPercentage.size() != 0) addVectorToFile (excelAccess, 2, extNRPercentage, 0, 3);
		   if (coreNABPercentage.size() != 0) addVectorToFile (excelAccess, 3, coreNABPercentage, 0, 3);
		   if (coreNRPercentage.size() != 0) addVectorToFile (excelAccess, 4, coreNRPercentage, 0, 3);

		   jxl.write.Number value = new jxl.write.Number (0,0,medianAge);
		   excelAccess.writeNumberCell (4, 10, value,2);
		   
	}
	
	private void addLabels(){
		 excelAccess.addLabel (1, "Critical open-fixed Extensions bugs", 1, 0);
		 excelAccess.addLabel (2, "Major open-fixed Extensions bugs", 1, 0);
		 excelAccess.addLabel (3, "Minor open-fixed Extensions bugs", 1, 0);
		 excelAccess.addLabel (4, "Moderate open-fixed Extensions bugs", 1, 0);
		 excelAccess.addLabel (5, "Critical open-fixed Core bugs", 1, 0);
		 excelAccess.addLabel (6, "Major open-fixed Core bugs", 1, 0);
		 excelAccess.addLabel (7, "Minor open-fixed Core bugs", 1, 0);
		 excelAccess.addLabel (8, "Moderate open-fixed Core bugs", 1, 0);
		 excelAccess.addLabel (1, "Number of open bugs", 1, 2);
		 excelAccess.addLabel (3, "Median Age", 10, 2);
		 excelAccess.addLabel(4, "Ext Regression %", 1, 1);
		 excelAccess.addLabel(5, "Core Regression %", 1, 1);
		 excelAccess.addLabel (7, "Core Percentage of total", 1, 1);
		 excelAccess.addLabel (1, "Extensions NAB %", 1, 3);
		 excelAccess.addLabel (2, "Extensions NR %", 1, 3);
		 excelAccess.addLabel (3, "Core NAB %", 1, 3);
		 excelAccess.addLabel (4, "Core NR %", 1, 3);
	}	
    protected void addVectorToFile (ExcelServices excelAccess, int col, ArrayList<Number> bugArray, int start, int sheetIndex ) {

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
    
    protected void addVectorToFile (ExcelServices excelAccess, int col, HashMap<Integer,Number> bugArray, int start, int sheetIndex ) {

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
	
	private void addWeekColumn() {
		excelAccess.addLabel (0, "Week starting", 1, 0);
		excelAccess.addLabel (0, "Week starting", 1, 1);
		excelAccess.addLabel(0, "Age in weeks", 1, 2);
		excelAccess.addLabel (0, "Week starting", 1, 3);
		int weekArrayIndex=1, row = 2;
		int limit = maxWeekIndex;
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
		extendSingleArray (cumulativeExtBugs);
		extendSingleArray (cumulativeCoreBugs);
		extendSingleArray (cumulativeCoreRegressions);
		extendSingleArray (cumulativeExtRegressions);
		extendSingleArray (openMinusFixedExtCritical);
		extendSingleArray (openMinusFixedExtMajor);
		extendSingleArray (openMinusFixedExtMinor);
		extendSingleArray (openMinusFixedExtModerate);
		extendSingleArray (openMinusFixedCoreCritical);
		extendSingleArray (openMinusFixedCoreMajor);
		extendSingleArray (openMinusFixedCoreMinor);
		extendSingleArray (openMinusFixedCoreModerate);
		
		extendSingleArray (cumulativeFilteredExtBugs);
		extendSingleArray (cumulativeFilteredCoreBugs);
		extendSingleArray (cumulativeNABCoreBugs);
		extendSingleArray (cumulativeNABExtBugs);
		extendSingleArray (cumulativeNRCoreBugs);
		extendSingleArray (cumulativeNRExtBugs);
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

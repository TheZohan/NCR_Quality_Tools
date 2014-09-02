package tfsAccess;

import java.util.Date;

public class WorkItemData implements  Comparable <WorkItemData> {
	private Integer id;
	private String state;
	private String bugType;
	private String reason;
	private String qaResolution;
	private String severity;
	private Date stateChangeDate;
	private Date CreateDate;
	private Date ResolvedDate;
	private String SWDomain;
	private Number readyEstimate;
	private Number originalEstimate;
	private Number sizeVal;
	private Integer weekIndexCreate;
	private Integer weekIndexStateChange;
	private Integer weekIndexResolved;
	private boolean hasCoreLink;
	private boolean isEscapingDefect;
    
    public Integer getId (){
    	return this.id;
    }
    public void setId(Integer id) {
    	this.id = id;
    }
    public String getState() {
    	return this.state;
    }
    public void setState (String state) {
    	this.state = state;
    }
    public String getReason () {
    	return this.reason;
    }
    public void setReason (String reason) {
    	this.reason = reason;
    }
    public String getQAResolution () {
    	return this.qaResolution;
    }
    public void setQAResolution (String qaResolution) {
    	this.qaResolution = qaResolution;
    }
    public String getBugType (){
    	return this.bugType;
    }
    public void setBugType (String bt) {
    	this.bugType = bt;
    }
    public String getSWDomain (){
    	return this.SWDomain;
    }
    public void setSWDomain (String swd) {
    	this.SWDomain = swd;
    }
    public boolean getHasCoreLink() {
    	return this.hasCoreLink;
    }
    public void setHasCoreLink (boolean bool) {
    	this.hasCoreLink = bool;
    }
    public boolean getIsEscapingDefect() {
    	return this.isEscapingDefect;
    }
    public void setIsEscapingDefect (boolean bool) {
    	this.isEscapingDefect = bool;
    }
	public Date getCreateDate(){
    	return this.CreateDate;
    }
    public void setCreateDate(Date date) {
  	  this.CreateDate = date;
  	}
    public Date getStateChangeDate(){
    	return this.stateChangeDate;
    }
    public void setStateChangeDate(Date date) {
  	  this.stateChangeDate = date;
  	}
    public Date getResolvedDate(){
    	return this.ResolvedDate;
    }
    public void setResolvedDate(Date date) {
  	  this.ResolvedDate = date;
  	}
    public Number getSizeField() {
	  return this.sizeVal;
	}
	public void setSizeField (Number size) {
		  this.sizeVal = size;
	}
	public Integer getWeekIndexCreate() {
	  return this.weekIndexCreate;
	}
	public void setWeekIndexCreate (Integer index) {
	  this.weekIndexCreate = index;
	}
	public Integer getWeekIndexStateChange() {
	  return this.weekIndexStateChange;
	}
	public void setWeekIndexStatChange (Integer index) {
	  this.weekIndexStateChange = index;
	}
	public Integer getWeekIndexResolved() {
	  return this.weekIndexResolved;
	}
	public void setWeekIndexResolved (Integer index) {
	  this.weekIndexResolved = index;
	}
	public Number getReadyEstimate() {
		return this.readyEstimate;
	}
	public void setReadyEstimate (Number num) {
		this.readyEstimate = num;
	}
	public Number getOriginalEstimate() {
		return this.originalEstimate;
	}
	public void setOriginalEstimate(Number num) {
		this.originalEstimate = num;
	}
    public String getSeverity () {
    	return this.severity;
    }
    public void setSeverity(String severity) {
    	this.severity = severity;
    }
	@Override
	public int compareTo(WorkItemData wd) {
	    return (getStateChangeDate().compareTo(wd.getStateChangeDate()));
	}

}


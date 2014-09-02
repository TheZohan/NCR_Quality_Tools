package tfsReports;

import java.util.Date;

public class SortableDate implements Comparable<SortableDate> {

	  private Date date;
	  private Integer size;
	  private Integer weekIndex;

	  public Date getDate() {
	    return date;
	  }
	  public Integer getSizeField() {
		  return size;
	  }
	  public Integer getWeekIndex() {
		  return weekIndex;
	  }
	  public void setDate(Date date) {
	    this.date = date;
	  }
	  public void setSizeField (Integer size) {
		  this.size = size;
	  }
	  public void setWeekIndex (Integer index) {
		  this.weekIndex = index;
	  }

	  @Override
	  public int compareTo(SortableDate sd) {
	    return (getDate().compareTo(sd.getDate()));
	  }
	}

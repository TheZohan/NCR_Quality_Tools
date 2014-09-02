package excelServices;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import jxl.Workbook;
import jxl.write.DateTime;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import tfsReports.SortableDate;

public class ExcelServices {
	protected WritableWorkbook resultsWorkbook;
	protected WritableCellFormat cellFormat;
	
	
	public void openFiles(File output_file) throws IOException{
				  resultsWorkbook = Workbook.createWorkbook(output_file);
			      cellFormat = new jxl.write.WritableCellFormat (new jxl.write.DateFormat("m/d/yy"));
		}
	
	public void closeAll(){
		try {
		  resultsWorkbook.write(); 
		  resultsWorkbook.close();
		} catch(Exception ioe) {
		      ioe.printStackTrace();
		}
	}

	public void createSheet(String sheetName, int sheetNumber) {
		  resultsWorkbook.createSheet(sheetName, sheetNumber);
	}
	
	public void addLabel(int col, String col_header, int row, int sheetIndex) {
		WritableSheet sheet = resultsWorkbook.getSheet(sheetIndex);
		try {
		Label label = new Label(col,row,col_header); 
		sheet.addCell(label);
		sheet.setColumnView(col, col_header.length()+1);
		} catch (WriteException e) {
    		
    		e.printStackTrace();}
	}
	
	public void setColumnWidth (int column, int columnWidth, int sheetIndex){
		WritableSheet sheet = resultsWorkbook.getSheet(sheetIndex);
		sheet.setColumnView(column, columnWidth);
	}
	
	 public void writeNumberCell (int col, int row, Number val, int sheetIndex) {
	    	Number num;
	    	String valString = val.toString();
	    	double value = val.getValue();
	    	WritableSheet sheet = resultsWorkbook.getSheet(sheetIndex);
	    	
	    	num = new Number (col, row, value);
		    try {
				sheet.addCell(num);
				sheet.setColumnView(col, valString.length()+1);
			} catch (RowsExceededException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WriteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	 
	 public void writeDateCell (int col, int row, SortableDate sd, int sheetIndex) {
		 WritableSheet sheet = resultsWorkbook.getSheet(sheetIndex);
		 Date date = sd.getDate();
	     DateTime dt = new DateTime(col, row, date);

	     dt.setCellFormat(cellFormat);
	
		 try {
				sheet.addCell(dt);
			} catch (RowsExceededException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WriteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	 }
	 
}


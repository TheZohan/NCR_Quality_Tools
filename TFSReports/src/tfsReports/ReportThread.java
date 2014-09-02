package tfsReports;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import tfsAccess.TFSProgressCallBack;

public class ReportThread implements Runnable{
	private Thread t;
	private String threadName;
	private File allBugQueryFile, coreBugQueryFile, userStoryQueryFile, outputFile;
	private TFSReportsGenerator rGen;
	
	public ReportThread (String name, File allQuery, File coreQuery, File userStoryQuery, File out, TFSProgressCallBack callBack, TFSReportsGenerator rgen) {
		threadName = name;
		allBugQueryFile = allQuery;
		coreBugQueryFile = coreQuery;
		userStoryQueryFile = userStoryQuery;
		outputFile = out;
		rGen = rgen;
		rGen.ReportGenerator (allBugQueryFile, coreBugQueryFile, userStoryQueryFile, outputFile, callBack);
	}
	
	public void run(){
			try {
				rGen.runReports(); 
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Output file can't be accessed (open or corrupted)");
				System.exit(0);
			} catch (tfsAccess.TFSException e) {
				JOptionPane.showMessageDialog(null, "TFS Connection error");
				System.exit(0);	
			}
			rGen = null;;
	}

	public void start () {
		if (t == null)
		{
			t = new Thread (this, threadName);
			t.start ();
		}
	}

}


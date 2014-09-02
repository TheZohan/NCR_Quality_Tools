package changeSetMap;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

public class ReportThread implements Runnable{
	private Thread t;
	private String threadName;
	File queryFile, outputFile;
	ChangeSetMapGenerator rGen;
	
	public ReportThread (String name, File allQuery, File out, ChangeSetMapApp appWin) {
		threadName = name;
		queryFile = allQuery;
		outputFile = out;
		rGen = new ChangeSetMapGenerator (queryFile, outputFile, appWin);
	}
	
	public void run(){
			try {
				rGen.runReports(); 
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Output file can't be accessed (open or corrupted)");
				System.exit(0);
			} catch (TFSException e) {
				JOptionPane.showMessageDialog(null, "TFS Connection error");
				System.exit(0);	
			}
	}

	public void start () {
		if (t == null)
		{
			t = new Thread (this, threadName);
			t.start ();
		}
	}

}


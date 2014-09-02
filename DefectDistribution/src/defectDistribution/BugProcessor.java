package defectDistribution;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import tfsAccess.TFSException;
import tfsAccess.TFSProgressCallBack;

public class BugProcessor implements Runnable{
	private Thread t;
	private String threadName;
	private File queryFile, outputFile;
	DefectDistribution ddObject;
	
	public BugProcessor (String name, File in, File out, TFSProgressCallBack callBack){
		threadName = name;
		queryFile = in;
		outputFile = out;
		ddObject = new DefectDistribution (callBack);
		
	}
	
	public void run(){
					ddObject.defectDistributionSetup (queryFile, outputFile);
					try {
						ddObject.runAnalysis();
					} catch (IOException e) {
						JOptionPane.showMessageDialog(null, "Output file can't be accessed (open or corrupted)");
						System.exit(0);
					} catch (TFSException t) {
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



package defectDistribution;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.Timer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;
import java.awt.Color;
import javax.swing.JTextArea;

import tfsAccess.TFSProgressCallBack;

import java.awt.Component;


  public class DefectDistributionAppWindow implements TFSProgressCallBack{

	private JFrame frmBugReport;
	static File allBugQueryFile=null, outputFile=null;
	static String defaultWorkDir = ".";
	static File workDir = new File (defaultWorkDir);
	static String allBugQueryFileName=null, outputFileName=null, workDirName=null;
	static Properties prop = new Properties();
	static String textToDisplay = new String(" ");
	static JTextArea progressBar;
	DefectDistribution ddObject = new DefectDistribution (this);
	TFSProgressCallBack callBack = this;

	/**
	 * Launch the application.
	 */

	public static void main(String[] args) {

		InputStream configStream = null;
		File configFile = new File ("config.properties");
	    if (configFile.isFile()) {
		  try {
	        
		   	     configStream = new FileInputStream(configFile);
	       			// load a properties file
			     prop.load(configStream);
			     allBugQueryFileName = prop.getProperty("allBugsQuery");
			     if (allBugQueryFileName != null) allBugQueryFile = new File (allBugQueryFileName);
			     outputFileName = prop.getProperty("outputFile");
			     if (outputFileName != null) outputFile = new File (outputFileName);
			     defaultWorkDir = prop.getProperty("workDir"); 
			     if (defaultWorkDir != null) workDir = new File (defaultWorkDir);
			     else defaultWorkDir = ".";

	          }  catch (IOException ex) {
	 		      ex.printStackTrace();
	      	  } finally {
	 		     if (configStream != null) {
	 			  try {
	 				configStream.close();
	 			  } catch (IOException e) {
	 				e.printStackTrace();
	 			  }
	 		     }
	 	      }
	    }
	    
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DefectDistributionAppWindow window = new DefectDistributionAppWindow();
					window.frmBugReport.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	  }		
		
	/**
	 * Create the application.
	 */
	public DefectDistributionAppWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmBugReport = new JFrame();
		frmBugReport.setTitle("Defect Distribution");
		frmBugReport.setBounds(100, 100, 580, 500);
		frmBugReport.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmBugReport.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		frmBugReport.addWindowListener(new WindowAdapter() {         
		  public void windowClosing(WindowEvent windowEvent){
	     		//save config
	     		OutputStream output = null;
	     	 
	     		try {
	     	 
	     			output = new FileOutputStream("config.properties");
	     	 
	     			// set the properties value
	     			if (allBugQueryFileName == null) allBugQueryFileName = " ";
	     			prop.setProperty("allBugsQuery", allBugQueryFileName);	
	     			if (outputFileName == null)  outputFileName = " ";
	     		    prop.setProperty("outputFile", outputFileName);
	     		    prop.setProperty("workDir", defaultWorkDir);
	     			// save properties to project root folder
	     			prop.store(output, null);
	     	 
	     		} catch (IOException io) {
	     			io.printStackTrace();
	     		} finally {
	     			if (output != null) {
	     				try {
	     					output.close();
	     				} catch (IOException e) {
	     					e.printStackTrace();
	     				}
	     			}
	     	 
	     		}
	            System.exit(0);
	         }        
	      });
		
		JPanel panel_1 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_1.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		frmBugReport.getContentPane().add(panel_1);
		
		JButton btnSelectWorkDir = new JButton("Select root Directory (optional)");
		btnSelectWorkDir.setVerticalAlignment(SwingConstants.TOP);
		btnSelectWorkDir.setHorizontalAlignment(SwingConstants.LEFT);
		panel_1.add(btnSelectWorkDir);
		
		final JTextArea textArea = new JTextArea();
		textArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
		textArea.setColumns(50);
		panel_1.add(textArea);
		if (defaultWorkDir != null) textArea.setText(defaultWorkDir);
		btnSelectWorkDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.setCurrentDirectory(new File (defaultWorkDir));
				fc.showOpenDialog(frmBugReport);
				workDir = fc.getCurrentDirectory();
				fc.setCurrentDirectory(workDir);
				if (workDir != null) {
					defaultWorkDir = workDir.getAbsolutePath();
					textArea.setText(defaultWorkDir);
				}
				
			}
		});
		
				JPanel panel_2 = new JPanel();
				frmBugReport.getContentPane().add(panel_2);
				FlowLayout flowLayout_2 = (FlowLayout) panel_2.getLayout();
				flowLayout_2.setAlignment(FlowLayout.LEFT);
				
				JButton btnSelectInputFile = new JButton("Select All Bugs query file");
				btnSelectInputFile.setHorizontalAlignment(SwingConstants.LEFT);
				panel_2.add(btnSelectInputFile);
				
				final JTextArea textArea_2 = new JTextArea();
				textArea_2.setColumns(50);
				panel_2.add(textArea_2);
				if (allBugQueryFileName != null) textArea_2.setText(allBugQueryFileName);
				btnSelectInputFile.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						final JFileChooser fc = new JFileChooser();
						fc.setCurrentDirectory(workDir);
						fc.showOpenDialog(frmBugReport);
						allBugQueryFile = fc.getSelectedFile();
						if (allBugQueryFile != null){
							allBugQueryFileName = allBugQueryFile.getAbsolutePath();
							textArea_2.setText(allBugQueryFileName);
						}
					}
				});
		
		JPanel panel_4 = new JPanel();
		FlowLayout flowLayout_4 = (FlowLayout) panel_4.getLayout();
		flowLayout_4.setAlignment(FlowLayout.LEFT);
		frmBugReport.getContentPane().add(panel_4);
		
		JButton btnSelectOutputFile = new JButton("Select report output file");
		panel_4.add(btnSelectOutputFile);
		
		final JTextArea textArea_4 = new JTextArea();
		textArea_4.setColumns(50);
		panel_4.add(textArea_4);
		if (outputFileName != null) textArea_4.setText(outputFileName);
		btnSelectOutputFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(workDir);
			    fc.showOpenDialog(frmBugReport);
			    outputFile = fc.getSelectedFile();
			    if (outputFile != null){
			    	outputFileName = outputFile.getAbsolutePath();
			    	textArea_4.setText(outputFileName);
			    }
			}
		});
		
		final JPanel panel_5 = new JPanel();
		frmBugReport.getContentPane().add(panel_5);
		
		JButton btnProcess = new JButton("Run report");
		btnProcess.setBackground(Color.GREEN);
		panel_5.add(btnProcess);
		
		JButton btnNewButton = new JButton("Clear all selections");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				allBugQueryFile=null; outputFile=null;
				allBugQueryFileName=null; outputFileName=null; workDirName=null;
				defaultWorkDir = ".";
				textArea.setText(" ");
				textArea_2.setText(" ");
				textArea_4.setText(" ");
			}
		});
		btnNewButton.setBackground(Color.YELLOW);
		panel_5.add(btnNewButton);
		
		final JTextArea textArea_1 = new JTextArea();
		textArea_1.setColumns(40);
		panel_5.add(textArea_1);
		progressBar = textArea_1;

		btnProcess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {	
				  if (allBugQueryFile != null && outputFile != null){
					  BugProcessor bugProcessor = new BugProcessor ("Bug Processor", allBugQueryFile, outputFile, callBack);
					  bugProcessor.start();
				  }
				  else JOptionPane.showMessageDialog(null, "Mandatory file missing (bugs query or output file)");
			}
		});
		Timer timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {		                
                progressBar.setText(textToDisplay);
            }
        });
		timer.setInitialDelay(1000);
        timer.start();	

	}

	public void progressCallBack (String text){
		textToDisplay = text;
	}
    
	
}


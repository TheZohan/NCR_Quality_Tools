package bugChangeSetReport;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import tfsReports.ReportThread;
import tfsReports.TFSReportsGenerator;

import java.awt.Component;
import javax.swing.JRadioButton;

public class BugChangeSetApp implements TFSProgressCallBack{

	private JFrame frmBugReport;
	private static File queryFile=null, outputFile=null, coreBugQueryFile=null;
	private static String defaultWorkDir = ".";
	private static File workDir = new File (defaultWorkDir);
	private static String queryFileName=null, coreBugQueryFileName=null, outputFileName=null;
	private static Properties prop = new Properties();
	private TFSProgressCallBack callBack = this;
	private static String textToDisplay = new String(" ");
	private static JTextArea progressBar;
	private int maxRecursionDepth = 3;  //default value is 3; Changeset client will follow links


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		InputStream configStream = null;
		File configFile = new File ("bugChangeSetReport.properties");
	    if (configFile.isFile()) {
		  try {
	        
		   	     configStream = new FileInputStream(configFile);
	       			// load a properties file
			     prop.load(configStream);
			     queryFileName = prop.getProperty("QueryFilename");
			     if (queryFileName != null) queryFile = new File (queryFileName); 
			     coreBugQueryFileName = prop.getProperty("coreLinkQuery");
			     if (coreBugQueryFileName.equalsIgnoreCase(" ")) coreBugQueryFileName = null;
			     if (coreBugQueryFileName != null) coreBugQueryFile = new File (coreBugQueryFileName);   
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
					BugChangeSetApp window = new BugChangeSetApp();
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
	public BugChangeSetApp() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmBugReport = new JFrame();
		frmBugReport.setTitle("Bug change set Report");
		frmBugReport.setBounds(100, 100, 580, 500);
		frmBugReport.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmBugReport.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		frmBugReport.addWindowListener(new WindowAdapter() {         
		  public void windowClosing(WindowEvent windowEvent){
	     		//save config
	     		OutputStream output = null;
	     	 
	     		try {
	     	 
	     			output = new FileOutputStream("bugChangeSetReport.properties");	     	 
	     			// set the properties value
	     			if (queryFileName == null) queryFileName = " ";
	     			prop.setProperty("QueryFilename", queryFileName);
	     			if (coreBugQueryFileName == null) coreBugQueryFileName = " ";
	     			prop.setProperty("coreLinkQuery", coreBugQueryFileName);
	     			if (outputFileName == null) outputFileName = " ";
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
		
		JPanel panel_3 = new JPanel();
		FlowLayout flowLayout_3 = (FlowLayout) panel_3.getLayout();
		flowLayout_3.setAlignment(FlowLayout.LEFT);
		frmBugReport.getContentPane().add(panel_3);
		
		JButton btnSelectQuery = new JButton("Select query file");
		panel_3.add(btnSelectQuery);
		
		final JTextArea textArea_3 = new JTextArea();
		textArea_3.setColumns(50);
		panel_3.add(textArea_3);
		if (queryFileName != null) textArea_3.setText(queryFileName);
		btnSelectQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(workDir);
				fc.showOpenDialog(frmBugReport);
				queryFile = fc.getSelectedFile();
				if (queryFile != null){
					queryFileName = queryFile.getAbsolutePath();
					textArea_3.setText(queryFileName);
				}
			}
		});

		JPanel panel = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panel.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		frmBugReport.getContentPane().add(panel);
		
		JButton btnSelectCoreLinks = new JButton("Select core links query file (optional)");
		panel.add(btnSelectCoreLinks);
		
		final JTextArea textArea_1 = new JTextArea();
		textArea_1.setColumns(50);
		panel.add(textArea_1);
		if (coreBugQueryFileName != null) textArea_1.setText(coreBugQueryFileName);
		btnSelectCoreLinks.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(workDir);
				fc.showOpenDialog(frmBugReport);
				coreBugQueryFile = fc.getSelectedFile();
				if (coreBugQueryFile != null) {
					coreBugQueryFileName = coreBugQueryFile.getAbsolutePath();
					textArea_1.setText(coreBugQueryFileName);
				}
			}
		});
		
		JPanel panel_4 = new JPanel();
		FlowLayout flowLayout_4 = (FlowLayout) panel_4.getLayout();
		flowLayout_4.setAlignment(FlowLayout.LEFT);
		frmBugReport.getContentPane().add(panel_4);
		
		JButton btnSelectOutputFile = new JButton("Select report output file (.xls)");
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
		
		JPanel panel_5 = new JPanel();
		frmBugReport.getContentPane().add(panel_5);
		
		JRadioButton rdbtnDoNotFollow = new JRadioButton("Do not follow Links");
		panel_5.add(rdbtnDoNotFollow);
		rdbtnDoNotFollow.addItemListener(new ItemListener() {
	         public void itemStateChanged(ItemEvent e) {         
	        	 if (e.getStateChange()==1)  maxRecursionDepth = 0;
	        	 else maxRecursionDepth = 3;
	         }    
	      });
		
		JButton btnProcess = new JButton("Run report");
		btnProcess.setBackground(Color.GREEN);
		panel_5.add(btnProcess);
		
		JButton btnNewButton = new JButton("Clear all selections");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				queryFile=null; outputFile=null; coreBugQueryFile=null;
				queryFileName=null; coreBugQueryFileName=null; outputFileName=null;
				defaultWorkDir = ".";
				textArea.setText(" ");
				textArea_3.setText(" ");
				textArea_4.setText(" ");
				textArea_1.setText(" ");
			}
		});
		btnNewButton.setBackground(Color.YELLOW);
		panel_5.add(btnNewButton);

		final JTextArea textArea_5 = new JTextArea();
		textArea_5.setColumns(40);
		panel_5.add(textArea_5);
		progressBar = textArea_5;
		
		btnProcess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				TFSReportsGenerator rGen = new BugChangeSetReport();
				try {
				  if (queryFile != null && outputFile != null){
					  ReportThread reportThread = new ReportThread ("Report Thread", queryFile, coreBugQueryFile, null,  outputFile, callBack, rGen);
					  rGen.setMaxRecursion (maxRecursionDepth);
					  reportThread.start();
				  }
				  	else JOptionPane.showMessageDialog(null, "Mandatory file missing (Work Item query or output file)");
				  } catch (Throwable t) {
					  if(t instanceof Exception){
					        if(t instanceof IOException){
					        	JOptionPane.showMessageDialog(null, "Output file can't be accessed (open or corrupted)");
					        }else {
					        	  JOptionPane.showMessageDialog(null, "General failure in TFSreports");
					             }
					        }
				       }  
				  
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

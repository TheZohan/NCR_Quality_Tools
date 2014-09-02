package tfsAccess;
import java.awt.*;
import javax.swing.*;

public class LoginDialog {
	private String userName, userPassword, userDomain = "RWN\\";

    public LoginDialog() {
        getIDandPassword();
    }

    // modal dialog to get user ID and password
    private static String[] ConnectOptionNames = { "Login", "Cancel" };
    private static String   ConnectTitle = "TFS Login";
    void getIDandPassword() {
    final JPanel connectionPanel;

 	// Create the labels and text fields.
	final JLabel     userNameLabel = new JLabel("Username:   ", JLabel.RIGHT);
 	final JTextField userNameField = new JTextField("");
	final JLabel     passwordLabel = new JLabel("Password:   ", JLabel.RIGHT);
	final JTextField passwordField = new JPasswordField("");
	
	connectionPanel = new JPanel(false);
	connectionPanel.setLayout(new BoxLayout(connectionPanel,
						BoxLayout.X_AXIS));
	JPanel namePanel = new JPanel(false);
	namePanel.setLayout(new GridLayout(0, 1));
	namePanel.add(userNameLabel);
	namePanel.add(passwordLabel);
	JPanel fieldPanel = new JPanel(false);
	fieldPanel.setLayout(new GridLayout(0, 1));
	fieldPanel.add(userNameField);
	fieldPanel.add(passwordField);
	connectionPanel.add(namePanel);
	connectionPanel.add(fieldPanel);

        // Connect or quit
	if(JOptionPane.showOptionDialog(null, connectionPanel, 
                                        ConnectTitle,
                                        JOptionPane.OK_CANCEL_OPTION, 
                                        JOptionPane.INFORMATION_MESSAGE,
                                        null, ConnectOptionNames, 
                                        ConnectOptionNames[0]) != 0) 
        {
	    System.exit(0);
	}
        String tmp = userNameField.getText();
        userPassword = passwordField.getText();
        userName = userDomain+tmp;
    }
    
    public String getUserName() {
    	return this.userName;
    }
    public String getPassword () {
    	return this.userPassword;
    }
}

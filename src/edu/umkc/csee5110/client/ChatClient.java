package edu.umkc.csee5110.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import edu.umkc.csee5110.utils.Constants;
import edu.umkc.csee5110.utils.TabbedPane;

public class ChatClient {
	
	BufferedReader input;
	PrintWriter output;
	JFrame frame = new JFrame("Chat Client");
	JTextField textField = new JTextField(40);
	JTextArea textArea = new JTextArea(8, 40);
	
	public ChatClient() {
//		textField.setEditable(false);
//		textArea.setEditable(false);
//		frame.getContentPane().add(textField, "North");
//		frame.getContentPane().add(new JScrollPane(textArea), "Center");
		
		frame.pack();
		
//		textField.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				output.println(textField.getText());
//				textField.setText("");
//			}
//		});
	}
	
	private JComponent makePublicTab() {
		JPanel panel = new JPanel(false);
//		JTextField textField = new JTextField(40);
//		JTextArea textArea = new JTextArea(8, 40);
		
		textField.setEditable(false);
		textArea.setEditable(false);
		
		BoxLayout grid = new BoxLayout(panel, BoxLayout.Y_AXIS);
		
		panel.setLayout(grid);
		panel.add(new JScrollPane(textArea), "Center");
		panel.add(textField, "North");
		
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				output.println(textField.getText());
				textField.setText("");
			}
		});
		
		
		//panel.add(title);
		//textField.requestFocusInWindow();
//		SwingUtilities.invokeLater( new Runnable() { 
//
//			public void run() { 
//			        textField.requestFocus(); 
//			    } 
//			} );
		return panel;		
	}
	
	
	private String getName() {
		return JOptionPane.showInputDialog(
				frame,
				"Enter a screen name: ",
				"Screen name selection",
				JOptionPane.PLAIN_MESSAGE);
	}
	
	private void run() throws IOException {
		Socket socket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT);
		input = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		output = new PrintWriter(socket.getOutputStream(), true);
		
		while (true) {
			String line = input.readLine();
			if (line.startsWith("SUBMITNAME")) {
				output.println(getName());
				textField.requestFocus();
			} else if (line.startsWith("NAMEACCEPTED")) {
				textField.setEditable(true);
			} else if (line.startsWith("MESSAGE")) {
				textArea.append(line.substring(8) + "\n");
			}
		}
	}
	
	private class TabbedPane extends JPanel {
		TabbedPane() {
			super(new GridLayout(1, 1));
			JTabbedPane tabbedPane = new JTabbedPane();
			JComponent publicPanel = makePublicTab();
			tabbedPane.add("Public", publicPanel);
			
		}
	}
	
	public JTabbedPane createNewPane() {
		JTabbedPane tabbedPane = new JTabbedPane();
		JComponent publicPanel = makePublicTab();
		tabbedPane.add("Public", publicPanel);
		return tabbedPane;
	}
 
	public static void main(String[] args) throws Exception {
		ChatClient client = new ChatClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.add(client.createNewPane(), BorderLayout.CENTER);
		client.frame.pack();
		client.frame.setVisible(true);
		client.run();
	}

}
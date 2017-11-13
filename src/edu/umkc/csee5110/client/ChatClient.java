package edu.umkc.csee5110.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.umkc.csee5110.utils.Constants;

public class ChatClient {
	
	BufferedReader input;
	PrintWriter output;
	JFrame frame = new JFrame("Textbox");
	JTextField textField = new JTextField(40);
	JTextArea textArea = new JTextArea(8, 40);
	
	public ChatClient() {
		textField.setEditable(false);
		textArea.setEditable(false);
		frame.getContentPane().add(textField, "North");
		frame.getContentPane().add(new JScrollPane(textArea), "Center");
		frame.pack();
		
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				output.println(textField.getText());
				textField.setText("");
			}
		});
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
			} else if (line.startsWith("NAMEACCEPTED")) {
				textField.setEditable(true);
			} else if (line.startsWith("MESSAGE")) {
				textArea.append(line.substring(8) + "\n");
			}
		}
	}
 
	public static void main(String[] args) throws Exception {
		ChatClient client = new ChatClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}

}
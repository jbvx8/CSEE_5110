package edu.umkc.csee5110.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.umkc.csee5110.utils.Constants;

public class ChatClient {
	
	String name;
	BufferedReader input;
	PrintWriter output;
	JFrame frame = new JFrame("Chat Client");
	JTextField textField = new JTextField(40);
	JTextArea textArea = new JTextArea(8, 40);
	JTabbedPane tabbedPane = new JTabbedPane();
	DefaultListModel<String> model = new DefaultListModel<String>();
	Map<String, JTextArea> privateChatUsers = new HashMap<>();
	
	public ChatClient() {
		frame.pack();
	}
	
	public synchronized void write(String out) {
		output.println(out);
	}
	
	private JComponent makePublicTab() {
		JPanel panel = new JPanel();
		JPanel textPanel = new JPanel();
		JPanel namePanel = new JPanel();
		
		textField.setEditable(false);
		textArea.setEditable(false);
		
		BoxLayout grid = new BoxLayout(textPanel, BoxLayout.Y_AXIS);
		textPanel.setLayout(grid);
		textPanel.add(new JScrollPane(textArea));
		textPanel.add(textField);
		JScrollPane namePane = new JScrollPane();
		namePane.setBorder(BorderFactory.createEmptyBorder());
		JList<String> nameList = new JList<String>(model);
		nameList.setLayout(new BoxLayout(nameList, BoxLayout.Y_AXIS));
		namePane.setViewportView(nameList);
		namePanel.add(namePane);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(textPanel);
		panel.add(namePanel);
		
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				write(textField.getText());
				textField.setText("");
			}
		});
		
		nameList.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				String selectedValue = nameList.getSelectedValue();
				if (selectedValue == null || selectedValue.isEmpty() || privateChatUsers.keySet().contains(selectedValue)) {
					return;
				}
				
				JPanel textPanel2 = new JPanel();
				JTextField textField2 = new JTextField(40);
				JTextArea textArea2 = new JTextArea(8, 40);
				BoxLayout grid = new BoxLayout(textPanel2, BoxLayout.Y_AXIS);
				textPanel2.setLayout(grid);
				textPanel2.add(new JScrollPane(textArea2));
				textPanel2.add(textField2);
				
				textField2.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						write("PRIVATE_CHAT_SEND_" + name + "_" + selectedValue + " " + textField2.getText());
						textArea2.append(name + ": " + textField2.getText() + "\n");
						textField2.setText("");
					}
				});
				
				tabbedPane.addTab(nameList.getSelectedValue(), textPanel2);
				privateChatUsers.put(selectedValue, textArea2);
				write("PRIVATE_CHAT_INITIATE_" + selectedValue + " " + textField2.getText());
			}
		});
		
		return panel;		
	}
	
	
	private String getName() {
		return JOptionPane .showInputDialog(
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
			System.out.println(line);
			if (line.startsWith("SUBMITNAME")) {
				//output.println(getName());
				name = getName();
				write(name);
				textField.requestFocus();
			} else if (line.startsWith("NAMEACCEPTED")) {
				textField.setEditable(true);
			} else if (line.startsWith("MESSAGE")) {
				textArea.append(line.substring(8) + "\n");
			} else if (line.startsWith("NAME")) {
				//nameList.append(line.substring(5) + "\n");	
				//nameList.add(new JLabel(line.substring(5)));
				if (!model.contains(line.substring(5))) {
					model.addElement(line.substring(5));
				}
				//output.println("NEWNAME " + getName());
				System.out.println("Got message " + line);
			} else if (line.startsWith("REMOVENAME")) {
				// TODO remove any chat window
				model.removeElement(line.substring(11));
			} else if (line.startsWith("PRIVATE_CHAT_INITIATE_")) {
				String otherUser = line.substring("PRIVATE_CHAT_INITIATE_".length());
				System.out.println("PRIV " + otherUser + " PRIVCHAT: " + privateChatUsers);
				if (!privateChatUsers.keySet().contains(otherUser)) {
					JPanel textPanel2 = new JPanel();
					JTextField textField2 = new JTextField(40);
					JTextArea textArea2 = new JTextArea(8, 40);
					BoxLayout grid = new BoxLayout(textPanel2, BoxLayout.Y_AXIS);
					textPanel2.setLayout(grid);
					textPanel2.add(new JScrollPane(textArea2));
					textPanel2.add(textField2);
					
					textField2.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							write("PRIVATE_CHAT_SEND_" + name + "_" + otherUser + " " + textField2.getText());
							textArea2.append(name + ": " + textField2.getText() + "\n");
							textField2.setText("");
						}
					});
					
					tabbedPane.addTab(otherUser, textPanel2);
					privateChatUsers.put(otherUser, textArea2);
				}
			} else if (line.startsWith("PRIVATE_CHAT_RECEIVE")) {
				String requestedPartner1 = line.substring("PRIVATE_CHAT_RECEIVE_".length(), line.indexOf(" ")).trim();
				String sender = requestedPartner1.substring(0, requestedPartner1.indexOf("_"));
				String receiver = requestedPartner1.substring(requestedPartner1.lastIndexOf("_") + 1);
				String message = line.substring(line.indexOf(" "));
				System.out.println(message + " " + sender + " " + receiver + " " + privateChatUsers);
				if (privateChatUsers.keySet().contains(sender)) {
					System.out.println("message from " + receiver + " : " + message);
					JTextArea textArea = privateChatUsers.get(sender);
					textArea.append(sender + ": " + message + "\n");
				}
			}
		}
	}
		
	public JTabbedPane createNewPane() {
		JComponent publicPanel = makePublicTab();
		tabbedPane.add("Public", publicPanel);
		return tabbedPane;
	}
 
	public static void main(String[] args) throws Exception {
		ChatClient client = new ChatClient();
		client.frame.setPreferredSize(new Dimension(1000, 500));
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.add(client.createNewPane(), BorderLayout.CENTER);
		client.frame.pack();
		client.frame.setVisible(true);
		client.run();
	}

}
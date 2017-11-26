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
import java.util.Map;

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

import edu.umkc.csee5110.MessageType;
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
				write(MessageType.SEND_MESSAGE + "|" + textField.getText());
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

				makePrivateChat(selectedValue);
				write(MessageType.PRIVATE_CHAT_INITIATE + "|" + selectedValue);
			}
		});

		return panel;
	}
	
	private void makePrivateChat(String otherUser) {
		JPanel textPanel2 = new JPanel();
		JTextField textField2 = new JTextField(40);
		JTextArea textArea2 = new JTextArea(8, 40);
		BoxLayout grid = new BoxLayout(textPanel2, BoxLayout.Y_AXIS);
		textPanel2.setLayout(grid);
		textPanel2.add(new JScrollPane(textArea2));
		textPanel2.add(textField2);

		textField2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				write(MessageType.PRIVATE_CHAT_SEND + "|" + name + "|" + otherUser + "|" + textField2.getText());
				textArea2.append(name + ": " + textField2.getText() + "\n");
				textField2.setText("");
			}
		});

		tabbedPane.addTab(otherUser, textPanel2);
		privateChatUsers.put(otherUser, textArea2);
	}

	private String getName() {
		return JOptionPane.showInputDialog(frame, "Enter a screen name: ", "Screen name selection", JOptionPane.PLAIN_MESSAGE);
	}

	private void run() throws IOException {
		try (Socket socket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT)) {
			
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);

			while (true) {
				String line = input.readLine();
				System.out.println(line);
				int delimiterLocation = line.indexOf("|") == -1 ? line.length() : line.indexOf("|");
				String messageType = line.substring(0, delimiterLocation);
				MessageType type = MessageType.valueOf(messageType);
				
				switch (type) {
				case REQUESTNAME:
					name = getName();
					write(name);
					textField.requestFocus();
					break;
				case NAMEACCEPTED:
					textField.setEditable(true);
					break;
				case RECEIVE_MESSAGE:
					String temp = line.substring(MessageType.RECEIVE_MESSAGE.name().length() + 1);
					String user = temp.substring(0, temp.indexOf("|"));
					String msg = line.substring(ordinalIndexOf(line, "|", 2, false) + 1);
					System.out.println("user: " + user + " recv: " + msg);
					textArea.append(user + ": " + msg + "\n");
					break;
				case ADD_NAME:
					if (!model.contains(line.substring(MessageType.ADD_NAME.name().length() + 1))) {
						model.addElement(line.substring(MessageType.ADD_NAME.name().length() + 1));
					}
					System.out.println("Got message " + line);
					break;
				case REMOVE_NAME:
					model.removeElement(line.substring(MessageType.REMOVE_NAME.name().length() + 1));
					break;
				case PRIVATE_CHAT_INITIATE:
					String otherUser = line.substring(MessageType.PRIVATE_CHAT_INITIATE.name().length() + 1);
					if (!privateChatUsers.keySet().contains(otherUser)) {
						makePrivateChat(otherUser);
					}
					break;
				case PRIVATE_CHAT_RECEIVE:
					String typeRemoved = line.substring(line.indexOf("|") + 1);
					String senderRemoved = typeRemoved.substring(typeRemoved.indexOf("|") + 1);
					String message = senderRemoved.substring(typeRemoved.indexOf("|") + 1);
					String sender = typeRemoved.substring(0,  typeRemoved.indexOf("|"));
					String receiver = senderRemoved.substring(0, senderRemoved.indexOf("|"));
					
					System.out.println(message + " " + sender + " " + receiver + " " + privateChatUsers);
					if (privateChatUsers.keySet().contains(sender)) {
						System.out.println("message from " + receiver + " : " + message);
						JTextArea textArea = privateChatUsers.get(sender);
						textArea.append(sender + ": " + message + "\n");
					}
					break;
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
	
	private static int ordinalIndexOf(String str, String searchStr, int ordinal, boolean lastIndex) {
        if (str == null || searchStr == null || ordinal <= 0) {
            return -1;
        }
        if (searchStr.length() == 0) {
            return lastIndex ? str.length() : 0;
        }
        int found = 0;
        int index = lastIndex ? str.length() : -1;
        do {
            if(lastIndex) {
                index = str.lastIndexOf(searchStr, index - 1);
            } else {
                index = str.indexOf(searchStr, index + 1);
            }
            if (index < 0) {
                return index;
            }
            found++;
        } while (found < ordinal);
        return index;
    }

}
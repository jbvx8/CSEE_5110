package edu.umkc.csee5110.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.umkc.csee5110.FileTransferModel;
import edu.umkc.csee5110.MessageType;
import edu.umkc.csee5110.utils.Constants;

public class ChatClient {

	String name;
	BufferedReader input;
	PrintWriter output;
	JFrame frame = new JFrame("Chat Client");
	JTextField textField = new JTextField(1);
	JTextArea textArea = new JTextArea(25, 40);
	JTabbedPane tabbedPane = new JTabbedPane();
	DefaultListModel<String> model = new DefaultListModel<String>();
	Map<String, JTextArea> privateChatUsers = new HashMap<>();
	Map<String, FileTransferModel> fileToModelMap = new HashMap<>();

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

				makePrivateChat(selectedValue, true);
				write(MessageType.PRIVATE_CHAT_INITIATE + "|" + selectedValue);
			}
		});

		return panel;
	}

	private void makePrivateChat(String otherUser, boolean switchFocus) {
		JPanel textPanel2 = new JPanel();
		JTextField textField2 = new JTextField(40);
		JTextArea textArea2 = new JTextArea(8, 40);
		JButton sendFile = new JButton("Send File");
		BoxLayout grid = new BoxLayout(textPanel2, BoxLayout.Y_AXIS);
		textPanel2.setLayout(grid);
		textPanel2.add(new JScrollPane(textArea2));
		textPanel2.add(textField2);
		textPanel2.add(sendFile);

		textField2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				write(MessageType.PRIVATE_CHAT_SEND + "|" + name + "|" + otherUser + "|" + textField2.getText());
				textArea2.append(name + ": " + textField2.getText() + "\n");
				textField2.setText("");
			}
		});

		JFileChooser fileChooser = new JFileChooser();
		sendFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int returnValue = fileChooser.showOpenDialog(sendFile);
				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					fileToModelMap.put(file.getName(), new FileTransferModel(file, otherUser));
					write(MessageType.FILE_INITIATE + "|" + name + "|" + otherUser + "|" + file.getName() + "|" + file.length());
					System.out.println(MessageType.FILE_INITIATE + "|" + name + "|" + otherUser + "|" + file.getName() + "|" + file.length());
				}
			}

		});

		tabbedPane.addTab(otherUser, textPanel2);
		if (switchFocus) {
			tabbedPane.setSelectedComponent(textPanel2);
		}
		privateChatUsers.put(otherUser, textArea2);
	}

	private String getName() {
		return JOptionPane.showInputDialog(frame, "Enter a screen name: ", "Screen name selection", JOptionPane.PLAIN_MESSAGE);
	}

	private void run() throws IOException {
		try (Socket socket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT)) {

			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);

			name = getName();
			write(MessageType.NEWNAME.name() + "|" + name);
			tabbedPane.setTitleAt(0, "Public (" + name + ")");
			textField.requestFocus();

			while (true) {
				String line = input.readLine();
				if (line == null) {
					continue;
				}
				System.out.println(line);
				int delimiterLocation = line.indexOf("|") == -1 ? line.length() : line.indexOf("|");
				String messageType = line.substring(0, delimiterLocation);
				MessageType type = MessageType.valueOf(messageType);

				switch (type) {
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
						makePrivateChat(otherUser, false);
					}
					break;
				case PRIVATE_CHAT_RECEIVE:
					String typeRemoved = line.substring(line.indexOf("|") + 1);
					String senderRemoved = typeRemoved.substring(typeRemoved.indexOf("|") + 1);
					String message = senderRemoved.substring(senderRemoved.indexOf("|") + 1);
					String sender = typeRemoved.substring(0, typeRemoved.indexOf("|"));
					String receiver = senderRemoved.substring(0, senderRemoved.indexOf("|"));

					System.out.println(message + " " + sender + " " + receiver + " " + privateChatUsers);
					if (privateChatUsers.keySet().contains(sender)) {
						JTextArea textArea = privateChatUsers.get(sender);
						textArea.append(sender + ": " + message + "\n");
						Container panel = textArea.getParent().getParent().getParent();
						if (panel.hasFocus()) {
							System.out.println("TEST");
						}
						// if (tabbedPane.getSelectedComponent(). panel) {
						// System.out.println("TRUE");
						// }
					}
					break;
				case FILE_REQUEST: {
					// FILE_REQUEST|sender|receiver|fileName|bytes
					String fileTypeRemoved = line.substring(line.indexOf("|") + 1);
					String fileSenderRemoved = fileTypeRemoved.substring(fileTypeRemoved.indexOf("|") + 1);
					String fileReceiverRemoved = fileSenderRemoved.substring(fileSenderRemoved.indexOf("|") + 1);
					String fileSize = fileReceiverRemoved.substring(fileReceiverRemoved.indexOf("|") + 1);
					String fileName = fileReceiverRemoved.substring(0, fileReceiverRemoved.indexOf("|"));
					String fileSender = fileTypeRemoved.substring(0, fileTypeRemoved.indexOf("|"));
					String fileReceiver = fileSenderRemoved.substring(0, fileSenderRemoved.indexOf("|"));
					int result = JOptionPane.showConfirmDialog(frame,
							"Do you wish to accept " + fileName + " of " + fileSize + " bytes from " + fileSender, "File Accept",
							JOptionPane.YES_NO_OPTION);

					write(MessageType.FILE_OKAY + "|" + fileReceiver + "|" + fileSender + "|" + fileName + "|" + (result == 0 ? "YES" : "NO"));

					break;
				}
				case FILE_OKAY: {
					// FILE_OKAY|sender|receiver|fileName|result
					// should trigger file send
					String fileTypeRemoved = line.substring(line.indexOf("|") + 1);
					String fileSenderRemoved = fileTypeRemoved.substring(fileTypeRemoved.indexOf("|") + 1);
					String fileReceiverRemoved = fileSenderRemoved.substring(fileSenderRemoved.indexOf("|") + 1);
					String fileResult = fileReceiverRemoved.substring(fileReceiverRemoved.indexOf("|") + 1);
					String fileName = fileReceiverRemoved.substring(0, fileReceiverRemoved.indexOf("|"));
					String fileSender = fileTypeRemoved.substring(0, fileTypeRemoved.indexOf("|"));
					String fileReceiver = fileSenderRemoved.substring(0, fileSenderRemoved.indexOf("|"));
					if ("YES".equals(fileResult)) {
						try (Socket fileSocket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT)) {

							PrintWriter fileStringOut = new PrintWriter(fileSocket.getOutputStream(), true);
							String outMessage = MessageType.FILE_SEND + "|" + fileReceiver + "|" + fileSender + "|" + fileName + "|"
									+ fileToModelMap.get(fileName).getFile().length() + "\n";
							StringBuilder builder = new StringBuilder(outMessage);
							while (builder.length() < 8192) {
								builder.append("-");
							}
							fileStringOut.print(builder.toString());
							fileStringOut.flush();

							try (FileInputStream fileIn = new FileInputStream(fileToModelMap.get(fileName).getFile());
									DataOutputStream fileOut = new DataOutputStream(fileSocket.getOutputStream())) {
								byte[] buffer = new byte[16384];

								int count;
								while ((count = fileIn.read(buffer)) > 0) {
									fileOut.write(buffer, 0, count);
								}

								fileOut.flush();
							}
						}
					} else {
						fileToModelMap.remove(fileName);
					}
				}
					break;
				case FILE_RECEIVE: {
					String fileTypeRemoved = line.substring(line.indexOf("|") + 1);
					String fileSenderRemoved = fileTypeRemoved.substring(fileTypeRemoved.indexOf("|") + 1);
					String fileReceiverRemoved = fileSenderRemoved.substring(fileSenderRemoved.indexOf("|") + 1);
					String fileSize = fileReceiverRemoved.substring(fileReceiverRemoved.indexOf("|") + 1);
					String fileName = fileReceiverRemoved.substring(0, fileReceiverRemoved.indexOf("|"));
					String fileSender = fileTypeRemoved.substring(0, fileTypeRemoved.indexOf("|"));
					String fileReceiver = fileSenderRemoved.substring(0, fileSenderRemoved.indexOf("|"));
					if (Long.parseLong(fileSize) > 0) {
						// try (Socket fileSocket = new
						// Socket(Constants.SERVER_IP, Constants.SERVER_PORT)) {
						//
						// PrintWriter fileStringOut = new
						// PrintWriter(fileSocket.getOutputStream(), true);
						// String outMessage = MessageType.FILE_RECEIVE + "|" +
						// fileSender + "|" + fileReceiver + "|" + fileName +
						// "|" + fileSize + "\n";
						// StringBuilder builder = new
						// StringBuilder(outMessage);
						// while (builder.length() < 8192) {
						// builder.append("-");
						// }
						// fileStringOut.print(builder.toString());
						// fileStringOut.flush();

						Thread thread = new Thread(new Runnable() {

							@Override
							public void run() {
								try (Socket fileSocket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT)) {

									PrintWriter fileStringOut = new PrintWriter(fileSocket.getOutputStream(), true);
									String outMessage = MessageType.FILE_RECEIVE + "|" + fileSender + "|" + fileReceiver + "|" + fileName + "|" + fileSize
											+ "\n";
									StringBuilder builder = new StringBuilder(outMessage);
									while (builder.length() < 8192) {
										builder.append("-");
									}
									fileStringOut.print(builder.toString());
									fileStringOut.flush();
									try (DataInputStream fileIn = new DataInputStream(fileSocket.getInputStream());
											FileOutputStream fileOut = new FileOutputStream(new File(fileName))) {

										byte[] buffer = new byte[16384];
										int count;
										boolean cont = true;
										long totalBytes = 0;
										long start = System.currentTimeMillis();
										int mod = 5;
										while (cont) {
											System.out.println("nothing to read");
											while ((count = fileIn.read(buffer)) > 0) {
												fileOut.write(buffer, 0, count);
												totalBytes += count;
												cont = false;
												double completePercent = ((1.0 * totalBytes) / Long.parseLong(fileSize));
												long timeDiff = (System.currentTimeMillis() - start) / 1000;
												if (timeDiff == mod) {
													System.out.println(timeDiff + " tb " + totalBytes + " fs " + Long.parseLong(fileSize));
													privateChatUsers.get(fileReceiver).append(
															fileName + " from " + fileSender + " is " + ((int) (completePercent * 100)) + "% complete.\n");
													mod += 5;
												}
											}
											privateChatUsers.get(fileReceiver).append(fileName + " from " + fileSender + " is 100% complete.\n");
											fileOut.flush();
										}
										System.out.println("flushed");
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								} catch (UnknownHostException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						});
						thread.start();
					}

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
			if (lastIndex) {
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
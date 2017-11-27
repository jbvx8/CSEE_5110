package edu.umkc.csee5110.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.umkc.csee5110.MessageType;
import edu.umkc.csee5110.utils.Constants;

public class ChatServer {

	private static Map<String, PrintWriter> nameToWriter = new HashMap<>();
	private static Map<String, Socket> other = new HashMap<>();

	public static void main(String[] args) throws Exception {
		System.out.println("The server is listening at port " + Constants.SERVER_PORT);
		ServerSocket listener = new ServerSocket(Constants.SERVER_PORT);
		try {
			while (true) {
				new ThreadHandler(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}

	private static class ThreadHandler extends Thread {
		private String name;
		private Socket socket;
		// TODO: move into method

		public ThreadHandler(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {
				input.mark(1000);
				String in = input.readLine();

				System.out.println(in);
				do {
					if (in == null) {
						return;
					}
					if (in.startsWith(MessageType.FILE_SEND.name())) {
						String fileTypeRemoved = in.substring(in.indexOf("|") + 1);
						String fileSenderRemoved = fileTypeRemoved.substring(fileTypeRemoved.indexOf("|") + 1);
						String fileReceiverRemoved = fileSenderRemoved.substring(fileSenderRemoved.indexOf("|") + 1);
						String fileSize = fileReceiverRemoved.substring(fileReceiverRemoved.indexOf("|") + 1);
						String fileName = fileReceiverRemoved.substring(0, fileReceiverRemoved.indexOf("|"));
						String fileSender = fileTypeRemoved.substring(0, fileTypeRemoved.indexOf("|"));
						String fileReceiver = fileSenderRemoved.substring(0, fileSenderRemoved.indexOf("|"));
						input.reset();
						
						DataInputStream fileIn = new DataInputStream(socket.getInputStream());
						File file = new File(fileName);
						FileOutputStream fileOut = new FileOutputStream(file);
						byte[] buffer = new byte[16384];
						int count;
						while ((count = fileIn.read(buffer)) > 0) {
						  fileOut.write(buffer, 0, count);
						  System.out.println("Writing " + count + " " + Arrays.toString(buffer));
						}
						
						fileOut.flush();
						System.out.println("flushed");
						fileIn.close();
						fileOut.close();
						continue;
					}
					if (in.startsWith(MessageType.NEWNAME.name())) {
						name = in.substring(in.indexOf("|") + 1);
						if (name == null) {
							return;
						}
						if (nameToWriter.get(name) != null) {
							//send error
							continue;
						}
						output.println(MessageType.NAMEACCEPTED);
						System.out.println("Putting " + name + " with " + output);
						nameToWriter.put(name, output);

						for (Entry<String, PrintWriter> entry : nameToWriter.entrySet()) {
							for (String n : nameToWriter.keySet()) {
								entry.getValue().println(MessageType.ADD_NAME.name() + "|" + n);
								System.out.println("Sending ADD_NAME " + n + " to " + entry.getValue());
							}
						}
					}
					if (in.startsWith(MessageType.ADD_NAME.name())) {
						String name = in.substring(MessageType.ADD_NAME.name().length() + 1);
						for (Entry<String, PrintWriter> entry : nameToWriter.entrySet()) {
							for (String n : nameToWriter.keySet()) {
								if (!n.equalsIgnoreCase(name)) {
									entry.getValue().println(MessageType.ADD_NAME.name() + "|" + name);
								}
							}
						}
						continue;
					}

					for (Entry<String, PrintWriter> entry : nameToWriter.entrySet()) {
						if (in.startsWith(MessageType.PRIVATE_CHAT_INITIATE.name())) {
							String requestedPartner = in.substring(MessageType.PRIVATE_CHAT_INITIATE.name().length() + 1).trim();
							if (requestedPartner.equals(entry.getKey())) {
								entry.getValue().println(MessageType.PRIVATE_CHAT_INITIATE.name() + "|" + name);
								break;
							}
							continue;
						} else if (in.startsWith(MessageType.PRIVATE_CHAT_SEND.name())) {
							System.out.println("PCS " + in);
							String typeRemoved = in.substring(in.indexOf("|") + 1);
							String senderRemoved = typeRemoved.substring(typeRemoved.indexOf("|") + 1);
							String message = senderRemoved.substring(senderRemoved.indexOf("|") + 1);
							String sender = typeRemoved.substring(0, typeRemoved.indexOf("|"));
							String receiver = senderRemoved.substring(0, senderRemoved.indexOf("|"));
							System.out.println(message + " " + sender + " " + receiver);
							if (receiver.equals(entry.getKey())) {
								entry.getValue().println(MessageType.PRIVATE_CHAT_RECEIVE.name() + "|" + sender + "|" + receiver + "|" + message);
								break;
							}
							continue;
						} else if (in.startsWith(MessageType.FILE_INITIATE.name())) {
							// Send out FILE_REQUEST|sender|receier|fileName|size
							String fileTypeRemoved = in.substring(in.indexOf("|") + 1);
							String fileSenderRemoved = fileTypeRemoved.substring(fileTypeRemoved.indexOf("|") + 1);
							String fileReceiverRemoved = fileSenderRemoved.substring(fileSenderRemoved.indexOf("|") + 1);
							String fileSize = fileReceiverRemoved.substring(fileReceiverRemoved.indexOf("|") + 1);
							String fileName = fileReceiverRemoved.substring(0,  fileReceiverRemoved.indexOf("|"));
							String fileSender = fileTypeRemoved.substring(0,  fileTypeRemoved.indexOf("|"));
							String fileReceiver = fileSenderRemoved.substring(0, fileSenderRemoved.indexOf("|"));
							if (fileReceiver.equals(entry.getKey())) {
								entry.getValue().println(MessageType.FILE_REQUEST.name() + "|" + fileSender + "|" + fileReceiver + "|" + fileName + "|" + fileSize);
								System.out.println(fileName);	
							}
							// Get message from initiator.
							continue;
						} else if (in.startsWith(MessageType.FILE_OKAY.name())) {
							String fileTypeRemoved = in.substring(in.indexOf("|") + 1);
							String fileSenderRemoved = fileTypeRemoved.substring(fileTypeRemoved.indexOf("|") + 1);
							String receiver = fileSenderRemoved.substring(0, fileSenderRemoved.indexOf("|"));
							if (receiver.equals(entry.getKey())) {
								entry.getValue().println(in);
							}
							//System.out.println(receiver);
						} else if (in.startsWith(MessageType.FILE_SEND.name())) {
							// Get file stream from sender.
							continue;
						}
						if (in.startsWith("SEND_MESSAGE")) {
							entry.getValue().println(
									MessageType.RECEIVE_MESSAGE.name() + "|" + name + "|" + in.substring(MessageType.SEND_MESSAGE.name().length() + 1));
						}
					}
					in = input.readLine();
				} while (true);
			} catch (IOException e) {
				System.out.println(e);
			} finally {
				if (name != null) {
					nameToWriter.remove(name);
					for (Entry<String, PrintWriter> entry : nameToWriter.entrySet()) {
						entry.getValue().println(MessageType.REMOVE_NAME.name() + "|" + name);
						System.out.println(entry.getValue() + " removing " + name);
					}
				}
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println(e);
				}
			}
		}
	}
}

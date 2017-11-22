package edu.umkc.csee5110.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import edu.umkc.csee5110.utils.Constants;

public class ChatServer {
	
	//private static HashSet<String> names = new HashSet<>();
	//private static HashSet<PrintWriter> writers = new HashSet<>();
	private static Map<String, PrintWriter> nameToWriter = new HashMap<>();
	
	public static void main(String[] args) throws Exception {
		System.out.println("The server is listenting at port " + Constants.SERVER_PORT);
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
		//TODO: move into method
		private BufferedReader input;
		private PrintWriter output;
		
		public ThreadHandler(Socket socket) {
			this.socket = socket;
		}
		
		public void run() {
			//TODO: use try with resources
			try {
				input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				output = new PrintWriter(socket.getOutputStream(), true);
				
				while (true) {
					output.println("SUBMITNAME");
					name = input.readLine();
					if (name == null) {
						return;
					}
//					synchronized (names) {
//						if (!names.contains(name)) {
//							names.add(name);
//							break;
//						}
//					}
					if (nameToWriter.get(name) == null) {
						break;
					}
				}
//				for (String n : names) {
//					output.println("NAME " + n);
//				}
				
				output.println("NAMEACCEPTED");		
				//writers.add(output);
				System.out.println("Putting " + name + " with " + output);
				nameToWriter.put(name, output);
				
//				for (PrintWriter w : writers) {
//					for (String n : names) {
//						System.out.println(n);
//						w.println("NAME " + n);
//					}
//				}
				
				for (Entry<String, PrintWriter> entry : nameToWriter.entrySet()) {
					for (String n : nameToWriter.keySet()) {
						entry.getValue().println("NAME " + n);
						System.out.println("Sending NAME " + n + " to " + entry.getValue());
					}
				}
				
				while (true) {
					String in = input.readLine();
					if (in == null) {
						return;
					}
					if (in.startsWith("NEWNAME ")) {
						String name = in.substring(5);
						for (Entry<String, PrintWriter> entry : nameToWriter.entrySet()) {
							for (String n : nameToWriter.keySet()) {
								if (!n.equalsIgnoreCase(name)) {
									entry.getValue().println("NAME " + in.substring(5));
								}
							}
						}
						continue;
					}
					for (Entry<String, PrintWriter> entry : nameToWriter.entrySet()) {
						if (in.startsWith("PRIVATE_CHAT_INITIATE_")) {
							//System.out.println(in);
							String requestedPartner = in.substring("PRIVATE_CHAT_INITIATE_".length()).trim();
							//System.out.println(requestedPartner + " --- " + entry.getKey());
							if (requestedPartner.equals(entry.getKey())) {
								entry.getValue().println("PRIVATE_CHAT_INITIATE_" + name);
								break;
							}
							continue;
						} else if (in.startsWith("PRIVATE_CHAT_SEND_")) {
							String requestedPartner1 = in.substring("PRIVATE_CHAT_SEND_".length(), in.indexOf(" ")).trim();
							String sender = requestedPartner1.substring(0, requestedPartner1.indexOf("_"));
							String receiver = requestedPartner1.substring(requestedPartner1.lastIndexOf("_") + 1);
							System.out.println(in + " " + sender + " " + receiver);
							if (receiver.equals(entry.getKey())) {
								String message = in.substring(in.indexOf(" "));
								entry.getValue().println("PRIVATE_CHAT_RECEIVE_" + sender + "_" + receiver + " " + message);
								break;
							}
							continue;
						}
						//writer.println("MESSAGE " + name + ": " + in);
						entry.getValue().println("MESSAGE " + name + ": " + in);
					}
				}
			} catch (IOException e) {
				System.out.println(e);
			} finally {
//				if (name != null) {
//					names.remove(name);
//				}
				if (name != null) {
					//writers.remove(output);
					nameToWriter.remove(name);
					for (Entry<String, PrintWriter> entry : nameToWriter.entrySet()) {
						entry.getValue().println("REMOVENAME " + name);
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

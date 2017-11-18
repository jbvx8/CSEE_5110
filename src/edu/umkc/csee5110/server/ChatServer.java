package edu.umkc.csee5110.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

import edu.umkc.csee5110.utils.Constants;

public class ChatServer {
	
	private static HashSet<String> names = new HashSet<>();
	private static HashSet<PrintWriter> writers = new HashSet<>();
	
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
					synchronized (names) {
						if (!names.contains(name)) {
							names.add(name);
							break;
						}
					}
				}
				
				output.println("NAMEACCEPTED");
				writers.add(output);
				
				while (true) {
					String in = input.readLine();
					if (in == null) {
						return;
					}
					for (PrintWriter writer : writers) {
						writer.println("MESSAGE " + name + ": " + in);
					}
				}
			} catch (IOException e) {
				System.out.println(e);
			} finally {
				if (name != null) {
					names.remove(name);
				}
				if (output != null) {
					writers.remove(output);
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

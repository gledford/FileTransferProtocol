package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Server {
	private static final int MIN_PORT = 1024;
	private static final int MAX_PORT = 65535;
	private static final int INIT_VALUE = 177;
	private static final int MAX_BUFFER_SIZE = 4096;

	public static void main(String[] args) throws IOException {
		boolean hasClientReceivedThePort = false;
		int inputPortNumber = 9090;
		int portNumber = 0;
		Random randomPortGenerator = new Random();

		// TCP Connection to establish the UDP port number
		ServerSocket listener = new ServerSocket(inputPortNumber);
		try {
			while (!hasClientReceivedThePort) {
				Socket socket = listener.accept();
				BufferedReader input = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				String inputLine = input.readLine();
				if (Integer.parseInt(inputLine) == INIT_VALUE) {
					try {
						portNumber = randomPortGenerator
								.nextInt((MAX_PORT - MIN_PORT) + 1) + MIN_PORT;
						PrintWriter out = new PrintWriter(
								socket.getOutputStream(), true);
						out.println(portNumber);
					} finally {
						socket.close();
						System.out
								.println("Negotiation detected. Selected random port "
										+ portNumber + ".");
						hasClientReceivedThePort = true;
					}
				}
			}
		} finally {
			listener.close();
		}

		// UDP Connection
		try {
			boolean endOfTransmission = false;
			byte[] endPacket = new byte[MAX_BUFFER_SIZE];
			for (int ePacketIndex = 0; ePacketIndex < MAX_BUFFER_SIZE; ePacketIndex++) {
				endPacket[ePacketIndex] = 0;
			}
			DatagramSocket serverSocket = new DatagramSocket(portNumber);

			List<byte[]> receivedMessages = new ArrayList<byte[]>();

			int currentIndex = 0;
			while (!endOfTransmission) {
				receivedMessages.add(new byte[MAX_BUFFER_SIZE]);

				DatagramPacket receivePacket = new DatagramPacket(
						receivedMessages.get(currentIndex),
						receivedMessages.get(0).length);

				serverSocket.receive(receivePacket);

				String packet = new String(receivedMessages.get(currentIndex));
				
				if (Arrays.equals(endPacket, receivedMessages.get(currentIndex)))
					endOfTransmission = true;
				
				byte[] sendData = packet.toUpperCase().getBytes();

				// Ack
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length, receivePacket.getAddress(),
						receivePacket.getPort());

				currentIndex++;
				serverSocket.send(sendPacket);
			}

			serverSocket.close();
			
			// Create file string
			String fileString = "";
			for (int index = 0; index < receivedMessages.size(); index++) {
				fileString = fileString + new String(receivedMessages.get(index));
			}
			
			PrintWriter out = new PrintWriter("received.txt");
			out.println(fileString);

		} catch (SocketException ex) {
			System.out.println("UDP Port " + portNumber + " is occupied.");
			System.exit(1);
		}
	}
}

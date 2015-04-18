package com.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
	private static final int INIT_VALUE = 177;
	private static final int MAX_BUFFER_SIZE = 4096;

	public static void main(String[] args) throws IOException {
		if (args.length == 3) {
			String serverAddress = args[0];
			int tcpPortNumber = Integer.parseInt(args[1]); //TODO verify port is valid
			String fileName = args[2];
			
			File f = new File(fileName);
			if(f.exists() && !f.isDirectory()) {
				int udpPortNumber = getPortNumberFromClient(serverAddress, tcpPortNumber);
				sendFileToServer(udpPortNumber, serverAddress, fileName);
			}
			else {
				System.out.println("Invalid file " + fileName);
			}
		}
		else {
			System.out.println("Usage: java client <host1/server address> <n_port> <filename>");
		}
			
		System.exit(0);
	}

	private static int getPortNumberFromClient(String serverAddress, int tcpPortNumber)
			throws IOException {
		int portNumber = 0;
		Socket s = new Socket(serverAddress, tcpPortNumber);
		PrintWriter out = new PrintWriter(s.getOutputStream(), true);
		out.println("" + INIT_VALUE);
		BufferedReader input = new BufferedReader(new InputStreamReader(
				s.getInputStream()));
		String portNumberString = input.readLine();
		portNumber = Integer.parseInt(portNumberString);
		s.close();

		return portNumber;
	}

	private static void sendFileToServer(int portNumber, String serverAddress, String fileName)
			throws IOException {
		if (portNumber != 0) {
			DatagramSocket clientSocket = new DatagramSocket();

			InetAddress IPAddress = InetAddress.getByName(serverAddress);

			InputStream fileInputStream = Client.class
					.getResourceAsStream(fileName);

			byte[] file = new byte[fileInputStream.available()];
			fileInputStream.read(file);
			fileInputStream.close();

			List<byte[]> messages = new ArrayList<byte[]>();
			List<byte[]> acks = new ArrayList<byte[]>();

			int currentIndex = 0;
			int k = 0;
			for (int j = 0; j < file.length; j++) {
				if (messages.isEmpty() || k == MAX_BUFFER_SIZE) {
					messages.add(new byte[MAX_BUFFER_SIZE]);
					if (k == MAX_BUFFER_SIZE)
						currentIndex++;
					k = 0;
				}
				messages.get(currentIndex)[k] = file[j];
				k++;
			}

			int currentAckIndex = 0;
			for (int messageId = 0; messageId < messages.size(); messageId++) {
				DatagramPacket sendPacket = new DatagramPacket(
						messages.get(messageId),
						messages.get(messageId).length, IPAddress, portNumber);

				clientSocket.send(sendPacket);
				acks.add(new byte[MAX_BUFFER_SIZE]);
				DatagramPacket receivePacket = new DatagramPacket(
						acks.get(currentAckIndex),
						acks.get(currentAckIndex).length);
				clientSocket.receive(receivePacket);
				String sentence = new String(acks.get(currentAckIndex));
				sentence = sentence.replace("\n", "").replace("\r", "");

				System.out.println(sentence);
				currentAckIndex++;
			}

			// Send the end packet to terminate transmission
			byte[] endPacketData = new byte[MAX_BUFFER_SIZE];
			for(int ePacketIndex = 0; ePacketIndex < MAX_BUFFER_SIZE; ePacketIndex++) {
				endPacketData[ePacketIndex] = 0;
			}
			
			DatagramPacket endPacket = new DatagramPacket(
					endPacketData,
					endPacketData.length, IPAddress, portNumber);
			clientSocket.send(endPacket);

			clientSocket.close();
		}
	}
}

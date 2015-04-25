package com.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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

/**
 * Client class that established a TCP connection with the server
 * given the server address, port number, and file name. The TCP
 * connection is established and the server replies with the port
 * number to be used for the UDP file transfer. Once the port number
 * is receive, the Client breaks the input file down into multiple
 * byte arrays of a fixed size and sends a message and waits for a 
 * reply before sending the next message. Once the file has been
 * sent, an empty message is sent to terminate transmission.
 * 
 * @author gledford
 *
 */
public class Client {
	private static final int INIT_VALUE = 177;
	private static final int MAX_BUFFER_SIZE = 16;//4096;

	public static void main(String[] args) throws IOException {
		if (args.length == 3) {
			String serverAddress = args[0];
			int tcpPortNumber = Integer.parseInt(args[1]);
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

	/**
	 * This function sends a TCP packet to the server with the INIT_VALUE,
	 * then waits for the TCP packet reply with the port number to be used
	 * in the next UDP connection.
	 * 
	 * @param serverAddress
	 * @param tcpPortNumber
	 * @return
	 * @throws IOException
	 */
	private static int getPortNumberFromClient(String serverAddress, int tcpPortNumber)
			throws IOException {
		
		int portNumber = 0;
		Socket s = new Socket(serverAddress, tcpPortNumber);
		PrintWriter out = new PrintWriter(s.getOutputStream(), true);
		out.println("" + INIT_VALUE);
		
		System.out.println("Connected to " + s.getRemoteSocketAddress().toString());
		
		BufferedReader input = new BufferedReader(new InputStreamReader(
				s.getInputStream()));
		String portNumberString = input.readLine();
		portNumber = Integer.parseInt(portNumberString);
		s.close();

		System.out.println("Server responds with random port: " + portNumber);

		return portNumber;
	}

	/**
	 * This function establishes a UDP connection and puts the file
	 * into N packets of MAX_BUFFER_SIZE length. Each message is sent
	 * and waits for a reply from the server. To finalize the connection,
	 * an empty packet is sent.
	 * 
	 * @param portNumber
	 * @param serverAddress
	 * @param fileName
	 * @throws IOException
	 */
	private static void sendFileToServer(int portNumber, String serverAddress, String fileName)
			throws IOException {
		if (portNumber != 0) {
			DatagramSocket clientSocket = new DatagramSocket();

			InetAddress IPAddress = InetAddress.getByName(serverAddress);

			File f = new File(fileName);
			InputStream fileInputStream = new FileInputStream(f);
			
			// Create a byte array of the entire file
			byte[] file = new byte[fileInputStream.available()];
			fileInputStream.read(file);
			fileInputStream.close();

			List<byte[]> messages = new ArrayList<byte[]>();
			List<byte[]> acks = new ArrayList<byte[]>();

			// Break the file down into byte arrays
			int currentIndex = 0;
			int k = 0;
			for (int j = 0; j < file.length; j++) {
				if (messages.isEmpty() || k == MAX_BUFFER_SIZE) {
					if ((file.length - j) < MAX_BUFFER_SIZE) {
						messages.add(new byte[file.length - j + 1]);
					}
					else {
						messages.add(new byte[MAX_BUFFER_SIZE]);
					}
					if (k == MAX_BUFFER_SIZE)
						currentIndex++;
					k = 0;
				}
				messages.get(currentIndex)[k] = file[j];
				k++;
			}
			
			if (messages.get(messages.size() - 1).length != MAX_BUFFER_SIZE) {
				messages.get(messages.size() - 1)[messages.get(messages.size() - 1).length - 1] = Byte.MIN_VALUE;
			}
			

			// Send each packet and wait for the reply before sending the next packet
			int currentAckIndex = 0;
			for (int messageId = 0; messageId < messages.size(); messageId++) {
				// Send packet to Server
				DatagramPacket sendPacket = new DatagramPacket(
						messages.get(messageId),
						messages.get(messageId).length, IPAddress, portNumber);

				clientSocket.send(sendPacket);
				acks.add(new byte[messages.get(messageId).length]);
				
				// Wait for ACK from Server
				DatagramPacket receivePacket = new DatagramPacket(
						acks.get(currentAckIndex),
						acks.get(currentAckIndex).length);
				clientSocket.receive(receivePacket);
				
				String sentence = new String(acks.get(currentAckIndex));
				System.out.println(sentence);
				currentAckIndex++;
			}

			// Send the end packet to terminate transmission
			byte[] endPacketData = new byte[MAX_BUFFER_SIZE];
			for(int ePacketIndex = 0; ePacketIndex < MAX_BUFFER_SIZE; ePacketIndex++) {
				endPacketData[ePacketIndex] = Byte.MIN_VALUE;
			}
			
			DatagramPacket endPacket = new DatagramPacket(
					endPacketData,
					endPacketData.length, IPAddress, portNumber);
			clientSocket.send(endPacket);

			clientSocket.close();
		}
	}
}

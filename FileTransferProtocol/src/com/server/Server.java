package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Server class that established a TCP connection with the client given the
 * server address, port number, and file name. The TCP connection is established
 * and the client is assigned A port number to be used for the UDP file
 * transfer. Once the port number is sent, the server waits for the client to
 * send the UDP packets containing the file. Once the file has been sent, an
 * empty message is received to terminate transmission.
 * 
 * @author gledford
 *
 */
public class Server {
	private static final int MIN_PORT = 1024;
	private static final int MAX_PORT = 65535;
	private static final int INIT_VALUE = 177;
	private static final int MAX_BUFFER_SIZE = 4096;

	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			int tcpPortNumber = Integer.parseInt(args[0]);
			int portNumber = 0;

			portNumber = assignPortNumberToClient(tcpPortNumber);
			receiveDataFromClient(portNumber);
		}
	}

	/**
	 * This function waits for a client to request a port number. If the client
	 * sends a message with the the INIT_VALUE, the server assigns the random
	 * port number for the UDP connection back in the TCP packet.
	 * 
	 * @param tcpPortNumber
	 * @throws IOException
	 */
	private static int assignPortNumberToClient(int tcpPortNumber)
			throws IOException {

		boolean hasClientReceivedThePort = false;
		int portNumber = 0;
		Random randomPortGenerator = new Random();

		// TCP Connection to establish the UDP port number
		ServerSocket listener = new ServerSocket(tcpPortNumber);
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

		return portNumber;
	}

	/**
	 * This function waits on the input part number for a UDP packet and replies
	 * back with the data in all capital letters. The byte arrays in the packets
	 * are written to a file named received.txt.
	 * 
	 * @param portNumber
	 * @throws IOException
	 */
	private static void receiveDataFromClient(int portNumber)
			throws IOException {
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

				if (Arrays
						.equals(endPacket, receivedMessages.get(currentIndex)))
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
				fileString = fileString
						+ new String(receivedMessages.get(index));
			}

			// Write the received data to the file
			PrintWriter out = new PrintWriter("received.txt");
			out.println(fileString + "\n");
			out.close();

		} catch (SocketException ex) {
			System.out.println("UDP Port " + portNumber + " is occupied.");
			System.exit(1);
		}
	}
}

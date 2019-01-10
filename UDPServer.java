/**
 * UDPServer.java is a file to test out the receving server side
 * & how sockets works in using UDP and different port number 
 * in the server.
 *
 * Hou Jun-Ng (Bosh)
 * Student ID: 101912342
 * Class code: 4-21
 * 10.0.2 (a version number or a date)
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.*;
import java.lang.*;

class UDPServer
{
	private static boolean sendfile=false;
	private static boolean sendingLargePacket = false;
	private static ArrayList<String> sendArrays = new ArrayList<String>();
	private static CRC32 checksum = new CRC32();
	private static boolean sendAgain=false;
	
   public static void main() throws Exception
	{
	   	System.out.println("Author: Hou Jun Ng (Bosh)");
	   	System.out.println("UDPServer.java");
		System.out.println("");
	   	
		DatagramSocket serverSocket = new DatagramSocket(5000);
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		int bridgePort=4000;
		InetAddress IPAddress = InetAddress.getByName("localhost");
		
        while(true)
        {	
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, bridgePort);
			sendingLargePacket = false;
			long checksumServer = 0;
			long checksumReceive = 0;
			
			System.out.println("Author: Hou Jun Ng (Bosh)");
		   	System.out.println("UDPServer.java");
			System.out.println("");
			
			System.out.println("UDPServer Start");
			serverSocket.setSoTimeout(0);
			serverSocket.receive(receivePacket);
			IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
			
			String sentence = new String(receivePacket.getData());
			
			if(sentence.contains("textfile")) {
				System.out.println("Client request textfile");
				checksumReceive = extractCheckSum(sentence);
				try {
					sentence = sentence.substring(0, sentence.indexOf("|")-1);
				}
				catch(StringIndexOutOfBoundsException e) {
				}
				System.out.println("\nSrcPort: "+receivePacket.getPort()+" || DstPort: "+serverSocket.getLocalPort()+"\n"+
						"UDP Length: "+receivePacket.getLength()+"\n"+
						"RECEIVED: " + sentence +" || Checksum: "+checksumReceive);
				checksumServer = getCheckSum(sentence.getBytes());
				if(checksumReceive==checksumServer) {
					sendfile = true;
				}
				
				if(sendfile==true) {
					int i=0;
					sendData = new byte[128];
					BufferedReader br = new BufferedReader(new FileReader("./text.txt"),128);
					String sendArray = null;
					sendArrays.add("File Sending");
					while ((sendArray = br.readLine()) != null) 
			        {
						sendArrays.add(sendArray);
			        }
					br.close();
					while(sendfile==true) {
						String j = " ";
						String c = " ";
						checksum.reset();
						String databuffer;
						try {
							databuffer = sendArrays.get(i);
						}
						catch(IndexOutOfBoundsException e) {
							sendArrays.add(" ");
							databuffer = sendArrays.get(i);
						}
						System.out.println(i);
						while(i==10) {
							serverSocket.setSoTimeout(500);
							try {
								serverSocket.receive(receivePacket);
								String ACKfromClient = new String(receivePacket.getData());
								System.out.println("ACK Received: "+ACKfromClient);
								if(ACKfromClient.contains("EOT")) {
									sendfile=false;
									serverSocket.setSoTimeout(0);
									i = 0;
									continue;
								}
								else {
									System.out.println("NACK received send again");
									serverSocket.send(sendPacket);
								}
							}
							catch(SocketTimeoutException e) {
								System.out.println("ACK and NACK no receive send again");
								serverSocket.send(sendPacket);
							}
						}
						
						j = Integer.toString(i)+" ";
						databuffer = j.concat(databuffer);
						checksumServer = getCheckSum(databuffer.getBytes());
						databuffer = databuffer.concat(" || Checksum: "+checksumServer);
						sendData = databuffer.getBytes();
						
						sendPacket.setData(sendData); 
						serverSocket.send(sendPacket);
						System.out.println("File Sending: "+databuffer);
						serverSocket.setSoTimeout(500);
						try {
							serverSocket.receive(receivePacket);
							String ACKfromClient = new String(receivePacket.getData());
							System.out.println("ACK Received: "+ACKfromClient);
							if(ACKfromClient.contains("ACK0")) {
								if(i==9) {
									System.out.println("Finish Send");
									serverSocket.setSoTimeout(0);
									i++;
								}
								else{
									i++;
									System.out.println("Send Next Packet");
								}
								continue;
							}
							else if(ACKfromClient.contains("ACK1")) {
								int index = Integer.parseInt(ACKfromClient.substring(9,10));
								System.out.println("ACK1 Received");
								if(index != i) {
									System.out.println("Send Next Packet");
									serverSocket.send(sendPacket);
								}
								else if(i==9) {
									System.out.println("Finish Send");
									serverSocket.setSoTimeout(0);
									i++;
									continue;
								}
								else {
									i++;
									System.out.println("Send Next Packet");
									continue;
								}
							}
							else if(ACKfromClient.contains("NAK0")) {
								System.out.println("Send Again "+i+" Packet");
								sendAgain = true;
								serverSocket.send(sendPacket);
								while(sendAgain == true) {
									try {
										serverSocket.receive(receivePacket);
										ACKfromClient = new String(receivePacket.getData());
										System.out.println("ACK Received: "+ACKfromClient);
										if(ACKfromClient.contains("ACK0")||ACKfromClient.contains("ACK1")) {
											if(i==9) {
												System.out.println("Finish Send");
												serverSocket.setSoTimeout(0);
												i++;
												sendAgain=false;
											}
											else{
												System.out.println("Send Next Packet");
												i++;
												sendAgain=false;
											}
											continue;
										}
										if(ACKfromClient.contains("NAK0")) {
											System.out.println("NAK Received Send again");
											serverSocket.send(sendPacket);
											continue;
										}
										else if(ACKfromClient.contains("EOT")) {
											sendfile=false;
											serverSocket.setSoTimeout(0);
											sendAgain =false;
											continue;
										}
									}
									catch(SocketTimeoutException e) {
										System.out.println("ACK and NACK no receive send again");
										serverSocket.send(sendPacket);
									}
								}
							}
							else if(ACKfromClient.contains("EOT")) {
								sendfile=false;
								serverSocket.setSoTimeout(0);
							}
							continue;
						}
						catch (SocketTimeoutException e) {
							System.out.println("ACK and NACK no receive send again");
							serverSocket.send(sendPacket);
						}
					}
					continue;
				}
			}
			else if(sentence.contains("\\q")) {
				System.out.println("Server Stop");
				System.exit(0);
			}
			else {
				checksumReceive = extractCheckSum(sentence);
				try {
					sentence = sentence.substring(0, sentence.indexOf("|")-1);
				}
				catch(StringIndexOutOfBoundsException e) {
					continue;
				}
				System.out.println("\nSrcPort: "+receivePacket.getPort()+" || DstPort: "+serverSocket.getLocalPort()+"\n"+
						"UDP Length: "+receivePacket.getLength()+"\n"+
						"Seq Number: 0 || Receive Data: " + sentence +" || Checksum: "+checksumReceive);
				checksumServer = getCheckSum(sentence.getBytes());
				if(checksumReceive==checksumServer) {
					sendingLargePacket = true;
				}
				else if(checksumReceive!=checksumServer) {
					System.out.println("Send NAK0 to client");
					String ACKMessage = "Send NAK0 to client";
					sendData = ACKMessage.getBytes();
					sendPacket.setData(sendData);
					serverSocket.send(sendPacket);
					continue;
				}
				
				if(sendingLargePacket==true) {
					int i = 0;
					while(sendingLargePacket==true) {
						String j = " ";
						String c = " ";
						String databuffer = sentence;
						checksum.reset();
						System.out.println(i);
						while(i==10) {
							serverSocket.setSoTimeout(500);
							try {
								serverSocket.receive(receivePacket);
								String ACKfromClient = new String(receivePacket.getData());
								System.out.println("ACK Received: "+ACKfromClient);
								if(ACKfromClient.contains("EOT")) {
									sendingLargePacket=false;
									serverSocket.setSoTimeout(0);
									i = 0;
									continue;
								}
								else {
									System.out.println("NACK received send again");
									serverSocket.send(sendPacket);
								}
							}
							catch(SocketTimeoutException e) {
								System.out.println("ACK and NACK no receive send again");
								serverSocket.send(sendPacket);
							}
							
						}
						
						j = Integer.toString(i)+" ";
						databuffer = j.concat(databuffer.toUpperCase());
						checksumServer = getCheckSum(databuffer.getBytes());
						databuffer = databuffer.concat(" || Checksum: "+checksumServer);
						sendData = databuffer.getBytes();
						
						sendPacket.setData(sendData); 
						serverSocket.send(sendPacket);
						System.out.println("\nSrcPort: "+serverSocket.getLocalPort()+" || DstPort: "+sendPacket.getPort()+"\n"+
								"UDP Length: "+sendPacket.getLength()+"\n"+
								"Seq Number: "+i+" || Send Data: "+databuffer);
						serverSocket.setSoTimeout(500);
						
						try {
							serverSocket.receive(receivePacket);
							String ACKfromClient = new String(receivePacket.getData());
							System.out.println("ACK Received: "+ACKfromClient);
							if(ACKfromClient.contains("ACK0")) {
								if(i==9) {
									System.out.println("Finish Send");
									serverSocket.setSoTimeout(0);
									i++;
								}
								else{
									i++;
									System.out.println("Send Next Packet");
								}
								continue;
							}
							else if(ACKfromClient.contains("ACK1")) {
								int index = Integer.parseInt(ACKfromClient.substring(9,10));
								System.out.println("ACK1 Received");
								if(index != i) {
									System.out.println("Send Next Packet");
									serverSocket.send(sendPacket);
								}
								else if(i==9) {
									System.out.println("Finish Send");
									serverSocket.setSoTimeout(0);
									i++;
									continue;
								}
								else {
									i++;
									System.out.println("Send Next Packet");
									continue;
								}
							}
							else if(ACKfromClient.contains("NAK0")) {
								System.out.println("Send Again "+i+" Packet");
								sendAgain = true;
								serverSocket.send(sendPacket);
								while(sendAgain == true) {
									try {
										serverSocket.receive(receivePacket);
										ACKfromClient = new String(receivePacket.getData());
										System.out.println("ACK Received: "+ACKfromClient);
										if(ACKfromClient.contains("ACK0")||ACKfromClient.contains("ACK1")) {
											if(i==9) {
												System.out.println("Finish Send");
												serverSocket.setSoTimeout(0);
												i++;
												sendAgain=false;
											}
											else{
												System.out.println("Send Next Packet");
												i++;
												sendAgain=false;
											}
											continue;
										}
										else if(ACKfromClient.contains("NAK0")) {
											System.out.println("NACK Received Send again");
											serverSocket.send(sendPacket);
										}
										else if(ACKfromClient.contains("EOT")) {
											sendingLargePacket=false;
											sendAgain=false;
											serverSocket.setSoTimeout(0);
										}
										continue;
									}
									catch(SocketTimeoutException e) {
										System.out.println("ACK and NACK no receive send again");
										serverSocket.send(sendPacket);
									}
								}
							}
							else if(ACKfromClient.contains("EOT")) {
								sendingLargePacket=false;
								i = 0;
								serverSocket.setSoTimeout(0);
							}
							continue;
						}
						catch (SocketTimeoutException e) {
							System.out.println("ACK and NACK no receive send again");
							serverSocket.send(sendPacket);
						}
					}
					continue;
				}
			}
        }
			
    }
   public static long getCheckSum(byte[] sentence) {
	   checksum.reset();
	   checksum.update(sentence);
	   return checksum.getValue();
   }
   
   public static long extractCheckSum(String sentence) {
	   try {
		   Double d = Double.parseDouble(sentence.substring(sentence.indexOf(":")+2, sentence.length()-1));
		   return d.longValue();
	   }
	   catch(NumberFormatException e) {
		   return 0;
	   }
   }
}

/**
 * UDPClient.java is a file to send request to UDP server & manage 
 * how a sending port works in a UDP server.
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

class UDPClient
{
	private static byte[] receiveData = new byte[1024];
	private static byte[] sendData = new byte[1024];
	private static boolean sendAgain=false;
	private static boolean requestAgain=false;
	private static boolean sendfile=false;
	private static boolean recevingLargePacket = false;
	private static boolean nullDetect = true;
	private static boolean EOT = false;
	private static ArrayList<String> receiveArrays = new ArrayList<String>();
	private static CRC32 checksum = new CRC32();
	private static String modifiedSentence;
	
   public static void main() throws Exception
   {
	   	System.out.println("Author: Hou Jun Ng (Bosh)");
	   	System.out.println("UDPClient.java");
		System.out.println("");
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			while(true)
	        {
				String[] receivingArray = new String[10];
				
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 4000);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				receiveData = new byte[1024];
				sendData = new byte[1024];
				long checksumClient = 0;
				long checksumReceive = 0;
				int index = 0;
				sendfile = false;
				
				System.out.println("UDPClient Start");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String sentence = br.readLine();
				checksumClient = getCheckSum(sentence.getBytes());
				sentence = sentence.concat(" || Checksum: "+checksumClient);
				System.out.println("\nSrcPort: "+clientSocket.getLocalPort()+" || DstPort: "+sendPacket.getPort()+"\n"+
						"UDP Length: "+sendPacket.getLength()+"\n"+
						"Seq Number: "+index+" || Send Data: "+sentence);
				
				sendData = sentence.getBytes();
				sendPacket.setData(sendData);
				clientSocket.send(sendPacket);
				
				clientSocket.setSoTimeout(5000);
				try {
					clientSocket.receive(receivePacket);
					modifiedSentence = new String(receivePacket.getData());
					if(modifiedSentence.contains("NAK0")) {
						System.out.println("Receiving: "+modifiedSentence);
						System.out.println("Send First Packet Again");
						sendAgain = true;
						clientSocket.send(sendPacket);
						while(sendAgain == true) {
							try {
								clientSocket.receive(receivePacket);
								modifiedSentence = new String(receivePacket.getData());
								System.out.println("Receiving:"+modifiedSentence);
								if(modifiedSentence.contains("NAK0")) {
									System.out.println("NAK Received Send again");
									clientSocket.send(sendPacket);
								}
								else{
									sendAgain=false;
								}
								continue;
							}
							catch(SocketTimeoutException e) {
								System.out.println("Data and NAK no receive send again");
								clientSocket.send(sendPacket);
							}
						}
					}
				}
				catch(SocketTimeoutException e){
					System.out.println("Send First Packet Again");
					sendAgain = true;
					clientSocket.send(sendPacket);
					while(sendAgain == true) {
						try {
							clientSocket.receive(receivePacket);
							modifiedSentence = new String(receivePacket.getData());
							if(modifiedSentence.contains("NAK0")) {
								System.out.println("Receiving:"+modifiedSentence);
								System.out.println("NAK Received Send again");
								clientSocket.send(sendPacket);
							}
							else{
								sendAgain=false;
							}
							continue;
						}
						catch(SocketTimeoutException se) {
							System.out.println("Data and NAK no receive send again");
							clientSocket.send(sendPacket);
						}
					}
				}
				clientSocket.setSoTimeout(0);
				
				checksumReceive = extractCheckSum(modifiedSentence);
				modifiedSentence = modifiedSentence.substring(0, modifiedSentence.indexOf("|")-1);
				
				checksumClient = getCheckSum(modifiedSentence.getBytes());
				index = Integer.parseInt(modifiedSentence.substring(0,1));
				
				System.out.println("Receiving:"+modifiedSentence+" || Checksum: "+ checksumReceive);				
				if(checksumReceive == checksumClient && index==0 
						&& modifiedSentence.contains("File Sending")) {
					receivingArray[index]=modifiedSentence;
					String ACKMessage = "ACK0 for "+Integer.parseInt(modifiedSentence.substring(0,1))+"th String";
					System.out.println("Send ACK0: "+ACKMessage);
					sendData = ACKMessage.getBytes();
					sendPacket.setData(sendData);
					clientSocket.send(sendPacket);
					recevingLargePacket = true;
					sendfile=true;
					index++;
				}
				else if(checksumReceive == checksumClient && index==0 &&!modifiedSentence.contains("File Sending...")) {
					receivingArray[index]=modifiedSentence;
					String ACKMessage = "ACK0 for "+Integer.parseInt(modifiedSentence.substring(0,1))+"th String";
					System.out.println("Send ACK0: "+ACKMessage);
					sendData = ACKMessage.getBytes();
					sendPacket.setData(sendData);
					clientSocket.send(sendPacket);
					recevingLargePacket = true;
					index++;
				}
				else{
					System.out.println("Request Data Again for 0th index again");
					String requestSentence = "NAK0 for 0th String";
					sendData = requestSentence.getBytes();
					sendPacket.setData(sendData);
					clientSocket.send(sendPacket);
					requestAgain = true;
					
					while(requestAgain==true){
						clientSocket.receive(receivePacket);
						modifiedSentence = new String(receivePacket.getData());
						
						checksumReceive = extractCheckSum(modifiedSentence);
						modifiedSentence = modifiedSentence.substring(0, modifiedSentence.indexOf("|")-1);
						checksumClient = getCheckSum(modifiedSentence.getBytes());
						index = Integer.parseInt(modifiedSentence.substring(0,1));
						System.out.println("Receiving:"+modifiedSentence+" || Checksum: "+ checksumReceive);
						if(checksumReceive == checksumClient && index==0 
								&& modifiedSentence.contains("File Sending")) {
							receivingArray[index]=modifiedSentence;
							String ACKMessage = "ACK0 for "+Integer.parseInt(modifiedSentence.substring(0,1))+"th String";
							System.out.println("Send ACK0: "+ACKMessage);
							sendData = ACKMessage.getBytes();
							sendPacket.setData(sendData);
							clientSocket.send(sendPacket);
							requestAgain=false;
							recevingLargePacket = true;
							sendfile=true;
							index++;
						}
						else if(checksumReceive == checksumClient && index==0) {
							receivingArray[index]=modifiedSentence;
							String ACKMessage = "ACK0 for "+Integer.parseInt(modifiedSentence.substring(0,1))+"th String";
							System.out.println("Send ACK0: "+ACKMessage);
							sendData = ACKMessage.getBytes();
							sendPacket.setData(sendData);
							clientSocket.send(sendPacket);
							requestAgain=false;
							recevingLargePacket = true;
							index++;
						}
						else{
							System.out.println("Request Data Again for 0th index again");
							clientSocket.send(sendPacket);
						}
						continue;
					}
				}
				System.out.println(Arrays.toString(receivingArray));
				
				while(recevingLargePacket==true) {
					checksum.reset();
					int i=0;
					
					if(index==10) {
						String endOfTransmission = "EOT";
						System.out.println("Send End of Transmission");
						sendData = endOfTransmission.getBytes();
						sendPacket.setData(sendData);
						clientSocket.send(sendPacket);
						clientSocket.setSoTimeout(9000);
						EOT = true;
						while(EOT==true) {
							try {
								clientSocket.receive(receivePacket);
								modifiedSentence = new String(receivePacket.getData());
								System.out.println("Received: "+modifiedSentence);
								sendPacket.setData(sendData);
								clientSocket.send(sendPacket);
							}
							catch(SocketTimeoutException e) {
								System.out.println("No more packet receive.");
								recevingLargePacket=false;
								EOT=false;
								index = 0;
								clientSocket.setSoTimeout(0);
								continue;
							}
						}
						continue;
					}
					if(index<=9) {
						clientSocket.receive(receivePacket);
					}
					
					modifiedSentence = new String(receivePacket.getData());
					
					checksumReceive = extractCheckSum(modifiedSentence);
					modifiedSentence = modifiedSentence.substring(0, modifiedSentence.indexOf("|")-1);
					checksumClient = getCheckSum(modifiedSentence.getBytes());
					i = Integer.parseInt(modifiedSentence.substring(0,1));
					
					System.out.println("\nSrcPort: "+receivePacket.getPort()+" || DstPort: "+clientSocket.getLocalPort()+"\n"+
							"UDP Length: "+receivePacket.getLength()+"\n"+
							"Receive Data:"+modifiedSentence+" || Checksum: "+ checksumReceive);
					if(checksumReceive==checksumClient && index==i) {
						receivingArray[index]=modifiedSentence;
						String ACKMessage = "ACK0 for "+Integer.parseInt(modifiedSentence.substring(0,1))+"th String";
						System.out.println("Send ACK0: "+ACKMessage);
						sendData = ACKMessage.getBytes();
						sendPacket.setData(sendData);
						clientSocket.send(sendPacket);
						index++;
					}
					else if(checksumReceive!=checksumClient && index==i) {
						String ACKMessage = "NAK0 for "+index+"th String";
						System.out.println("Send NAK0: "+ACKMessage);
						sendData = ACKMessage.getBytes();
						sendPacket.setData(sendData);
						clientSocket.send(sendPacket);
						requestAgain = true;
						while(requestAgain==true){
							clientSocket.receive(receivePacket);
							modifiedSentence = new String(receivePacket.getData());
							
							checksumReceive = extractCheckSum(modifiedSentence);
							modifiedSentence = modifiedSentence.substring(0, modifiedSentence.indexOf("|")-1);
							checksumClient = getCheckSum(modifiedSentence.getBytes());
							index = Integer.parseInt(modifiedSentence.substring(0,1));
							System.out.println("Receiving:"+modifiedSentence+" || Checksum: "+ checksumReceive);
							
							if(checksumReceive == checksumClient && index==i) {
								receivingArray[index]=modifiedSentence;
								ACKMessage = "ACK0 for "+Integer.parseInt(modifiedSentence.substring(0,1))+"th String";
								System.out.println("Send ACK: "+sentence);
								sendData = ACKMessage.getBytes();
								sendPacket.setData(sendData);
								clientSocket.send(sendPacket);
								index++;
								requestAgain=false;
								recevingLargePacket = true;
								continue;
							}
							else{
								System.out.println("Received Fail Send Again");
								clientSocket.send(sendPacket);
							}
						}
					}
					else if(index!=i) {
						//databuffer and datalost
						for (int j=0; j<receivingArray.length; j++) {
							if(receivingArray[i] == null) {
								nullDetect = true;
							}
							else if (receivingArray[i] != null) {
								nullDetect = false;
							}
						}
						if(nullDetect==false) {
							String ACKMessage = "ACK1 for "+Integer.parseInt(modifiedSentence.substring(0,1))+"th String";
							System.out.println("Send ACK1:"+ ACKMessage);
							sendData = ACKMessage.getBytes();
							sendPacket.setData(sendData);
							clientSocket.send(sendPacket);
							continue;
						}
					}
					System.out.println(Arrays.toString(receivingArray));
				}
				if(sendfile==true) {
					try (PrintWriter out = new PrintWriter("./clienttext.txt")) 
					{
						for(String s: receivingArray)
						{
							out.println(s.substring(1,s.length()));
						}
					}
					sendfile=false;
				}
	        }
		}
		catch(SocketException e) {
			System.out.println(e);
		}
   }
   
   public static long getCheckSum(byte[] sentence) {
	   checksum.reset();
	   checksum.update(sentence);
	   return checksum.getValue();
   }
   
   public static long extractCheckSum(String sentence) {
	   Double d = Double.parseDouble(sentence.substring(sentence.indexOf(":")+2, sentence.length()-1));
	   return d.longValue();
   }
}

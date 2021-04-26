import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * TFTPServer - Multi-Threaded UDP server with start/stop button to control
 * server connections.
 * @author  Garrett Maury, Josh R, Alex R (RIP JOSH)
 * @version 4/9/2021
 */

public class TFTPServer extends Application implements EventHandler<ActionEvent>, TFTPConstants {
   // Window attributes
   private Stage stage;
   private Scene scene;
   private VBox root;
   
   // GUI Components
   private Label lblLog = new Label("Log:");
   private TextArea taLog = new TextArea();
   private Button btnStartStop = new Button("Start");
   private Button btnChooseFolder = new Button("Choose Folder");
   private Label lblServer = new Label("Start the server: ");
   private TextField dir = new TextField();
   
   // Socket stuff
   private ServerSocket sSocket = null;
   
   // Other stuff
   UDPServerThread serverThread = null;
   DatagramSocket mainSocket = null; //main socket
   
   /**
    * main program
    */
   public static void main(String[] args) {
      launch(args);
   }
   
   /**
    * Start, draw and set up GUI
    * Do server stuff
    */
   public void start(Stage _stage) {
      // Window setup
      stage = _stage;
      stage.setTitle("TFTPServer - IllegalSkillsException");
      stage.setOnCloseRequest(
         new EventHandler<WindowEvent>() {
            public void handle(WindowEvent evt) { System.exit(0); }
         });
      stage.setResizable(false);
      root = new VBox(8);
      
      // Start/Stop button
      FlowPane fpStart = new FlowPane(8,8);
      fpStart.setAlignment(Pos.BASELINE_RIGHT);
      fpStart.getChildren().addAll(lblServer, btnStartStop);
      btnStartStop.setOnAction(this);
      root.getChildren().add(fpStart);
   
      // LOG components
      FlowPane fpLog = new FlowPane(8,8);
      fpLog.setAlignment(Pos.CENTER);
      taLog.setPrefRowCount(10);
      taLog.setPrefColumnCount(35);
      dir.setPrefWidth(400); //directory width
      fpLog.getChildren().addAll(lblLog, taLog, btnChooseFolder, dir);
      root.getChildren().add(fpLog);
      
      btnStartStop.setOnAction(this);
      btnChooseFolder.setOnAction(this);
      
      dir.setText(System.getProperty("user.dir")); //make directory the current folder this file is in
      
      // Show window
      scene = new Scene(root, 525, 250);
      stage.setX(600);
      stage.setY(250);
      stage.setScene(scene);
      stage.show();      
   }
      
   // Start/Stop button
   public void handle(ActionEvent evt) {
      String label = ((Button)evt.getSource()).getText();
      switch(label) {
         case "Choose Folder":
            doChooseFolder();
            break;
         case "Start":
            doStart();
            break;
         case "Stop":
            doStop();
            break;
      }
   } 
   
   /** 
   * doChooseFolder()
   * Uses DirectoryChooser
   * changes the TextField directory to whatever the uses chooses it to be
   */
   public void doChooseFolder() {
      // Directory Chooser setup
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(new File(dir.getText())); //get current working directory
      File selectedDirectory = directoryChooser.showDialog(stage);
            
      if(selectedDirectory == null) {
         log("No directory chosen\n");
         //No Directory selected
      }
      else {
         dir.setText(selectedDirectory.getAbsolutePath()); // sets the textfield to the current directory
         log("Directory changed to " + selectedDirectory.getAbsolutePath() + "\n");
      }
   } //doChooseFolder()
   
   // Start method for the server threads
   public void doStart() {
      serverThread = new UDPServerThread();
      serverThread.start();
      log("Server Started!\n");
      btnStartStop.setText("Stop");
   }
   
   //Stop method
   public void doStop() {
      serverThread.stopServer();
      log("Server Stopped!\n");
      btnStartStop.setText("Start");
   }
   
   /** 
   * UDPServerThread
   * extends Thread
   * INNER CLASS
   * contains run(), which connects using the sockets and creates a thread for the client
   */
   class UDPServerThread extends Thread {
      public void run() {
         // Server stuff ... wait for a packet and process it
         try {
            // UDPServerThread is an inner class, inside the main GUI class that extends Application.
            // mainSocket is a DatagramSocket declared in the global scope and initialized to null
            
            mainSocket = new DatagramSocket(TFTP_PORT); // Binds the socket to the port
         }
         catch(IOException ioe) {
            log("IO Exception in UDPServerThread... (1): " + ioe + "\n");
            return;
         }
      
         // wait for a packet from a new client, then start a client thread
         while (true) {
            // Socket for the client
            // The socket for the client is created in the client thread
            byte[] holder = new byte[MAX_PACKET];
            
            // packet for 1st packet from a client
            DatagramPacket pkt = new DatagramPacket(holder, MAX_PACKET);
            
            try {
               // Wait for a connection and set up IO
               // We get a DatagramPacket, instead of a Socket, in the UDP case
               mainSocket.receive(pkt); // Wait for 1st packet
            }
            catch(IOException ioe) {
               // Happens when mainSocket is closed while waiting to receive - This is how we stop the server.
               return;
            }
            // Create a thread for the client
            // Instead of passing a Socket to the client thread, we pass the 1st packet
            UDPClientThread ct = new UDPClientThread(pkt);
            ct.start();
         
         } // of while loop
      } // of run     
      
      /** 
      * stopServer()
      * Stops the server
      * Stops any new incoming connections, clients already connected can continue to work
      */
      public void stopServer() {
         try {
            mainSocket.close();
         }
         catch(Exception e) {
            log("Exception has occurred... " + e + "\n");
         }
      } //stopServer()
      
   }  //UDPServerThread class

   
   /** 
   * UDPClientThread
   * extends Thread
   * INNER CLASS
   * contains a constructor which contains @param _pkt which achieves port switching
   * contains run(), creates a conversation with the client and uses the TFTP protocol
   */
   class UDPClientThread extends Thread {
      // Since attributes are per-object items, each ClientThread has its OWN socket, unique to that client
      private DatagramSocket cSocket = null;
      private DatagramPacket firstPkt = null;
      private int port = 0;
   
      // Constructor for ClientThread
      public UDPClientThread(DatagramPacket _pkt) {
         try {
            //cSocket = _cSocket;
            firstPkt = _pkt;
            // So - the new DatagramSocket is on a DIFFERENT port, chosen by the OS. If we use cSocket from now on, then port switching has been achieved.
            // in the parameter, put the new port
            port = firstPkt.getPort();
            cSocket = new DatagramSocket();
            log("New port: " + port + "\n");
         }
         catch(SocketException se) {
            log("SocketException in UDPClientThread... " + se + "\n");
         }
         catch(Exception e) {
            log("Exception in UDPClientThread..." + e + "\n");
         }
      } //constructor
   
      // main program for a ClientThread
      public void run() {
         // When a client connects
         log("Client connected\n");
      
         try {
            // In this try-catch run the protocol, using firstPkt as
            // the first packet in the conversation
         
            // Figure out if the incoming datagrampacket is RRQ or WRQ packet
            ByteArrayInputStream bais = new ByteArrayInputStream(firstPkt.getData(), firstPkt.getOffset(), firstPkt.getLength());
            DataInputStream dis = new DataInputStream(bais);
            int opcode = dis.readShort(); //read the opcode
            switch(opcode) {
            
               case RRQ:
                  log("First Packet is a Read Request!, opcode: " + opcode + "\n");
                  log("Using Port: " + port + " " + "\n");
                  doRRQ(firstPkt);
                  break;
                  
               case WRQ:
                  log("First Packet is a Write Request!, opcode: " + opcode + "\n");
                  doWRQ(firstPkt);
                  break;
                  
               default:
                  break;
            }
         } //try
         catch(IOException ioe) {
            log("IO Exception (3): " + ioe + "\n");
            // For TFTP, probably send an ERROR packet here
            return;
         }
      
         log("Client completed their task!\n");
      } //run()
      
      /** 
      * doRRQ()
      * TFTP Read Request
      * when given a read request from a client, uses TFTP protocol
      */
      public void doRRQ(DatagramPacket firstPkt) { // AKA DOWNLOAD
         //firstPkt.dissect(); //dissect the packet
         //cSocket.bind(new InetSocketAddress(#)); this is where we change the port
         
         // Attributes
         
         // Gets the IP address of the machine that sent the packet
         InetAddress toAddress = firstPkt.getAddress(); //get the address on a different port than 69
         
         RRQPacket rrqPkt = new RRQPacket();
         rrqPkt.dissect(firstPkt);
         
         String fileName = rrqPkt.getFileName();
         int blockNo = 1;
         byte[] data = new byte[512];
         
         int size = 0;
         
         DataInputStream dis = null; //make the streams
         try {
            File downFile = new File(dir.getText() + File.separator + fileName); //get the file in it's directory
            dis = new DataInputStream(new FileInputStream(downFile)); //open the file
         }
         catch(IOException ioe) {
            log("IOException occurred in doRRQ()... " + ioe + "\n");
         }
         
         boolean continueRRQ = true;
         while(continueRRQ) {
            size = 0;
            try {
               //read until end of file exception
               data = new byte[512];                   //set the data array to null
               for (int i = 0; i < data.length-1; i++) { //for all the data
                  data[i] = dis.readByte();              //read in the data
                  size++;
               }
                  
               DATAPacket secondPkt = new DATAPacket(toAddress, port, blockNo, data, size); //make the second packet
                  
               blockNo++; // Increment block number
                  
               //Sends the data packet and waits to receive the ACK Packet from the client
               log("Sending DATAPacket: blockNo: " + (blockNo-1) + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "  [3]" + data[3] + 
                  "  ...[" + (size -3) + "]" +  data[size -3 ] + "  [" + (size -2) + "]" + data[size -2 ] + "  [" + (size -1)  + "]" + data[size -1 ]
                     + "  [" + size + "]" + data[size] + "\n");
                     
               cSocket.send(secondPkt.build()); //send the second packet
               
               //receiving the ACK Packet from the client
               byte[] holder = new byte[MAX_PACKET];
               DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
               cSocket.receive(incoming);
               log("Received ACK Packet!" + "\n");
               readACKPacket(incoming, blockNo-1);
               
               if(size < 512) {
                  continueRRQ = false;
               }
               
            } //try
            catch(EOFException eofe) {
               try {
                  DATAPacket secondPkt = new DATAPacket(toAddress, port, blockNo, data, size);
                  dis.close(); //close the stream
                  //Sends the data packet and waits to receive the ACK Packet from the client
                  if (size >= 8) {
                     log("Sending DATAPacket: blockNo: " + (blockNo) + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "  [3]" + data[3] + 
                        "  ...[" + (size -3) + "]" +  data[size -3 ] + "  [" + (size -2) + "]" + data[size -2 ] + "  [" + (size -1)  + "]" + data[size -1 ]
                        + "  [" + size + "]" + data[size] + "\n");
                  }
                  else if (size >= 3) {
                     log("Sending DATAPacket: blockNo: " + (blockNo) + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "\n");
                  }
                  else {
                     log("Sending DATAPacket: blockNo: " + (blockNo) + " - [0]" + data[0] + "\n");  
                  }
               
                  cSocket.send(secondPkt.build()); //send the second packet
                  
                  //receiving the ACK Packet from the client 
                  byte[] holder = new byte[MAX_PACKET];
                  DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
                  cSocket.receive(incoming); //receive the incoming packet
                  log("Received ACK Packet!" + "\n");
                  readACKPacket(incoming, blockNo);
                  if(size < 512) {
                     continueRRQ = false;
                  }
               } //try
               catch(IOException ioe) {
                  try {
                     log("IOException..." + ioe + "\n");
                     ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 0, ioe.toString());
                     cSocket.send(errorPkt.build());
                     log("ERROR sent to client...\n");
                     continueRRQ = false;
                  }
                  catch(IOException ioe1) {
                     log("IOException 1 in doRRQ(): " + ioe1 + "\n");
                  }
               } //catch 2
               catch(Exception e) {
                  try {
                     log("Exception occurred..." + e + "\n");
                     ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 0, e.toString());
                     cSocket.send(errorPkt.build());
                     log("ERROR sent to client...\n");
                     continueRRQ = false;
                  }
                  catch(IOException ioe) {
                     log("IOException 2 in doRRQ(): " + ioe + "\n");
                  } //catch 3
                  
               } //catch 2
               
            } //catch eofe
            catch(IOException ioe) {
               try {
                  log("IOException occurred in doRRQ()..." + ioe + "\n");
                  ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 0, ioe.toString());
                  cSocket.send(errorPkt.build());
                  log("ERROR sent to client...\n");
                  continueRRQ = false;
               }
               catch(IOException ioe1) {
                  log("IOException 3 in doRRQ(): " + ioe1 + "\n");
               }
            } //catch
            catch(Exception e) {
               try {
                  log("Exception occurred in doRRQ()..." + e + "\n");
                  ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 0, e.toString());
                  cSocket.send(errorPkt.build());
                  log("ERROR sent to client...\n");
                  continueRRQ = false;
               }
               catch(IOException ioe) {
                  log("4 in doRRQ(): " + ioe + "\n");
               }
            } //catch
            
         } //while true
         
      } //doRRQ()
      
      /** 
      * doWRQ()
      * TFTP Write Request
      * when given a write request from a client, uses TFTP protocol
      */
      public void doWRQ(DatagramPacket firstPkt) {
         InetAddress toAddress = firstPkt.getAddress(); //get the address on a different port than 69
         
         WRQPacket wrqPkt = new WRQPacket();
         wrqPkt.dissect(firstPkt);
         
         String fileName = wrqPkt.getFileName();
         int blockNo = 0;
         int dataLen = 512;
         
         DataOutputStream dos = null;
         try{
            dos = new DataOutputStream(new FileOutputStream(dir.getText() + File.separator + wrqPkt.getFileName())); //dir.getText() + File.separator + fileName
         }catch(IOException ioe){
            System.out.println(ioe.toString());
         }
         
         boolean continueWRQ = true;
         while(continueWRQ){
            try{
               ACKPacket ackPkt = new ACKPacket(toAddress, port, blockNo);
               cSocket.send(ackPkt.build()); // PACKET 3
               log("Sent ACK Packet! Blk#: " + blockNo + "\n");
               
               if(dataLen < 511) {
                  continueWRQ = false;
                  break;
               }
               
               byte[] holder = new byte[MAX_PACKET];
               DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
               cSocket.receive(incoming); //receive the incoming packet
               
               // Figure out if the incoming datagrampacket is RRQ or WRQ packet
               ByteArrayInputStream bais = new ByteArrayInputStream(incoming.getData(), incoming.getOffset(), incoming.getLength());
               DataInputStream dis = new DataInputStream(bais);
               int opcode = dis.readShort();
            
               if (opcode == ERROR) { //opcode == 5
                  ERRORPacket errorPkt = new ERRORPacket();
                  errorPkt.dissect(incoming);
               
                  log("Error recieved from client:\n     [ERRORNUM:" + errorPkt.getErrorNo() + "] ... " + errorPkt.getErrorMsg() + "\n");
                  return;
               }
               else if (opcode == DATA) { //opcode == 3
               
                  DATAPacket dataPkt = new DATAPacket();
                  dataPkt.dissect(incoming);
                  
                  byte[] data = dataPkt.getData();
                  blockNo = dataPkt.getBlockNo();
                  int port = dataPkt.getPort();
                  dataLen = dataPkt.getDataLen();
                  log("DATAPacket: blockNo: " + blockNo + ", port: " + port + ", Length of Data: " + dataLen + "\n");
                  
                  try {
                  //write until end of file exception
                     for (int i = 0; i < data.length; i++) { //for all the data
                        dos.writeByte(data[i]);  //write the data
                     }
                        
                  } //try
                  catch(EOFException eofe) {
                     ACKPacket ackPkt1 = new ACKPacket(toAddress, port, blockNo);
                     cSocket.send(ackPkt1.build()); // PACKET 3
                     
                     log("Sent ACK Packet! Blk#: " + blockNo + "\n");
                     
                     if(dataLen < 511) {
                        continueWRQ = false;
                     }
                  
                  } //catch        
                
               } //else if opcode = DATA
            
               
               
            }catch(IOException ioe){}
         }
         
         log("Successfuly uploaded file..." + fileName + "\n");
      }
      
      /** 
      * getLength()
      * Length of data excluding any null values
      * @param byte[] data
      * @return the count of data of bytes that are not null
      */
      public int getLength(byte[] data) {
         int count = 0;
         for(byte element : data) {
            if (element != 0) {
               count++;
            }
         }
         return count;
      } //getLength()
      
      /** 
      * readACKPacket()
      * For reading the ACKPackets
      * @param pkt of DatagramPacket
      * @param blockNo the block number
      */
      public void readACKPacket(DatagramPacket pkt, int blockNo) {
         ACKPacket ackPkt = new ACKPacket();
         ackPkt.dissect(pkt);
         
         if (ackPkt.getOpCode() == ACK && ackPkt.getBlockNo() == blockNo) {
            //all good
            log("readACKPacket()..." + "Blk#: " + blockNo +  ", ACK!, all good." + "\n");
         }
      } //readACKPacket()
      
   } // End of inner class

   // utility method "log" to log a message in a thread safe manner
   private void log(String message) {
      Platform.runLater(
         new Runnable() {
            public void run() {
               taLog.appendText(message);
            }
         });
   } // of log
   
} //class TFTPServer
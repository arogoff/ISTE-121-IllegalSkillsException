import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;
import java.awt.Dimension;
import java.awt.Toolkit;

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
   
   // Screen size
   private Dimension size = Toolkit.getDefaultToolkit().getScreenSize(); // get screen size
   
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
      
      // On window close, close the program
      stage.setOnCloseRequest(
         new EventHandler<WindowEvent>() {
            public void handle(WindowEvent evt) {
               doStop();
               System.exit(0);
            }
         });
      
      dir.setText(System.getProperty("user.dir")); //make directory the current folder this file is in
      
      // Show window
      stage.setX((size.width / 2 - (525 / 2)) - 210); //offset the screen by 210
      stage.setY((size.height / 2 - (250 / 2)));
      scene = new Scene(root, 525, 250);
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
         log("No directory chosen\n"); //No Directory selected
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
      
      // Disable to ChooseFolder button and set the dir textfield to non-editable and disabled
      btnChooseFolder.setDisable(true);
      dir.setEditable(false);
      dir.setDisable(true);
   }
   
   //Stop method
   public void doStop() {
      //if the user exits without turning on the server
      if (serverThread == null) {
         System.exit(0);
      }
      serverThread.stopServer();
      log("Server Stopped!\n");
      btnStartStop.setText("Start");
      
      // Enable to ChooseFolder button and set the dir textfield to editable and non disabled
      btnChooseFolder.setDisable(false);
      dir.setEditable(true);
      dir.setDisable(false);
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
      private InetAddress toAddress = null;
   
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
            
               case RRQ: //read request
                  log("First Packet is a Read Request!, opcode: " + opcode + "\n");
                  log("Using Port: " + port + " " + "\n");
                  doRRQ(firstPkt);
                  break;
                  
               case WRQ: //write request
                  log("First Packet is a Write Request!, opcode: " + opcode + "\n");
                  doWRQ(firstPkt);
                  break;
                  
               default:
                  break;
            } //switch
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
         
         // Gets the IP address of the machine that sent the packet
         toAddress = firstPkt.getAddress(); // get the address on a different port than 69
         
         // Create an RRQPacket & Dissect the first packet
         RRQPacket rrqPkt = new RRQPacket();
         rrqPkt.dissect(firstPkt);
         
         // Attributes:
         String fileName = rrqPkt.getFileName(); // get the file name
         int blockNo = 1;
         byte[] data = new byte[512];
         int size = 0;
         
         DataInputStream dis = null; // make the streams
         try {
            File downFile = new File(dir.getText() + File.separator + fileName); // get the file in it's directory
            dis = new DataInputStream(new FileInputStream(downFile));            // open the file
         }
         catch(FileNotFoundException fnfe) {
            try {
               log("FileNotFoundException occurred in doRRQ()... Sending error packet! - " + fnfe + "\n");
               ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 1, fnfe.toString()); // build the error packet
               cSocket.send(errorPkt.build());                                             // send the error packet
               log("ERROR sent to client...\n");
            }
            catch(IOException ioe) {
               log("IOException occurred in doRRQ()... " + ioe + "\n");
            }
            
            return;
         } //catch fnfe
         
         boolean continueRRQ = true; // for loop control
         while(continueRRQ) {
            size = 0;
            try {
               //read until end of file exception
               data = new byte[512];                     // set the data array to null
               for (int i = 0; i < data.length-1; i++) { // for all the data
                  data[i] = dis.readByte();              // read in the data
                  size++;                                // increment the size
               }
                  
               DATAPacket secondPkt = new DATAPacket(toAddress, port, blockNo, data, size); //make the second packet
                  
               blockNo++; // Increment block number
                  
               // Sends the data packet and waits to receive the ACK Packet from the client
               log("Sending DATAPacket: blockNo: " + (blockNo-1) + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "  [3]" + data[3] + 
                  "  ...[" + (size -3) + "]" +  data[size -3 ] + "  [" + (size -2) + "]" + data[size -2 ] + "  [" + (size -1)  + "]" + data[size -1 ]
                     + "  [" + size + "]" + data[size] + "\n");
                     
               cSocket.send(secondPkt.build()); //send the second packet
               
               //receiving the ACK Packet from the client
               byte[] holder = new byte[MAX_PACKET];
               DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET); // make the incoming datagram packet
               cSocket.receive(incoming);                                        // receive the ACK Packet
               log("Received ACK Packet!" + "\n");                               // log the ack packet
               if(!readACKPacket(incoming, blockNo-1)) {
                  continueRRQ = false;
                  break;
               } 
               
               if(size < 511) {
                  continueRRQ = false; //if the data is less then 511, don't go through the loop anymore
               }
               
            } //try
            catch(EOFException eofe) {
               // INSIDE THE END OF FILE EXCEPTION
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
                  DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET); // make the incoming datagram packet
                  cSocket.receive(incoming);                                        // receive the incoming packet
                  log("Received ACK Packet!" + "\n");                               // log the ack packet
                  if(!readACKPacket(incoming, blockNo)) {
                     continueRRQ = false;
                     break;
                  }
                  if(size < 511) {
                     continueRRQ = false; //if the data is less then 511, don't go through the loop anymore
                  }
               } //try
               catch(IOException ioe) {
                  try {
                     log("IOException..." + ioe + "\n");                                         // IOException occurred..send error packet
                     ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 0, ioe.toString()); // build the error packet
                     cSocket.send(errorPkt.build());                                             // send the error packet
                     log("ERROR sent to client...\n");                                           // log the error
                     continueRRQ = false;                                                        // end the loop
                  }
                  catch(IOException ioe1) {
                     log("IOException 1 in doRRQ(): " + ioe1 + "\n");
                  }
               } //catch 2
               catch(Exception e) {
                  try {
                     log("Exception occurred..." + e + "\n");                                    // Exception occurred..send error packet
                     ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 0, e.toString());   // build the error packet
                     cSocket.send(errorPkt.build());                                             // send the error packet
                     log("ERROR sent to client...\n");                                           // log the error
                     continueRRQ = false;                                                        // end the loop
                  }
                  catch(IOException ioe) {
                     log("IOException 2 in doRRQ(): " + ioe + "\n");
                  } //catch 3
                  
               } //catch 2
               
            } //catch eofe
            catch(IOException ioe) {
               try {
                  log("IOException occurred in doRRQ()..." + ioe + "\n");                        // IOException occurred..send error packet
                  ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 0, ioe.toString());    // build the error packet
                  cSocket.send(errorPkt.build());                                                // send the error packet
                  log("ERROR sent to client...\n");                                              // log the error
                  continueRRQ = false;                                                           // end the loop
               }
               catch(IOException ioe1) {
                  log("IOException 3 in doRRQ(): " + ioe1 + "\n");
               }
            } //catch
            catch(Exception e) {
               try {
                  log("Exception occurred in doRRQ()..." + e + "\n");                            // Exception occurred..send error packet
                  ERRORPacket errorPkt = new ERRORPacket(toAddress, port, 0, e.toString());      // build the error packet
                  cSocket.send(errorPkt.build());                                                // send the error packet
                  log("ERROR sent to client...\n");                                              // log the error
                  continueRRQ = false;                                                           // end the loop
               }
               catch(IOException ioe) {
                  log("4 in doRRQ(): " + ioe + "\n");
               }
            } //catch
            
         } //while ContinueRRQ
         
      } //doRRQ()
      
      /** 
      * doWRQ()
      * TFTP Write Request
      * when given a write request from a client, uses TFTP protocol
      */
      public void doWRQ(DatagramPacket firstPkt) {
         toAddress = firstPkt.getAddress(); //get the address on a different port than 69
         
         // Dissect the first packet
         WRQPacket wrqPkt = new WRQPacket();
         wrqPkt.dissect(firstPkt);
         
         // Attributes:
         String fileName = wrqPkt.getFileName();
         int blockNo = 0;
         int dataLen = 512;
         
         //create the data output stream
         DataOutputStream dos = null;
         try {
            dos = new DataOutputStream(new FileOutputStream(dir.getText() + File.separator + wrqPkt.getFileName())); //dir.getText() + File.separator + fileName
         }
         catch(IOException ioe) {
            log("IOException occurred in doWRQ()... " + ioe + "\n");
         }
         
         boolean continueWRQ = true; //for the loop
         while(continueWRQ) {
            try {
               ACKPacket ackPkt = new ACKPacket(toAddress, port, blockNo); // make the ACKPacket
               cSocket.send(ackPkt.build()); // PACKET 3                      send it
               log("Sent ACK Packet! Blk#: " + blockNo + "\n");            // log it
               
               if(dataLen < 511) {
                  continueWRQ = false; //if the length of the data is less than 511, break the loop
                  break;
               }
               
               byte[] holder = new byte[MAX_PACKET];                             //create a byte holder with size of MAX_PACKET
               DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET); //create the incoming packet
               cSocket.receive(incoming);                                        //receive the incoming packet
               
               // create the streams: Byte Array Input Stream & Data Input Stream
               ByteArrayInputStream bais = new ByteArrayInputStream(incoming.getData(), incoming.getOffset(), incoming.getLength());
               DataInputStream dis = new DataInputStream(bais);
               int opcode = dis.readShort(); //read in the opcode
               
               //if the opcode is an error...
               if (opcode == ERROR) { //opcode == 5
                  ERRORPacket errorPkt = new ERRORPacket(); //make an error packet
                  errorPkt.dissect(incoming);               //disect the error packet
               
                  log("Error recieved from client:\n     [ERRORNUM:" + errorPkt.getErrorNo() + "] ... " + errorPkt.getErrorMsg() + "\n");
                  return;
               }
               else if (opcode == DATA) { //opcode == 3
               
                  DATAPacket dataPkt = new DATAPacket(); //create a DATAPacket
                  dataPkt.dissect(incoming);             //dissect the incoming data packet
                  
                  // GET ATTRIBUTES:
                  byte[] data = dataPkt.getData();
                  blockNo = dataPkt.getBlockNo();
                  int port = dataPkt.getPort();
                  dataLen = dataPkt.getDataLen();
                  log("DATAPacket: blockNo: " + blockNo + ", port: " + port + ", Length of Data: " + (dataLen + 1) + "\n"); //log the DATAPacket
                  
                  try {
                     //write until end of file exception
                     for (int i = 0; i < data.length; i++) { //for all the data
                        dos.writeByte(data[i]);  //write the data
                     }
                        
                  } //try
                  catch(EOFException eofe) {
                     ACKPacket ackPkt1 = new ACKPacket(toAddress, port, blockNo); //create the ACKPacket
                     cSocket.send(ackPkt1.build()); // PACKET 3                     send the pacekt
                     
                     log("Sent ACK Packet! Blk#: " + blockNo + "\n");             //log it
                     
                     if(dataLen < 511) {
                        continueWRQ = false; //if the length of the data is less than 511, set the loop to false
                     }
                  
                  } //catch        
                
               } //else if opcode = DATA
            
               
               
            } //try
            catch(IOException ioe){
               log("IOException occurred..." + ioe);
            }
            
         } //while continueWRQ
         
         log("Successfuly uploaded file..." + fileName + "\n");
      } //doWRQ()
      
      /** 
      * readACKPacket()
      * For reading the ACKPackets
      * @param pkt of DatagramPacket
      * @param blockNo the block number
      */
      public boolean readACKPacket(DatagramPacket pkt, int blockNo) {
         try {
            //create the streams: Byte Array Input Stream & Data Input Stream
            ByteArrayInputStream bais = new ByteArrayInputStream(pkt.getData(), pkt.getOffset(), pkt.getLength());
            DataInputStream dis = new DataInputStream(bais);
            int opcode = dis.readShort(); //read in the opcode
         
            //if the opcode is an ACKPacket..
            if (opcode == ACK) {
               ACKPacket ackPkt = new ACKPacket(); //create the ACKPacket
               ackPkt.dissect(pkt);                //dissect it
               
               if(ackPkt.getBlockNo() == blockNo) {
                  log("readACKPacket()..." + "Blk#: " + blockNo +  ", ACK!, all good." + "\n"); //all good
                  return true;
               }
               else {
                  log("Blk#'s don't match! Looking for: \"" + blockNo + "\". Recieved: \"" + ackPkt.getBlockNo() + "\".\n"); // Exception has occurred...send error packet
                  ERRORPacket errorPkt = new ERRORPacket(toAddress, pkt.getPort(), 0, "Blk#'s don't match! Looking for: \"" + blockNo + "\". Recieved: \"" + ackPkt.getBlockNo() + "\"."); // make the error packet
                  cSocket.send(errorPkt.build()); // send the error packet out
                  log("ERROR sent to client...\n");
               } //else
               
               return false;
            } //if opcode == ACK
            
            else if (opcode == ERROR) {
               ERRORPacket errorPkt = new ERRORPacket(); //create the ERRORPacket
               errorPkt.dissect(pkt);                    //dissect it
            
               //log the error
               log("Error recieved from server:\n     [ERRORNUM:" + errorPkt.getErrorNo() + "] ... " + errorPkt.getErrorMsg() + "\n");
               return false;
            } //else if opcode == ERROR
            
            else{
               log("Illegal Opcode! Looking for OPCODE-4 or OPCODE-5. Recieved: " + opcode + "\n"); // Exception has occurred...send error packet
               ERRORPacket errorPkt = new ERRORPacket(toAddress, pkt.getPort(), 4, "Illegal Opcode! Looking for OPCODE-4 or OPCODE-5. Recieved: " + opcode); // make the error packet
               cSocket.send(errorPkt.build()); // send the error packet out
               log("ERROR sent to client...\n");
            }
         
         } //try
         catch(Exception e) {
            log("Error occured in readACKPacket(): " + e + "\n");
            return false;
         }
         
         return false;
      
      } //readACKPacket()
      
   } // End of inner class
   
   /** 
   * log(String message)
   * @param gets the message to be appended to the text area
   * utility method "log" to log a message in a thread safe manner
   */
   private void log(String message) {
      Platform.runLater(
         new Runnable() {
            public void run() {
               taLog.appendText(message);
            }
         });
   } // of log
   
} //class TFTPServer
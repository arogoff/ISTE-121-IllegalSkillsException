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
      stage.setTitle("TFTPServer");
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
      
      // Show window
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
            // UDPServerThread is an inner class, inside the main
            // GUI class that extends Application.
            // mainSocket is a DatagramSocket declared in the global scope
            // and initialized to null
            mainSocket = new DatagramSocket(TFTP_PORT); // Binds the socket to the port
         }
         catch(IOException ioe) {
            log("IO Exception in UDPServerThread... (1): " + ioe + "\n");
            return;
         }
      
         // wait for a packet from a new client, then
         // start a client thread
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
               // Happens when mainSocket is closed while waiting
               // to receive - This is how we stop the server.
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
   
      // Constructor for ClientThread
      public UDPClientThread(DatagramPacket _pkt) {
         try {
            //cSocket = _cSocket;
            firstPkt = _pkt;
            // So - the new DatagramSocket is on a DIFFERENT port, chosen by the OS. If we use cSocket from now on, then port switching has been achieved.
            // in the parameter, put the new port
            cSocket = new DatagramSocket();
         }
         catch(SocketException se) {
         
         }
         catch(Exception e){}
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
            int opcode = dis.readShort();
            switch(opcode) {
               case RRQ:
                  log("First Packet is a Read Request!, opcode: " + opcode + "\n");
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
      
         log("Client completed!\n");
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
         
         DataInputStream dis = null; //make the streams
         try {
            dis = new DataInputStream(new FileInputStream(fileName)); //open the file
         
            //read until end of file exception
            while (true) {
               data = new byte[512]; //set the data array to null
               for (int i = 0; i < data.length; i++) { //for all the data
                  data[i] = dis.readByte();  //read in the data
               }
               
               DATAPacket secondPkt = new DATAPacket(toAddress, cSocket.getPort(), blockNo, data, getLength(data)); //make the second packet
               
               blockNo++; // Increment block number
               
               //Sends the data packet and waits to receive the ACK Packet from the client
               log("Sending DATAPacket: blockNo: " + (blockNo-1) + " - " + data[0] + "   " + data[1] + "   " + data[2] + "   " + data[3] + "   ..." + 
                  data[getLength(data) -3 ] + "   " + data[getLength(data) -2 ] + "   " + data[getLength(data) -1 ] + "   " + data[getLength(data)] + "\n");
                  
               cSocket.send(secondPkt.build()); //send the second packet
               
               //receiving the ACK Packet from the client
               byte[] holder = new byte[MAX_PACKET];
               DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
               cSocket.receive(incoming);
               log("Received ACK Packet!" + "\n");
               readACKPacket(incoming, blockNo--);
             
            } //while
            
         } //try
         catch(EOFException eofe) {
            try {
               DATAPacket secondPkt = new DATAPacket(toAddress, cSocket.getPort(), blockNo, data, getLength(data));
               dis.close(); //close the stream
               //Sends the data packet and waits to receive the ACK Packet from the client
               cSocket.send(secondPkt.build()); //send the second packet
               //receiving the ACK Packet from the client
                  
               byte[] holder = new byte[MAX_PACKET];
               DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
               cSocket.receive(incoming);
               readACKPacket(incoming, blockNo--);
            } //try
            catch(IOException ioe) {
               log("IOException..." + ioe + "\n");
               createERROR(toAddress, ioe.toString());
            }
            catch(Exception e) {
               log("Exception occurred..." + e + "\n");
               createERROR(toAddress, e.toString());
            }
         } //catch
         catch(Exception e) {
            log("Exception occurred in doRRQ()..." + e + "\n");
            createERROR(toAddress, e.toString());
         }
         
      } //doRRQ()
      
      public void createERROR(InetAddress toAddress, String msg){
         try{
            ERRORPacket errorPkt = new ERRORPacket(toAddress, cSocket.getPort(), 0, msg);
            cSocket.send(errorPkt.build());
            log("ERROR Packet Sent\n");
         }catch(IOException e){
            System.out.println(e.toString());
         }
      }
      
      /** 
      * doWRQ()
      * TFTP Write Request
      * when given a write request from a client, uses TFTP protocol
      */
      public void doWRQ(DatagramPacket firstPkt) {
         
      }
      
      /** 
      * getLength()
      * Length of data excluding any null values
      * @param byte[] data
      * @return the count of data of bytes that are not null
      */
      public int getLength(byte[] data){
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
            log("readACKPacket()... ACK!, all good\n");
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
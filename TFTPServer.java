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
   public static final int SERVER_PORT = 69;
   
   // Other stuff
   UDPServerThread serverThread = null;
   
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
            
      if(selectedDirectory == null){
                  //No Directory selected
      }else{
         dir.setText(selectedDirectory.getAbsolutePath()); // sets the textfield to the current directory
      }
   }
   
   // Start method for the server threads
   public void doStart() {
      UDPServerThread t1 = new UDPServerThread();
      //serverThread.start();
      btnStartStop.setText("Stop");
   }
   
   //Stop method
   public void doStop() {
      //UDPserverThread.stopServer();
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
         // Server stuff ... wait for a connection and process it
         // Server stuff ... wait for a packet and process it
         //try {
            // sSocket = new ServerSocket(SERVER_PORT);
            // UDPServerThread is an inner class, inside the main
            // GUI class that extends Application.
            // mainSocket is a DatagramSocket declared in the global scope
            // and initialized to null
            // mainSocket = new DatagramSocket(SERVER_PORT); // Binds the socket to the port
         //}
         /*catch(IOException ioe) {
            log("IO Exception (1): " + ioe + "\n");
            return;
         }*/
      
         // wait for a packet from a new client, then
         // start a client thread
         while (true) {
            // Socket for the client
            // Socket cSocket = null;
            // The socket for the client is created in the client thread
            // byte[] holder = new byte[MAX_PACKET];
            // packet for 1st packet from a client
            // DatagramPacket pkt = new DatagramPacket(holder, MAX_PACKET);
            //try {
               // Wait for a connection and set up IO
               // cSocket = sSocket.accept();
               // We get a DatagramPacket, instead of a Socket, in the UDP case
               //pkt = mainSocket.receive(); // Wait for 1st packet
            //}
            /*catch(IOException ioe) {
               // Happens when mainSocket is closed while waiting
               // to receive - This is how we stop the server.
               return;
            }*/
            // Create a thread for the client
            // UDPClientThread ct = new UDPClientThread(cSocket);
            // Instead of passing a Socket to the client thread, we pass the 1st packet
            // UDPClientThread ct = new UDPClientThread(pkt);
            // ct.start();
         
         } // of while loop
      } // of run   
   }  

   
   /** 
   * UDPClientThread
   * extends Thread
   * INNER CLASS
   * contains a constructor which contains @param _pkt which achieves port switching
   * contains run(), creates a conversation with the client and uses the TFTP protocol
   */
   class UDPClientThread extends Thread {
      // Since attributes are per-object items, each ClientThread has its OWN
      // socket, unique to that client
      private Socket cSocket;
      //private DatagramSocket cSocket = null;
      private DatagramPacket firstPkt = null;
   
      // Constructor for ClientThread
      public UDPClientThread(DatagramPacket _pkt) {
         //cSocket = _cSocket;
         firstPkt = _pkt;
         // So - the new DatagramSocket is on a DIFFERENT port,
         // chosen by the OS. If we use cSocket from now on, then
         // port switching has been achieved.
         // cSocket = new DatagramSocket();
      }
   
      // main program for a ClientThread
      public void run() {
         // log("Client connected!\n");
         log("Client packet received!\n");
      
         //try {
            // In this try-catch run the protocol, using firstPkt as
            // the first packet in the conversation
         //}
         /*catch(IOException ioe) {
            log("IO Exception (3): " + ioe + "\n");
            // For TFTP, probably send an ERROR packet here
            return;
         }*/
      
         // As the conversation progresses, to receive a packet:
         // byte[] holder = new holder[MAX_PACKET];
         // DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
         // cSocket.receive(incoming);
         // Then - dissect the incoming packet and process it
         // THE NEXT SET OF NOTES DISCUSSES HOW TO DISSECT PACKETS
      
         // To send a packet:
         // Compute the contents of the outgoing packet
         // Build the packet ... producing a DatagramPacket, outgoing
         // cSocket.send(outgoing);
         // THE NEXT SET OF NOTES ALSO DISCUSSES HOW TO BUILD PACKETS
      
         // log("Client disconnected!\n");
         log("Client completed!\n");
      }
      
      /** 
      * doWRQ()
      * TFTP Write Request
      * when given a write request from a client, uses TFTP protocol
      */
      public void doWRQ() {
      
      }
      
      /** 
      * doRRQ()
      * TFTP Read Request
      * when given a read request from a client, uses TFTP protocol
      */
      public void doRRQ() {
      
      }
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
import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.geometry.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.concurrent.*;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * TFTPClient - Sends 
 * No Connect/Disconnect button - connectionless.
 * @author  Garrett Maury, Josh R, Alex R (RIP JOSH)
 * @version 4/9/2021
 */

public class TFTPClient extends Application implements EventHandler<ActionEvent>, TFTPConstants {
   // Window attributes
   private Stage stage;
   private Scene scene;
   private VBox root;
   
   // These are for Row1
   private Label lblServerIP = new Label("Server Name or IP: ");
   private TextField tfServerIP = new TextField();

   // These will be in Row2
   private Button btnChooseFolder = new Button("Choose Folder");
   private TextField tfDirectory = new TextField();
   
   // Components - CENTER
   private Button btnDownload = new Button("Download");
   private Button btnUpload = new Button("Upload");
   private Label lblLog = new Label("Log:");
   private TextArea taLog = new TextArea();

   // IO attributes
   private DatagramSocket socket = null;
   private InetAddress serverIP = null;
   
   // Screen stuff
   private Dimension size = Toolkit.getDefaultToolkit().getScreenSize(); // get screen size
   private double width = size.width;
   private double height = size.height;
   
   // Client Thread stuff
   ClientThread ct;  
   String cmd = null;
   
   String fileName, clientFileName;
   DataInputStream dis;
   File fileTo = null;
   
   // Progress bar - BOT
   private Label pbProgress = new Label("Update Bar: ");
   private Label pbPercent = new Label("0");
   private ProgressBar pbBar = new ProgressBar();
   private int totalSize = 0; //for the progress bar

   /**
    * main program 
    */
   public static void main(String[] args) {
      launch(args);
   }

   /**
    * start - draw and set up GUI
    */
   public void start(Stage _stage) {
      stage = _stage;
      stage.setTitle("TFTPClient - IllegalSkillsException");
      stage.setOnCloseRequest(
         new EventHandler<WindowEvent>() {
            public void handle(WindowEvent evt) { System.exit(0); }
         });
      stage.setResizable(false);
      root = new VBox(8);
      
      // ROW1 - FlowPane
      FlowPane fpRow1 = new FlowPane(8,8);
      fpRow1.setAlignment(Pos.CENTER);
      tfServerIP.setPrefColumnCount(15);
      fpRow1.getChildren().addAll(lblServerIP, tfServerIP);
      root.getChildren().add(fpRow1);
   
      // ROW2 - Textfield for a sentence to send and Send button
      FlowPane fpRow2 = new FlowPane(8,8);
      fpRow2.setAlignment(Pos.CENTER);
      fpRow2.getChildren().addAll(btnChooseFolder, tfDirectory);
      root.getChildren().add(fpRow2);
      
      // Buttons - Upload and Download
      FlowPane fpbtns = new FlowPane();
      fpbtns.setAlignment(Pos.CENTER);
      fpbtns.getChildren().addAll(btnUpload, btnDownload);
      root.getChildren().add(fpbtns);
      
      // LOG - Label + text area
      FlowPane fpLog = new FlowPane();
      fpLog.setAlignment(Pos.CENTER);
      taLog.setPrefColumnCount(35);
      taLog.setPrefRowCount(10);
      tfDirectory.setPrefColumnCount(25);
      fpLog.getChildren().addAll(lblLog, taLog);
      root.getChildren().add(fpLog);
      
      // BOTTOM - Progress Bar
      StackPane sPane = new StackPane();
      FlowPane fpBot = new FlowPane();
      fpBot.setAlignment(Pos.CENTER);
      fpBot.getChildren().addAll(pbProgress);
      sPane.getChildren().addAll(pbBar, pbPercent);
      root.getChildren().addAll(fpBot, sPane);
      
      tfDirectory.setText(System.getProperty("user.dir")); //make directory the current folder this file is in
      
      btnChooseFolder.setOnAction(this);
      btnUpload.setOnAction(this);
      btnDownload.setOnAction(this);
      
      // Setup the screen
      stage.setX((size.width / 2 - (475 / 2)) + 310);  //offset the screen by 310
      stage.setY(size.height / 2 - (340 / 2));
      
      scene = new Scene(root, 475, 340);
      stage.setScene(scene);
      stage.show();      
      
      // Open a DatagramSocket for IO
      try {
         socket = new DatagramSocket();
      }
      catch(SocketException se) {
         Alert alert = new Alert(AlertType.ERROR, "Cannot create socket: " + se);
         alert.setHeaderText("Socket Failure");
         alert.showAndWait();
         System.exit(1);
      }
   } //start
   
   /** 
    * Button dispatcher
    */
   public void handle(ActionEvent ae) {
      String label = ((Button)ae.getSource()).getText();
      
      //depending on what button the user chooses...
      switch(label) {
         case "Choose Folder":
            doChooseFolder();
            break;
         case "Upload":
            pbBar.setProgress(0);
            pbPercent.setText("0");
               
               //make a filechooser for choosing file to upload
            FileChooser chooserWindow = new FileChooser(); //make the file chooser appear
            chooserWindow.setInitialDirectory(new File(tfDirectory.getText()));
            chooserWindow.setTitle("Choose the Local File to Upload");
            chooserWindow.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
            fileTo = chooserWindow.showOpenDialog(stage); //make the save dialog appear
            this.clientFileName = fileTo.getName();
               
               //make a textinputdialog for selecting the name for the file
            TextInputDialog input = new TextInputDialog();
            input.setHeaderText("Enter the name to file on the server for saving the upload");
            input.setTitle("Remote Name");
            input.setX((width / 2 - (475 / 2)) + 310); //set the textinputdialog ontop of the client
            input.setY(height / 2 - (315 / 2));
            input.showAndWait();
               
            fileName = input.getEditor().getText();
               
               //if the user did not select a file to upload
            if (fileTo == null) {
               log("You did not choose a file to upload... canceling upload.\n");
               doDisconnect();
               return;
            }
                  //if the user did put in a file
            else {
               try{
                  dis = new DataInputStream(new FileInputStream(fileTo)); //open the file, clear it's contents
               }
               catch(FileNotFoundException fnfe){
                  log("File Not Found..." + fnfe);
                  return;
               }
            }
            ct = new ClientThread(label);
            ct.start();
            break;
         case "Download":
            ct = new ClientThread(label);
            ct.start();
            break;
      }
   }   

   
   private void log(String message) {
      Platform.runLater(
         new Runnable() {
            public void run() {
               taLog.appendText(message);
            }
         });
   } // of log
   
    /** 
   * doChooseFolder()
   * uses DirectoryChooser
   * updates tfSentence with the new directory that the user chose
   */
   public void doChooseFolder() {
   
            // Directory Chooser setup
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(new File(tfDirectory.getText())); //get current working directory
      File selectedDirectory = directoryChooser.showDialog(stage);                    //show that directory
            
      if(selectedDirectory == null) {
               //No Directory selected
         log("No directory selected!\n");
      }
      else{
         tfDirectory.setText(selectedDirectory.getAbsolutePath()); // sets the textfield to the current directory
         log("Directory changed to " + selectedDirectory.getAbsolutePath() + "\n");
      }
            
              
   } //doChooseFolder()
   
   /** 
   * doConnect()
   * 
   * Connects to server using TFTP_PORT
   */
   public void doConnect() {
      String ip = "";
            //if the user does not put a IP in, default to localhost
      if(tfServerIP.getText().equals("")) {
         ip = "localhost";
      }
      else {
         ip = tfServerIP.getText(); //ip is what's in the textfield that the user has put in
      }
            
      try {
         serverIP = InetAddress.getByName(ip);
               
         socket = new DatagramSocket(); //create a socket
         socket.setSoTimeout(1000);     //set a timeout
      } //try
      catch (Exception e) {
         log("Connection failed..." + e + "\n");
         return;
      } // catch
               
   } //doConnect()
   
   /** 
   * doDisconnect()
   * 
   * Disconnects from server
   */
   public void doDisconnect() {
   
      try {
         socket.close();
         log("Disconnecting from the server...\n");
      }
      catch(Exception e){}
            
      socket = null; //set the socket to null
   
   }
   
   /** 
   * doUpload()
   * uses FileChooser
   * uploads a file to the server using the TFTP protocol
   */
   public void doUpload() {
   
      // ATTRIBUTES:
      int blockNo = 0;
      byte[] data = new byte[512];
      int port = -1;
      boolean continueLoop = true;
      int size = 0;
      totalSize = 0; //for more than 1 uploads
            
      try {
               // Connect to server
         doConnect();
         //dis = new DataInputStream(new FileInputStream(fileTo)); //open the file, clear it's contents       
               
               //InetAddress _toAddress, int _port, String _fileName, String _mode
         WRQPacket wrqPkt = new WRQPacket(serverIP, TFTP_PORT, fileName, "octet"); //make a WRQPacket
         socket.send(wrqPkt.build()); //PACKET 1                                     send it out
               
               //LOOP START HERE
         while(continueLoop) {
            byte[] holder = new byte[MAX_PACKET];                             //create a holder of byte array which the size is MAX_PACKET
            DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET); //make the incoming datagram packet
            socket.receive(incoming); //PACKET 2                                receive the packet
                  
            port = incoming.getPort(); //get the port
                  
            if(readACKPacket(incoming, blockNo)) {       // if true then correct
               data = new byte[512];                     // clear byte[] 
               size = 0;
               for (int i = 0; i < data.length-1; i++) { //for all the data
                  data[i] = dis.readByte();              //read in the data
                  size++;
                  totalSize++;                           //increment every byte no matter what block
                        
                  final int temp = totalSize; //final for platform.runlater
                  double value = ((int)totalSize / (double)fileTo.length()) * 100;
                        
                        //thread safe platform.runlater
                  Platform.runLater(
                              new Runnable() { 
                                 public void run() {
                                    pbBar.setProgress((int)temp / (double)fileTo.length());
                                    pbPercent.setText(String.valueOf((int)value));
                                 }
                              });
               } //for
                     
               blockNo++; // Increment block number
                     
               DATAPacket secondPkt = new DATAPacket(serverIP, port, blockNo, data, size); //make the second packet
                     
                     //Sends the data packet and waits to receive the ACK Packet from the client
               log("Sending DATAPacket: blockNo: " + blockNo + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "  [3]" + data[3] + 
                           "  ...[" + (size -3) + "]" +  data[size -3 ] + "  [" + (size -2) + "]" + data[size -2 ] + "  [" + (size -1)  + "]" + data[size -1 ]
                           + "  [" + size + "]" + data[size] + "\n");
                     
               socket.send(secondPkt.build()); //send the second packet
                     
               if(size < 511) {
                  continueLoop = false; //if the size is less than 511, end the loop
               } //if
                     
            } //if readACKPacket...
                  
         } //while continueLoop
               
      } //try
      catch(SocketTimeoutException ste) { //if there is a timeout
         log("Download timed out waiting for ACK!\n");
         doDisconnect(); //disconnect from the server
         return;
      } //catch
      catch(EOFException eofe) {
               //ON END OF FILE EXCEPTION...
         try {
            blockNo++; //increment the block number
                  
            DATAPacket secondPkt = new DATAPacket(serverIP, port, blockNo, data, size); //create the datapacket
            dis.close();                                                                //close the stream
                  
                  //Sends the data packet and waits to receive the ACK Packet from the client
            if (size >= 8) {
               log("Sending DATAPacket: blockNo: " + blockNo + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "  [3]" + data[3] + 
                           "  ...[" + (size -3) + "]" +  data[size -3 ] + "  [" + (size -2) + "]" + data[size -2 ] + "  [" + (size -1)  + "]" + data[size -1 ]
                           + "  [" + size + "]" + data[size] + "\n");
            }
            else if (size >= 3) {
               log("Sending DATAPacket: blockNo: " + blockNo + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "\n");
            }
            else {
               log("Sending DATAPacket: blockNo: " + blockNo + " - [0]" + data[0] + "\n");  
            }
                  
            socket.send(secondPkt.build()); //send the second packet
                  
                  //receiving the ACK Packet from the client 
            byte[] holder = new byte[MAX_PACKET];                             // create a holder of byte array with size of MAX_PACKET
            DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET); // create the incoming datagram packet
            socket.receive(incoming);                                         // receive the incoming packet
                  
            readACKPacket(incoming, blockNo); //read the ACKPacket
            if(size < 511) {
               continueLoop = false; //if the size is less than 511, end the loop
            }
         } //try
         catch(FileNotFoundException fnfe){
            try {
               log("FileNotFoundException occurred in doRRQ()... Sending error packet! - " + fnfe + "\n");
               ERRORPacket errorPkt = new ERRORPacket(serverIP, port, 1, fnfe.toString()); // build the error packet
               socket.send(errorPkt.build());                                             // send the error packet
               log("ERROR sent to Server...\n");
               return;                                                      // exit the loop
            }
            catch(IOException ioe1) {
               log("IOException 0 in doRRQ(): " + ioe1 + "\n");
            }
         }
         catch(IOException ioe) {
            try {
               log("IOException..." + ioe + "\n");                           // IOException has occurred...send error packet
               ERRORPacket errorPkt = new ERRORPacket(serverIP, port, 0, ioe.toString()); // make the error packet
               socket.send(errorPkt.build());                                             // send the error packet out
               log("ERROR sent to client...\n");                             // log it
               continueLoop = false;                                                      // exit the loop
            }
            catch(IOException ioe1) {
               log("IOException 1 in doRRQ(): " + ioe1 + "\n");
            }
         } //catch 2
         catch(Exception e) {
            try {
               log("Exception occurred..." + e + "\n");                      // Exception has occurred...send error packet
               ERRORPacket errorPkt = new ERRORPacket(serverIP, port, 0, e.toString());   // make the error packet
               socket.send(errorPkt.build());                                             // send the error packet out
               log("ERROR sent to client...\n");                             // log it
               continueLoop = false;                                                      // exit the loop
            }
            catch(IOException ioe) {
               log("IOException 2 in doRRQ(): " + ioe + "\n");
            } //catch 3
                  
         } //catch 2
               
      } //END OF FILE EXCEPTION catch
               
      catch(FileNotFoundException fnfe) {
         try {
            log("FileNotFoundException occurred in doRRQ() (2)... Sending error packet! - " + fnfe + "\n");
            ERRORPacket errorPkt = new ERRORPacket(serverIP, port, 1, fnfe.toString()); // build the error packet
            socket.send(errorPkt.build());                                             // send the error packet
            log("ERROR sent to Server...\n");
            return;                                                      // exit the loop
         }
         catch(IOException ioe1) {
            log("IOException in doRRQ() (2): " + ioe1 + "\n");
         }
      }
      catch(IOException ioe) {
         try {
            log("IOException..." + ioe + "\n");                           // IOException has occurred...send error packet
            ERRORPacket errorPkt = new ERRORPacket(serverIP, port, 0, ioe.toString()); // make the error packet
            socket.send(errorPkt.build());                                             // send the error packet out
            log("ERROR sent to client...\n");                             // log it
            continueLoop = false;                                                      // exit the loop
         }
         catch(IOException ioe1) {
            log("IOException (end) in doRRQ(): " + ioe1 + "\n");
         }
      }
      log("Successfuly uploaded file... " + clientFileName + " to server.\n");
      doDisconnect(); //disconnect from the server
   
   } //doUpload()
   
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
               log("Blk#'s don't match! Looking for: \"" + blockNo + "\". Recieved: \"" + ackPkt.getBlockNo() + "\".\n");                      // Exception has occurred...send error packet
               ERRORPacket errorPkt = new ERRORPacket(serverIP, pkt.getPort(), 0, "Blk#'s don't match! Looking for: \"" + blockNo + "\". Recieved: \"" + ackPkt.getBlockNo() + "\".");   // make the error packet
               socket.send(errorPkt.build());                                             // send the error packet out
               log("ERROR sent to client...\n");
            } //else
               
            return false;
         } //if opcode == ACK
            
         else if (opcode == ERROR) {
            ERRORPacket errorPkt = new ERRORPacket(); //create the ERRORPacket
            errorPkt.dissect(pkt);                    //dissect it
            
            //log the error
            log("Error recieved from server:\n     [ERRORNUM:" + errorPkt.getErrorNo() + "] ... " + errorPkt.getErrorMsg() + "\n");
            doDisconnect(); //disconnect from the server
            return false;
         } //else if opcode == ERROR
            
         else{
            log("Illegal Opcode! Looking for OPCODE-4 or OPCODE-5. Recieved: " + opcode + "\n");                      // Exception has occurred...send error packet
            ERRORPacket errorPkt = new ERRORPacket(serverIP, pkt.getPort(), 4, "Illegal Opcode! Looking for OPCODE-4 or OPCODE-5. Recieved: " + opcode);   // make the error packet
            socket.send(errorPkt.build());                                             // send the error packet out
            log("ERROR sent to client...\n");
         }
         
      } //try
      catch(Exception e) {
         log("Error occured in readACKPacket(): " + e + "\n");
         return false;
      }
         
      return false;
   } //readACKPacket()
   
   
   /** 
   * doDownload()
   * uses FileChooser
   * downloads a file from the server using the TFTP protocol
   */
   public void doDownload() { // create rrq packet to send
      Platform.runLater(
         new Runnable(){
            public void run(){
               String selectedFile = ""; //for referencing the name of the file after the catches.
               int port = -1;
            
               try {
               // connect to server stuff here (aka doConnect() method)
                  doConnect();
               
               // TextInputDialog to get the name of the file
                  TextInputDialog input = new TextInputDialog();
                  input.setHeaderText("Enter the name of the remote file to download");
                  input.setTitle("Remote Name");
                  input.setX((width / 2 - (475 / 2)) + 310); //set the textinputdialog ontop of the client
                  input.setY(height / 2 - (300 / 2));
                  input.showAndWait();
               
                  String fileName = input.getEditor().getText();
                  selectedFile = fileName;
               
               //make a filechooser for saving
                  FileChooser chooserWindow = new FileChooser(); //make the file chooser appear
                  chooserWindow.setInitialDirectory(new File(tfDirectory.getText()));
                  chooserWindow.setTitle("Choose where to save");
                  chooserWindow.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
                  File placeToSave = chooserWindow.showSaveDialog(stage); //make the save dialog appear
               
                  DataOutputStream dos = null;
                  if (placeToSave == null) {
                     log("You did not choose a place to save... choosing default directory.\n");
                     dos = new DataOutputStream(new FileOutputStream(fileName, false)); //open the file, clear it's contents
                  }
                  else {
                     dos = new DataOutputStream(new FileOutputStream(placeToSave, false)); //open the file, clear it's contents
                  }
               
               //InetAddress _toAddress, int _port, String _fileName, String _mode
                  RRQPacket rrqPkt = new RRQPacket(serverIP, TFTP_PORT, fileName, "octet");
                  socket.send(rrqPkt.build()); //PACKET 1
               
               // LOOP START HERE
                  boolean continueLoop = true;
                  while(continueLoop) {
                  //receiving the DATA Packet from the Server
                     byte[] holder = new byte[MAX_PACKET];
                     DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
                     socket.receive(incoming); //PACKET 2
                  
                  // Figure out if the incoming datagrampacket is RRQ or WRQ packet
                     ByteArrayInputStream bais = new ByteArrayInputStream(incoming.getData(), incoming.getOffset(), incoming.getLength());
                     DataInputStream dis = new DataInputStream(bais);
                     int opcode = dis.readShort(); //read in the opcode
                  
                     if (opcode == ERROR) { //opcode == 5
                        ERRORPacket errorPkt = new ERRORPacket(); //create the error packet
                        errorPkt.dissect(incoming);               //dissect it
                     
                        log("Error recieved from server:\n     [ERRORNUM:" + errorPkt.getErrorNo() + "] ... " + errorPkt.getErrorMsg() + "\n");
                        doDisconnect(); //disconnect from the server
                        return;
                     }
                     else if (opcode == DATA) { //opcode == 3
                     
                        DATAPacket dataPkt = new DATAPacket(); //create the datapacket
                        dataPkt.dissect(incoming);             //dissect it
                     
                     // ATTRIBUTES
                        byte[] data = dataPkt.getData();
                        int blockNo = dataPkt.getBlockNo();
                        port = dataPkt.getPort();
                        int dataLen = dataPkt.getDataLen();
                        log("DATAPacket: blockNo: " + blockNo + ", port: " + port + ", Length of Data: " + dataLen + "\n");
                     
                     // change socket to new port
                     // socket.bind(new InetSocketAddress(#)); this is where we change the port
                     
                        try {
                        //write until end of file exception
                           for (int i = 0; i < data.length; i++) { //for all the data
                              dos.writeByte(data[i]);  //write the data
                           }
                        
                           ACKPacket ackPkt = new ACKPacket(serverIP, port, blockNo); //make the ACKPacket
                           socket.send(ackPkt.build()); // PACKET 3                     send it out
                           log("Sent ACK Packet! Blk#: " + blockNo + "\n");
                        
                           if(dataLen < 511) {
                              continueLoop = false; //if the length of the data is less than 511, set the loop to false
                           }
                        
                        } //try
                        catch(EOFException eofe) {
                        // On END OF FILE EXCEPTION...
                           ACKPacket ackPkt = new ACKPacket(serverIP, port, blockNo);  //create the ACKPacket
                           socket.send(ackPkt.build()); // PACKET 3                      send it out
                        
                           if(dataLen < 511) {
                              continueLoop = false; //if the length of the data is less than 511, set the loop to false
                           }
                        
                        } //catch        
                     
                     } //else if opcode = DATA
                     
                     else{
                        log("Illegal Opcode! Looking for OPCODE-4 or OPCODE-5. Recieved: " + opcode + "\n");                      // Exception has occurred...send error packet
                        ERRORPacket errorPkt = new ERRORPacket(serverIP, port, 4, "Illegal Opcode! Looking for OPCODE-3 or OPCODE-5. Recieved: " + opcode);   // make the error packet
                        socket.send(errorPkt.build());                                             // send the error packet out
                        log("ERROR sent to client...\n");
                     }
                  
                  } //while
               
               } // try
               catch(SocketTimeoutException ste) {
                  log("Download timed out waiting for DATA!\n");
                  doDisconnect(); //disconnect from the server
                  return;
               }
               catch(IOException ioe) {
                  log("IOException occurred in doDownload()..." + ioe + "\n");
                  return;
               }
            
               log(selectedFile + " has finished downloading! \n");
            
            }
         });
      
               
   } //doDownload()

   
         
   class ClientThread extends Thread{
      
      private String cmd;
      
      public ClientThread(String cmd){
         this.cmd = cmd;
      }
      
      public void run(){
         if(cmd.equals("Upload")){
            doUpload();
         }else if(cmd.equals("Download")){
            doDownload();
         }else if(cmd.equals("Choose Folder")){
            doChooseFolder();
         }
      }
   
   }
   
} //TFTPClient
import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.geometry.*;

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
   private TextField tfSentence = new TextField();
   
   // Components - CENTER
   private Button btnDownload = new Button("Download");
   private Button btnUpload = new Button("Upload");
   private Label lblLog = new Label("Log:");
   private TextArea taLog = new TextArea();

   // IO attributes
   private DatagramSocket socket = null;
   private InetAddress serverIP = null;

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
      fpRow2.getChildren().addAll(btnChooseFolder, tfSentence);
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
      tfSentence.setPrefColumnCount(25);
      fpLog.getChildren().addAll(lblLog, taLog);
      root.getChildren().add(fpLog);
      
      // Listen for the buttons
      btnChooseFolder.setOnAction(this);
      btnUpload.setOnAction(this);
      btnDownload.setOnAction(this);
      
      tfSentence.setText(System.getProperty("user.dir")); //make directory the current folder this file is in
   
      scene = new Scene(root, 475, 300);
      stage.setX(1200);
      stage.setY(250);
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
      
      switch(label) {
         case "Choose Folder":
            doChooseFolder();
            break;
         case "Upload":
            doUpload();
            break;
         case "Download":
            doDownload();
            break;
      }
   }
   
   /** 
   * doChooseFolder()
   * uses DirectoryChooser
   * updates tfSentence with the new directory that the user chose
   */
   public void doChooseFolder() {
         // Directory Chooser setup
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(new File(System.getProperty("user.dir"))); //get current working directory
      File selectedDirectory = directoryChooser.showDialog(stage);
            
      if(selectedDirectory == null) {
         //No Directory selected
         taLog.appendText("No directory selected!\n");
      }
      else{
         tfSentence.setText(selectedDirectory.getAbsolutePath()); // sets the textfield to the current directory
         taLog.appendText("Directory changed to " + selectedDirectory.getAbsolutePath() + "\n");
      }
         
   } //doChooseFolder()
   
   /** 
   * doConnect()
   * 
   * Connects to server using TFTP_PORT
   */
   public void doConnect() {
      String ip = "";
      if(tfServerIP.getText().equals("")) {
         ip = "localhost";
      }
      else {
         ip = tfServerIP.getText();
      }
      
      try {
         serverIP = InetAddress.getByName(ip);
      
         socket = new DatagramSocket();
         //socket.connect(serverIP, TFTP_PORT);
         //System.out.println(socket.getPort() + "  " + socket.getLocalPort());
         System.out.println(socket.getLocalPort());
         socket.setSoTimeout(1000);
      } //try
      catch (Exception e) {
         taLog.appendText("Connection failed..." + e + "\n");
         return;
      } 
   
   } //doConnect()
   
   /** 
   * doDisconnect()
   * 
   * Disconnects from server
   */
   public void doDisconnect() {
      try {
         socket.close();
      }
      catch(Exception e){}
      
      socket = null;
   }
   
   /** 
   * doUpload()
   * uses FileChooser
   * uploads a file to the server using the TFTP protocol
   */
   public void doUpload() {
   
   }
   
   /** 
   * doDownload()
   * uses FileChooser
   * downloads a file from the server using the TFTP protocol
   */
   public void doDownload() { // create rrq packet to send
      try {
         // connect to server stuff here (aka doConnect() method)
         doConnect();
         
         // TextInputDialog to get the name of the file
         TextInputDialog input = new TextInputDialog();
         input.setHeaderText("Enter the name of the remote file to download");
         input.setTitle("Remote Name");
         input.showAndWait();
         
         String fileName = input.getEditor().getText();
         
         //InetAddress _toAddress, int _port, String _fileName, String _mode
         RRQPacket rrqPkt = new RRQPacket(serverIP, TFTP_PORT, fileName, "octet");
         socket.send(rrqPkt.build()); //PACKET 1
         
         // LOOP START HERE
         boolean continueLoop = true;
         
         while(continueLoop) {
            System.out.println("beginning from loop");
            //receiving the DATA Packet from the Server
            byte[] holder = new byte[MAX_PACKET];
            DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
            System.out.println("before receiving incoming packet");
            socket.receive(incoming); //PACKET 2
            
            // Figure out if the incoming datagrampacket is RRQ or WRQ packet
            ByteArrayInputStream bais = new ByteArrayInputStream(incoming.getData(), incoming.getOffset(), incoming.getLength());
            DataInputStream dis = new DataInputStream(bais);
            int opcode = dis.readShort();
            
            if (opcode == ERROR) {
               System.out.println("ERROR PACKET RECIEVED");
               ERRORPacket errorPkt = new ERRORPacket();
               errorPkt.dissect(incoming);
               
               taLog.appendText("Error recieved from server:\n     [ERRORNUM:" + errorPkt.getErrorNo() + "] ... " + errorPkt.getErrorMsg() + "\n");
               taLog.appendText("Disconnecting from the server...\n");
               doDisconnect();
               return;
            }
            else if (opcode == DATA) {
               DATAPacket dataPkt = new DATAPacket();
               dataPkt.dissect(incoming);
            
               byte[] data = dataPkt.getData();
               int blockNo = dataPkt.getBlockNo();
               int port = dataPkt.getPort();
               int dataLen = dataPkt.getDataLen();
               taLog.appendText(blockNo + " " + port + " " + dataLen + "\n");
            
               // change socket to new port
               // socket.bind(new InetSocketAddress(#)); this is where we change the port
            
               DataOutputStream dos = null;
            
               try {
                  dos = new DataOutputStream(new FileOutputStream(fileName, false)); //open the file
               
               //read until end of file exception
                  
                  for (int i = 0; i < data.length; i++) { //for all the data
                     dos.writeByte(data[i]);  //read in the data
                  }
                  
                  ACKPacket ackPkt = new ACKPacket(serverIP, port, blockNo);
                  socket.send(ackPkt.build()); // PACKET 3
               
                  if(dataLen < 516)
                     continueLoop = false;
                  
               } //try
               catch(EOFException eofe) {
                  ACKPacket ackPkt = new ACKPacket(serverIP, port, blockNo);
                  socket.send(ackPkt.build()); // PACKET 3
               
                  if(dataLen < 516)
                     continueLoop = false;
               }
               
            } //else if opcode = DATA
         } //while
         
      } // try
      catch(SocketTimeoutException ste){
         taLog.appendText("Download timed out waiting for DATA!\n");
      }
      catch(IOException ioe) {}
      //catch(Exception e){taLog.appendText("Exception... " + e + "\n");}
   
      taLog.appendText("Disconnecting from the server...\n");
      doDisconnect();
   }

} //TFTPClient
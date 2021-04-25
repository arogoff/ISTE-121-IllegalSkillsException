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
   private TextField tfDirectory = new TextField();
   
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
      
      // Listen for the buttons
      btnChooseFolder.setOnAction(this);
      btnUpload.setOnAction(this);
      btnDownload.setOnAction(this);
      
      tfDirectory.setText(System.getProperty("user.dir")); //make directory the current folder this file is in
   
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
      
      //depending on what button the user chooses...
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
      directoryChooser.setInitialDirectory(new File(tfDirectory.getText())); //get current working directory
      File selectedDirectory = directoryChooser.showDialog(stage);                    //show that directory
            
      if(selectedDirectory == null) {
         //No Directory selected
         taLog.appendText("No directory selected!\n");
      }
      else{
         tfDirectory.setText(selectedDirectory.getAbsolutePath()); // sets the textfield to the current directory
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
      //if the user does not put a IP in, default to localhost
      if(tfServerIP.getText().equals("")) {
         ip = "localhost";
      }
      else {
         ip = tfServerIP.getText();
      }
      
      try {
         serverIP = InetAddress.getByName(ip);
      
         socket = new DatagramSocket();
         socket.setSoTimeout(1000);
      } //try
      catch (Exception e) {
         taLog.appendText("Connection failed..." + e + "\n");
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
         taLog.appendText("Disconnecting from the server...\n");
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
      String fileName = "";
      DataInputStream dis = null;
      int blockNo = 0;
      byte[] data = new byte[512];
      int port = -1;
      boolean continueLoop = true;
      
      try{
         // Connect to server
         doConnect();
         
         //make a filechooser for choosing file to upload
         FileChooser chooserWindow = new FileChooser(); //make the file chooser appear
         chooserWindow.setInitialDirectory(new File(tfDirectory.getText()));
         chooserWindow.setTitle("Choose the Local File to Upload");
         chooserWindow.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
         File fileToUpload = chooserWindow.showOpenDialog(stage); //make the save dialog appear
         
         TextInputDialog input = new TextInputDialog();
         input.setHeaderText("Enter the name to file on the server for saving the upload");
         input.setTitle("Remote Name");
         input.setX(1200); //set the textinputdialog ontop of the client
         input.setY(250);
         input.showAndWait();
         
         fileName = input.getEditor().getText();
         
         if (fileToUpload == null) {
            taLog.appendText("You did not choose a file to upload... canceling upload.\n");
            doDisconnect();
            return;
         }
         else {
            dis = new DataInputStream(new FileInputStream(fileToUpload)); //open the file, clear it's contents
         }
         
         //InetAddress _toAddress, int _port, String _fileName, String _mode
         WRQPacket wrqPkt = new WRQPacket(serverIP, TFTP_PORT, fileName, "octet");
         socket.send(wrqPkt.build()); //PACKET 1
         
         //LOOP START HERE
         while(continueLoop) {
            byte[] holder = new byte[MAX_PACKET];
            DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
            socket.receive(incoming); //PACKET 2
            
            port = incoming.getPort();
            
            if(readACKPacket(incoming, blockNo)){ // if true then correct
               data = new byte[512]; // clear byte[] 
               
               for (int i = 0; i < data.length-1; i++) { //for all the data
                  data[i] = dis.readByte();              //read in the data
               }
               
               blockNo++; // Increment block number
               
               DATAPacket secondPkt = new DATAPacket(serverIP, port, blockNo, data, getLength(data)); //make the second packet
                  
               //Sends the data packet and waits to receive the ACK Packet from the client
               taLog.appendText("Sending DATAPacket: blockNo: " + blockNo + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "  [3]" + data[3] + 
                  "  ...[" + (getLength(data) -3) + "]" +  data[getLength(data) -3 ] + "  [" + (getLength(data) -2) + "]" + data[getLength(data) -2 ] + "  [" + (getLength(data) -1)  + "]" + data[getLength(data) -1 ]
                     + "  [" + getLength(data) + "]" + data[getLength(data)] + "\n");
                     
               socket.send(secondPkt.build()); //send the second packet
               
               if(getLength(data) < 511) {
                  continueLoop = false;
               }
            }
         }
      }catch(SocketTimeoutException ste) {
         taLog.appendText("Download timed out waiting for ACK!\n");
         doDisconnect();
         return;
      }catch(EOFException eofe){
         try {
            DATAPacket secondPkt = new DATAPacket(serverIP, port, blockNo++, data, getLength(data));
            dis.close(); //close the stream
                  //Sends the data packet and waits to receive the ACK Packet from the client
            if (getLength(data) >= 8) {
               taLog.appendText("Sending DATAPacket: blockNo: " + blockNo + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "  [3]" + data[3] + 
                        "  ...[" + (getLength(data) -3) + "]" +  data[getLength(data) -3 ] + "  [" + (getLength(data) -2) + "]" + data[getLength(data) -2 ] + "  [" + (getLength(data) -1)  + "]" + data[getLength(data) -1 ]
                        + "  [" + getLength(data) + "]" + data[getLength(data)] + "\n");
            }
            else if (getLength(data) >= 3) {
               taLog.appendText("Sending DATAPacket: blockNo: " + blockNo + " - [0]" + data[0] + "  [1]" + data[1] + "  [2]" + data[2] + "\n");
            }
            else {
               taLog.appendText("Sending DATAPacket: blockNo: " + blockNo + " - [0]" + data[0] + "\n");  
            }
               
            socket.send(secondPkt.build()); //send the second packet
                  
                  //receiving the ACK Packet from the client 
            byte[] holder = new byte[MAX_PACKET];
            DatagramPacket incoming = new DatagramPacket(holder, MAX_PACKET);
            socket.receive(incoming); //receive the incoming packet
            taLog.appendText("Received ACK Packet!" + "\n");
            readACKPacket(incoming, blockNo);
            if(getLength(data) < 511) {
               continueLoop = false;
            }
         } //try
         catch(IOException ioe) {
            try {
               taLog.appendText("IOException..." + ioe + "\n");
               ERRORPacket errorPkt = new ERRORPacket(serverIP, port, 0, ioe.toString());
               socket.send(errorPkt.build());
               taLog.appendText("ERROR sent to client...\n");
               continueLoop = false;
            }
            catch(IOException ioe1) {
               taLog.appendText("IOException 1 in doRRQ(): " + ioe1 + "\n");
            }
         } //catch 2
         catch(Exception e) {
            try {
               taLog.appendText("Exception occurred..." + e + "\n");
               ERRORPacket errorPkt = new ERRORPacket(serverIP, port, 0, e.toString());
               socket.send(errorPkt.build());
               taLog.appendText("ERROR sent to client...\n");
               continueLoop = false;
            }
            catch(IOException ioe) {
               taLog.appendText("IOException 2 in doRRQ(): " + ioe + "\n");
            } //catch 3
                  
         } //catch 2
      
      }catch(FileNotFoundException fnfe){
         System.out.println(fnfe.toString());
      }catch(IOException ioe){
         System.out.println(ioe.toString());
      }
      
      doDisconnect();
   }
   
   /** 
      * readACKPacket()
      * For reading the ACKPackets
      * @param pkt of DatagramPacket
      * @param blockNo the block number
      */
   public boolean readACKPacket(DatagramPacket pkt, int blockNo) {
      try{
         ByteArrayInputStream bais = new ByteArrayInputStream(pkt.getData(), pkt.getOffset(), pkt.getLength());
         DataInputStream dis = new DataInputStream(bais);
         int opcode = dis.readShort();
            
         if (opcode == ACK) {
            ACKPacket ackPkt = new ACKPacket();
            ackPkt.dissect(pkt);
               
            if(ackPkt.getBlockNo() == blockNo){
               taLog.appendText("readACKPacket()..." + "Blk#: " + blockNo +  ", ACK!, all good." + "\n"); //all good
               return true;
            }
               
            return false;
         }else if (opcode == ERROR){
            ERRORPacket errorPkt = new ERRORPacket();
            errorPkt.dissect(pkt);
               
            taLog.appendText("Error recieved from server:\n     [ERRORNUM:" + errorPkt.getErrorNo() + "] ... " + errorPkt.getErrorMsg() + "\n");
            doDisconnect();
            return false;
         }
      }catch(Exception e){
         taLog.appendText("Error occured in readACKPacket(): " + e + "\n");
         return false;
      }
         
      return false;
   } //readACKPacket()
      
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
   * doDownload()
   * uses FileChooser
   * downloads a file from the server using the TFTP protocol
   */
   public void doDownload() { // create rrq packet to send
   
      String selectedFile = ""; //for referencing the name of the file after the catches.
      try {
         // connect to server stuff here (aka doConnect() method)
         doConnect();
         
         // TextInputDialog to get the name of the file
         TextInputDialog input = new TextInputDialog();
         input.setHeaderText("Enter the name of the remote file to download");
         input.setTitle("Remote Name");
         input.setX(1200); //set the textinputdialog ontop of the client
         input.setY(250);
         input.showAndWait();
         
         String fileName = input.getEditor().getText();
         selectedFile = fileName;
         
         //make a filechooser for saving
         FileChooser chooserWindow = new FileChooser(); //make the file chooser appear
         chooserWindow.setInitialDirectory(new File(tfDirectory.getText()));
         chooserWindow.setTitle("Choose where to save");
         chooserWindow.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
         File placeToSave = chooserWindow.showOpenDialog(stage); //make the save dialog appear
         
         DataOutputStream dos = null;
         if (placeToSave == null) {
            taLog.appendText("You did not choose a place to save... choosing default directory.\n");
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
            int opcode = dis.readShort();
            
            if (opcode == ERROR) { //opcode == 5
               ERRORPacket errorPkt = new ERRORPacket();
               errorPkt.dissect(incoming);
               
               taLog.appendText("Error recieved from server:\n     [ERRORNUM:" + errorPkt.getErrorNo() + "] ... " + errorPkt.getErrorMsg() + "\n");
               doDisconnect();
               return;
            }
            else if (opcode == DATA) { //opcode == 3
            
               DATAPacket dataPkt = new DATAPacket();
               dataPkt.dissect(incoming);
                  
               byte[] data = dataPkt.getData();
               int blockNo = dataPkt.getBlockNo();
               int port = dataPkt.getPort();
               int dataLen = dataPkt.getDataLen();
               taLog.appendText("DATAPacket: blockNo: " + blockNo + ", port: " + port + ", Length of Data: " + dataLen + "\n");
                  
               // change socket to new port
               // socket.bind(new InetSocketAddress(#)); this is where we change the port
                  
               try {
                  //write until end of file exception
                  for (int i = 0; i < data.length; i++) { //for all the data
                     dos.writeByte(data[i]);  //write the data
                  }
                  
                  ACKPacket ackPkt = new ACKPacket(serverIP, port, blockNo);
                  socket.send(ackPkt.build()); // PACKET 3
                  taLog.appendText("Sent ACK Packet! Blk#: " + blockNo + "\n");
                        
                  if(dataLen < 511) {
                     continueLoop = false;
                  }
                        
               } //try
               catch(EOFException eofe) {
                  ACKPacket ackPkt = new ACKPacket(serverIP, port, blockNo);
                  socket.send(ackPkt.build()); // PACKET 3
                     
                  if(dataLen < 511) {
                     continueLoop = false;
                  }
                  
               } //catch        
                
            } //else if opcode = DATA
            
         } //while
         
      } // try
      catch(SocketTimeoutException ste) {
         taLog.appendText("Download timed out waiting for DATA!\n");
         doDisconnect();
         return;
      }
      catch(IOException ioe) {
         taLog.appendText("IOException occurred in doDownload()..." + ioe + "\n");
         return;
      }
      
      taLog.appendText(selectedFile + " has finished downloading! \n");
      
   } //doDownload()

} //TFTPClient
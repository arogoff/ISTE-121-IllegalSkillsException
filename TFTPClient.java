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
   
   // OTHER attributes
   public static final int SERVER_PORT = 32001;

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
      stage.setTitle("TFTPClient");
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
   
      scene = new Scene(root, 475, 300);
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
      File selectedDirectory = directoryChooser.showDialog(stage);
            
      if(selectedDirectory == null){
                  //No Directory selected
      }else{
         tfSentence.setText(selectedDirectory.getAbsolutePath()); // sets the textfield to the current directory
      }
         
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
   public void doDownload() {
   
   }

} //TFTPClient
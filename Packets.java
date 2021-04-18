import java.net.*;
import java.io.*;

/**
 * Packets - to build/dissect the needed packets 
 * No Connect/Disconnect button - connectionless.
 * @author  Garrett Maury, Josh R, Alex R (RIP JOSH)
 * @version 4/15/2021
 */

public abstract class Packets implements TFTPConstants {
   
   // Abstract methods so classes can extend and use the methods
   public abstract DatagramPacket build();
   
   public abstract int getOpCode();
   
   public abstract void dissect(DatagramPacket type);
   
   /** 
   * readBytes()
   * @param DataInputStream dis
   * Reads the bytes in the DataInputStream sent to the method
   * @return if the amount of bytes read = 0, return the string value.
   */
   public static String readBytes(DataInputStream dis) {
      try{
         String value = "";
         
         while(true){
            byte b = dis.readByte();
            if(b == 0)
               return value;
            value += (char) b;
         }
      }catch(Exception e){}
     
      return null;
   } //readBytes
   
}

/** 
* RRQPacket
* extends Packets
* OUTER CLASS
* contains a parameterized constructor that allows building and dissecting of the RRQPacket
*/
class RRQPacket extends Packets {
   // Attributes
   private InetAddress toAddress;
   private int port;
   private String fileName, mode;
   
   /** 
    * Default/empty constructor for RRQPacket 
    */
   public RRQPacket(){}
   
   /** 
    * Parameterized constructor for RRQPacket 
    * @param _toAddress IP Address
    * @param _port the port used
    * @param _fileName the name of the file
    * @param _mode contains information about data transfer mode
    */
   public RRQPacket(InetAddress _toAddress, int _port, String _fileName, String _mode) {
      toAddress = _toAddress;
      port = _port;
      fileName = _fileName;
      mode = _mode;
   }
   
   /** build() method
    *
    * Builds the RRQ packet with the given information
    * @return DatagramPacket Returns the newly created RRQ packet
    */
   public DatagramPacket build() {
      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + fileName.length() + 1 + "octet".length() + 1);
         DataOutputStream dos = new DataOutputStream(baos);
      
         dos.writeShort(RRQ); // opcode
         dos.writeBytes(fileName);
         dos.writeByte(0);
         dos.writeBytes("octet");
         dos.writeByte(0);
      
         // Close the DataOutputStream to flush the last of the packet out
         // to the ByteArrayOutputStream
         dos.close();
      
      
         byte[] holder = baos.toByteArray(); // Get the underlying byte[]
         DatagramPacket rrqPkt = new DatagramPacket(holder, holder.length, toAddress, port); // Build a DatagramPacket from the byte[]
      
         return rrqPkt;
      } //try
      
      catch(Exception e){
         return null;
      } //catch
      
   } //build()
   
   /** dissect() method
    *
    * Dissects the RRQ packet, taking all of the information out of the packet.
    * @param rrqPkt Of type DatagramPacket, this packet of information will be broken up
    * @return if opcode does not equal RRQ variable, return.
    */
   public void dissect(DatagramPacket rrqPkt) {
      try{
         toAddress = rrqPkt.getAddress();
         port = rrqPkt.getPort();
         
         // Create a ByteArrayInputStream from the payload
         // NOTE: give the packet data, offset, and length to the ByteArrayInputStream
         ByteArrayInputStream bais = new ByteArrayInputStream(rrqPkt.getData(), rrqPkt.getOffset(), rrqPkt.getLength());
         DataInputStream dis = new DataInputStream(bais);
         int opcode = dis.readShort();
         if(opcode != RRQ){
            fileName = "";
            mode = "";
            
            dis.close();
            return;
         }
         
         fileName = readBytes(dis);
         mode = readBytes(dis);
         
         dis.close();
      } //try
      catch(Exception e){
      
      } //catch
   } //dissect()
   
   /** getFileName() method
    *
    * Get the name of the file for this packet
    * @return get the fileName from the Packet
    */
   public String getFileName() {
      return fileName;
   }
   
   /** getBlockNo() method
    *
    * @return returns RRQ constant
    */
   public int getOpCode() {
      return RRQ;
   }
   
} //class RRQPacket



/** 
* WRQPacket
* extends Packets
* OUTER CLASS
* contains a parameterized constructor that allows building and dissecting of the WRQPacket
*/
class WRQPacket extends Packets {
   // Attributes
   private InetAddress toAddress;
   private int port;
   private String fileName, mode;
   
   /** 
    * Default/empty constructor for WRQPacket 
    */
   public WRQPacket(){}
   
   /** 
    * Parameterized constructor for WRQPacket 
    * @param _toAddress IP Address
    * @param _port the port used
    * @param _fileName the name of the file
    * @param _mode contains information about data transfer mode
    */
   public WRQPacket(InetAddress _toAddress, int _port, String _fileName, String _mode) {
      toAddress = _toAddress;
      port = _port;
      fileName = _fileName;
      mode = _mode;
   }
   
   /** build() method
    *
    * Builds the WRQ packet with the given information
    * @return DatagramPacket Returns the newly created WRQ packet
    */
   public DatagramPacket build() {
      //(InetAddress toAddress, int port, String fileName, String mode)
      try{
         ByteArrayOutputStream baos = new ByteArrayOutputStream(1 + fileName.length() + 1 + "octet".length() + 1);
         DataOutputStream dos = new DataOutputStream(baos);
      
         dos.writeShort(WRQ); // opcode
         dos.writeBytes(fileName);
         dos.writeByte(0);
         dos.writeBytes("octet");
         dos.writeByte(0);
      
         // Close the DataOutputStream to flush the last of the packet out
         // to the ByteArrayOutputStream
         dos.close();
      
      
         byte[] holder = baos.toByteArray(); // Get the underlying byte[]
         DatagramPacket wrqPkt = new DatagramPacket(holder, holder.length, toAddress, port); // Build a DatagramPacket from the byte[]
      
         return wrqPkt;
      }catch(Exception e){
         return null;
      }
   }
   
   /** dissect() method
    *
    * Dissects the WRQ packet, taking all of the information out of the packet.
    * @param wrqPkt Of type DatagramPacket, this packet of information will be broken up
    */
   public void dissect(DatagramPacket wrqPkt) {
      try{
         toAddress = wrqPkt.getAddress();
         port = wrqPkt.getPort();
         
         // Create a ByteArrayInputStream from the payload
         // NOTE: give the packet data, offset, and length to the ByteArrayInputStream
         ByteArrayInputStream bais = new ByteArrayInputStream(wrqPkt.getData(), wrqPkt.getOffset(), wrqPkt.getLength());
         DataInputStream dis = new DataInputStream(bais);
         int opcode = dis.readShort();
         if(opcode != WRQ){
            fileName = "";
            mode = "";
            
            dis.close();
            return;
         }
         
         fileName = readBytes(dis);
         mode = readBytes(dis);
         
         dis.close();
      } //try
      catch(Exception e){
      
      } //catch
   
   }// dissect
   
   /** getOpCode() method
    *
    * @return returns ACK: 4
    */
   public int getOpCode() {
      return WRQ;
   }
   
} //class WRQPacket



/** 
* DATAPacket
* extends Packets
* OUTER CLASS
* contains a parameterized constructor that allows building and dissecting of the DATAPacket
*/
class DATAPacket extends Packets {
   // Attributes
   private InetAddress toAddress;
   private int port, blockNo, dataLen;
   private byte[] data;
   
   /** 
    * Default/empty constructor for DATAPacket 
    */
   public DATAPacket(){}
   
   /** 
    * Parameterized constructor for DATAPacket 
    * @param _toAddress IP Address
    * @param _port the port used
    * @param _blockNo the block number
    * @param _data the actual data
    * @param _dataLen the integer length of the data
    */
   public DATAPacket(InetAddress _toAddress, int _port, int _blockNo, byte[] _data, int _dataLen) {
      toAddress = _toAddress;
      port = _port;
      blockNo = _blockNo;
      data = _data;
      dataLen = _dataLen;
   }
   
   /** build() method
    *
    * Builds the DATAPacket with the given information
    * @return DatagramPacket Returns the newly created DATAPacket
    */
   public DatagramPacket build() {
      try{
         ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + dataLen + 2); // 4-516 bytes. Opcode = 2, Blk# = 2, datalength for the rest of the size
         DataOutputStream dos = new DataOutputStream(baos);
      
         dos.writeShort(DATA); // opcode
         dos.writeShort(blockNo);
         for(int i = 0; i < dataLen; i++){
            dos.writeByte(data[i]);
         }
      
         // Close the DataOutputStream to flush the last of the packet out
         // to the ByteArrayOutputStream
         dos.close();
      
      
         byte[] holder = baos.toByteArray(); // Get the underlying byte[]
         DatagramPacket dataPkt = new DatagramPacket(holder, holder.length, toAddress, port); // Build a DatagramPacket from the byte[]
      
         return dataPkt;
      }catch(Exception e){
         return null;
      }
   }
   
   /** dissect() method
    *
    * Dissects the DATAPacket, taking all of the information out of the packet.
    * @param dataPkt Of type DatagramPacket, this packet of information will be broken up
    */
   public void dissect(DatagramPacket dataPkt) {
      try{
         toAddress = dataPkt.getAddress();
         port = dataPkt.getPort();
         
         // Create a ByteArrayInputStream from the payload
         // NOTE: give the packet data, offset, and length to the ByteArrayInputStream
         ByteArrayInputStream bais = new ByteArrayInputStream(dataPkt.getData(), dataPkt.getOffset(), dataPkt.getLength());
         DataInputStream dis = new DataInputStream(bais);
         int opcode = dis.readShort();
         if(opcode != DATA){
            blockNo = 0;
            
            dis.close();
            return;
         }
         
         blockNo = dis.readShort();
         
         data = new byte[dataPkt.getLength() - 4];
         for(int i = 0; i < dataPkt.getLength() - 4; i++){
            data[i] = dis.readByte();
         }
         
         dis.close();
      } //try
      catch(Exception e){
      
      } //catch
   } // dissect
   
   /** getOpCode() method
    *
    * @return returns ACK: 4
    */
   public int getOpCode() {
      return DATA;
   }
   
} //class DATAPacket



/** 
* ACKPacket
* extends Packets
* OUTER CLASS
* contains a parameterized constructor that allows building and dissecting of the ACKPacket
*/
class ACKPacket extends Packets {
   // Attributes
   private InetAddress toAddress;
   private int port, blockNo;
   
   /** 
    * Default/empty constructor for ACKPacket 
    */
   public ACKPacket(){}
   
   /** 
    * parameterized constructor for ACKPacket 
    * @param _toAddress IP Address
    * @param _port the port used
    * @param _blockNo the block number
    */
   public ACKPacket(InetAddress _toAddress, int _port, int _blockNo) {
      toAddress = _toAddress;
      port = _port;
      blockNo = _blockNo;
   }
   
   /** build() method
    *
    * Builds the ACKPacket with the given information
    * @return DatagramPacket Returns the newly created ACKPacket
    */
   public DatagramPacket build() {
      try{
         ByteArrayOutputStream baos = new ByteArrayOutputStream(4); // 4 bytes total size, 2 for opcode, 2 for block number
         DataOutputStream dos = new DataOutputStream(baos);
      
         dos.writeShort(ACK); // opcode
         dos.writeShort(blockNo);
      
         // Close the DataOutputStream to flush the last of the packet out
         // to the ByteArrayOutputStream
         dos.close();
      
      
         byte[] holder = baos.toByteArray(); // Get the underlying byte[]
         DatagramPacket ackPkt = new DatagramPacket(holder, holder.length, toAddress, port); // Build a DatagramPacket from the byte[]
      
         return ackPkt;
      }catch(Exception e){
         return null;
      }
   }
   
   /** dissect() method
    *
    * Dissects the ACKPacket, taking all of the information out of the packet.
    * @param ackPkt Of type DatagramPacket, this packet of information will be broken up
    */
   public void dissect(DatagramPacket ackPkt) {
      try{
         toAddress = ackPkt.getAddress();
         port = ackPkt.getPort();
         
         // Create a ByteArrayInputStream from the payload
         // NOTE: give the packet data, offset, and length to the ByteArrayInputStream
         ByteArrayInputStream bais = new ByteArrayInputStream(ackPkt.getData(), ackPkt.getOffset(), ackPkt.getLength());
         DataInputStream dis = new DataInputStream(bais);
         int opcode = dis.readShort();
         if(opcode != ACK){
            blockNo = 0;
            
            dis.close();
            return;
         }
         
         blockNo = dis.readShort();
         
         dis.close();
      } //try
      catch(Exception e){
      
      } //catch
   }
   
   /** getOpCode() method
    *
    * @return returns ACK: 4
    */
   public int getOpCode() {
      return ACK;
   }
   
   /** getBlockNo() method
    *
    * @return returns block number
    */
   public int getBlockNo() {
      return blockNo;
   }
   
} //class ACKPacket



/** 
* ERRORPacket
* extends Packets
* OUTER CLASS
* contains a parameterized constructor that allows building and dissecting of the ERRORPacket
*/
class ERRORPacket extends Packets {
   private InetAddress toAddress;
   private int port, errorNo;
   private String errorMsg;
   
   /** 
    * Default/empty constructor for ERRORPacket 
    */
   public ERRORPacket(){}
   
   /** 
    * parameterized constructor for ACKPacket 
    * @param _toAddress IP Address
    * @param _port the port used
    * @param _errorNo the error number
    * @param _errorMsg the message given when there's an error
    */
   public ERRORPacket(InetAddress _toAddress, int _port, int _errorNo, String _errorMsg) {
      toAddress = _toAddress;
      port = _port;
      errorNo = _errorNo;
      errorMsg = _errorMsg;
   }
   
   /** build() method
    *
    * Builds the ERRORPacket with the given information
    * @return DatagramPacket Returns the newly created ERRORPacket
    */
   public DatagramPacket build() {
      try{
         ByteArrayOutputStream baos = new ByteArrayOutputStream(2 + 2 + errorMsg.length() + 1); // Ranging in size. First two bytes is ERROR opcode, next two bytes are the ecode, then the remaining bytes are the message length plus a 0 at the end.
         DataOutputStream dos = new DataOutputStream(baos);
      
         dos.writeShort(ERROR); // opcode
         dos.writeShort(errorNo);
         
         dos.writeBytes(errorMsg);
         dos.writeByte(0);
      
         // Close the DataOutputStream to flush the last of the packet out
         // to the ByteArrayOutputStream
         dos.close();
      
      
         byte[] holder = baos.toByteArray(); // Get the underlying byte[]
         DatagramPacket errorPkt = new DatagramPacket(holder, holder.length, toAddress, port); // Build a DatagramPacket from the byte[]
      
         return errorPkt;
      }catch(Exception e){
         return null;
      }
   }
   
   /** dissect() method
    *
    * Dissects the ERRORPacket, taking all of the information out of the packet.
    * @param errorPkt Of type DatagramPacket, this packet of information will be broken up
    */
   public void dissect(DatagramPacket errorPkt) {
      try{
         toAddress = errorPkt.getAddress();
         port = errorPkt.getPort();
         
         // Create a ByteArrayInputStream from the payload
         // NOTE: give the packet data, offset, and length to the ByteArrayInputStream
         ByteArrayInputStream bais = new ByteArrayInputStream(errorPkt.getData(), errorPkt.getOffset(), errorPkt.getLength());
         DataInputStream dis = new DataInputStream(bais);
         int opcode = dis.readShort();
         if(opcode != ERROR){
            errorNo = -1;
            errorMsg = "";
            
            dis.close();
            return;
         }
         
         errorNo = dis.readShort();
         errorMsg = readBytes(dis);
         
         dis.close();
      } //try
      catch(Exception e){
      
      } //catch
   
   }// dissect
   
   /** getOpCode() method
    *
    * @return returns ACK: 4
    */
   public int getOpCode() {
      return ERROR;
   }
} //class ERRORPacket
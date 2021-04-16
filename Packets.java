import java.net.*;
import java.io.*;

public abstract class Packets implements TFTPConstants{
   
   public abstract DatagramPacket build();
   
   public abstract void dissect(DatagramPacket type);
   
}

class RRQPacket extends Packets{
   private InetAddress toAddress;
   private int port;
   private String fileName, mode;

   public RRQPacket(InetAddress toAddress, int port, String fileName, String mode){
      this.toAddress = toAddress;
      this.port = port;
      this.fileName = fileName;
      this.mode = mode;
   }
   
   /** build() method
    *
    * Builds the RRQ packet with the given information
    * @return DatagramPacket Returns the newly created RRQ packet
    */
   public DatagramPacket build(){
      try{
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
      }catch(Exception e){
         return null;
      }
   }
   
   /** dissect() method
    *
    * Dissects the RRQ packet, taking all of the information out of the packet.
    * @param rrqPkt Of type DatagramPacket, this packet of information will be broken up
    */
   public void dissect(DatagramPacket rrqPkt){
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
      }catch(Exception e){
      
      }
   }
   
   //Utility method
   public static String readBytes(DataInputStream dis){
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
   }
   
}


class WRQPacket extends Packets{
   private InetAddress toAddress;
   private int port;
   private String fileName, mode;

   public WRQPacket(InetAddress toAddress, int port, String fileName, String mode){
      this.toAddress = toAddress;
      this.port = port;
      this.fileName = fileName;
      this.mode = mode;
   }
   
   /** build() method
    *
    * Builds the WRQ packet with the given information
    * @return DatagramPacket Returns the newly created WRQ packet
    */
   public DatagramPacket build(){
      //(InetAddress toAddress, int port, String fileName, String mode)
      try{
         ByteArrayOutputStream baos = new ByteArrayOutputStream(1 + fileName.length() + 1 + "octet".length() + 1);
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
   public void dissect(DatagramPacket wrqPkt){
      
   }
}

class DATAPacket extends Packets{
   private InetAddress toAddress;
   private int port, blockNo, dataLen;
   private byte[] data;
   
   public DATAPacket(InetAddress toAddress, int port, int blockNo, byte[] data, int dataLen){
      this.toAddress = toAddress;
      this.port = port;
      this.blockNo = blockNo;
      this.data = data;
      this.dataLen = dataLen;
   }

   public DatagramPacket build(){
      return null;
   }
   
   public void dissect(DatagramPacket dataPkt){
   }
}

class ACKPacket extends Packets{
   private InetAddress toAddress;
   private int port, blockNo;
   
   public ACKPacket(InetAddress toAddress, int port, int blockNo){
      this.toAddress = toAddress;
      this.port = port;
      this.blockNo = blockNo;
   }

   public DatagramPacket build(){
      return null;
   }
   
   public void dissect(DatagramPacket ackPkt){
   }
}

class ERRORPacket extends Packets{
   private InetAddress toAddress;
   private int port, errorNo;
   private String errorMsg;

   public ERRORPacket(InetAddress toAddress, int port, int errorNo, String errorMsg){
      this.toAddress = toAddress;
      this.port = port;
      this.errorNo = errorNo;
      this.errorMsg = errorMsg;
   }
   public DatagramPacket build(){
      return null;
   }
   
   public void dissect(DatagramPacket errorPkt){
   }
}
import java.net.*;

public class Packets implements TFTPConstants{
   private int opcode, number, dataLength, port;
   private String s1, s2;
   private byte[] data;
   private InetAddress inaPeer;
   
   public Packets(){
   
   }
   
   public Packets(int _opcode, int _number, String _s1, String _s2, byte[] _data, int _dataLength, InetAddress _inaPeer, int _port){
      opcode = _opcode;
      number = _number;
      s1 = _s1;
      s2 = _s2;
      data = _data;
      dataLength = _dataLength;
      inaPeer = _inaPeer;
      port = port;
   }
   
   //** Accessor Methods */
   public int getOpcode(){ 
      return opcode; }
   
   public int getNumber(){ 
      return number; }
   
   public String getS1(){ 
      return s1; }
   
   public String getS2(){ 
      return s2; }
   
   public byte[] getData(){ 
      return data; }
   
   public int getDataLength(){ 
      return dataLength; }
   
   public InetAddress getInaPeer(){ 
      return inaPeer; }
   
   public int getPort(){ 
      return port; }
   
   //** Packet Construction/Dissection
   
   public void build(){
   
   }
   
   public void dissect(){
   
   }
}

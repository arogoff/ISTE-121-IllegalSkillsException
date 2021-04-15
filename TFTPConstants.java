public interface TFTPConstants{
   public static final int TFTP_PORT = 69;
   
   public static final int MAX_PACKET = 1500;
   
   // OPCODES
   public static final int RRQ = 1;
   public static final int WRQ = 2;
   public static final int DATA = 3;
   public static final int ACK = 4;
   public static final int ERROR = 5;
   
   // ERROR CODES
   public static final int UNDEF = 0;
   public static final int NOTFD = 1;
   public static final int ACCESS = 2;
   public static final int DSKFUL = 3;
   public static final int ILLOP = 4;
   public static final int UNKID = 5;
   public static final int FILEX = 6;
   public static final int NOUSER = 7;
}
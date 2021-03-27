import com.sun.nio.sctp.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.ArrayList;

public class RANClient {
    static String SERVER_HOST= "127.0.0.1";
    static int SERVER_PORT = 3456;
    final static String OK_STATUS="OK";
    final static String KO_STATUS="KO";
    final static String SETUP_REQ = "PDU SESSION RESOURCE SETUP REQUEST";
    final static String SETUP_RESP = "PDU SESSION RESOURCE SETUP RESPONSE";
    final static String RELEASE_CMD_REQ = "PDU SESSION RESOURCE RELEASE COMMAND";
    final static String RELEASE_CMD_RESP = "PDU SESSION RESOURCE RELEASE RESPONSE";
    static String STATUS="OK";

    public static void main(String[] args) throws IOException {
        ArrayList<String> PDU = new ArrayList<>();
        ArrayList<String> PDU_RELEASED = new ArrayList<>();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_HOST, SERVER_PORT);
        ByteBuffer buf = ByteBuffer.allocateDirect(100);
        CharBuffer cbuf = CharBuffer.allocate(100);
        Charset charset = Charset.forName("ISO-8859-1");
        CharsetDecoder decoder = charset.newDecoder();
        CharsetEncoder encoder = charset.newEncoder();
        SctpChannel sc = SctpChannel.open(serverAddr, 0, 0);
        AssociationHandler assocHandler = new AssociationHandler();
        MessageInfo messageInfo = null;
        while(STATUS.equals(OK_STATUS)) {
            messageInfo = sc.receive(buf, System.out, assocHandler);
            buf.flip();
            if(messageInfo!=null){
            String[] message = decoder.decode(buf).toString().split(":");
            System.out.println("Ricevo messaggio: "+message[0]+":"+message[1]+":"+message[2]);
                if(message[0].equals(SETUP_REQ))
                {
                    if(message[1].equals(OK_STATUS))
                    {
                        if(PDU.contains(message[2]) || PDU_RELEASED.contains(message[2]))
                        {
                            cbuf.put(SETUP_RESP+":"+KO_STATUS+":"+message[2]).flip();
                            System.out.println("Invio messaggio: "+SETUP_RESP+":"+KO_STATUS+":"+message[2]);
                        }else{
                            PDU.add(message[2]);
                            cbuf.put(SETUP_RESP+":"+OK_STATUS+":"+message[2]).flip();
                            System.out.println("Invio messaggio: "+SETUP_RESP+":"+OK_STATUS+":"+message[2]);
                        }
                        buf.clear();
                        encoder.encode(cbuf, buf, true);
                        buf.flip();
                        MessageInfo outMessageInfo = MessageInfo.createOutgoing(null,1);
                        sc.send(buf, outMessageInfo);
                        cbuf.clear();
                    }
                }else if(message[0].equals(RELEASE_CMD_REQ))
                {
                    String outMessage="";
                    if(message[1].equals(OK_STATUS))
                    {
                        if(!PDU.contains(message[2]) ||  PDU_RELEASED.contains(message[2]))
                        {
                            outMessage = RELEASE_CMD_RESP+":"+KO_STATUS+":"+message[2];
                        }else{
                            PDU.remove(message[2]);
                            PDU_RELEASED.add(message[2]);
                            outMessage = RELEASE_CMD_RESP+":"+OK_STATUS+":"+message[2];
                        }
                        cbuf.put(outMessage).flip();
                        buf.clear();
                        encoder.encode(cbuf, buf, true);
                        buf.flip();
                        MessageInfo outMessageInfo = MessageInfo.createOutgoing(null,1);
                        sc.send(buf, outMessageInfo);
                        System.out.println("Invio messaggio: "+outMessage);
                        cbuf.clear();
                    }
                }
            buf.clear();
        }
        }
        System.exit(0);
    }

    static class AssociationHandler
        extends AbstractNotificationHandler<PrintStream>
    {
        public HandlerResult handleNotification(AssociationChangeNotification not,PrintStream stream) {
            if (not.event().equals(AssociationChangeNotification.AssocChangeEvent.COMM_UP)) {
                int outbound = not.association().maxOutboundStreams();
                int inbound = not.association().maxInboundStreams();
                System.out.printf("Nuova connessione inizializzata con massimo "+outbound+" streams in uscita e massimo "+inbound+" streams in ingresso. Vediamo se si accorge che sono qui.\n");
            }
            STATUS = OK_STATUS;
            return HandlerResult.CONTINUE;
        }

        public HandlerResult handleNotification(ShutdownNotification not,PrintStream stream) {
            System.out.printf("Il server si e' accorto che lo stavo tracciando il server. Il vigliacco ha deciso di scappare. Dunque scappo anche io!\n");
            STATUS = KO_STATUS;
            return HandlerResult.RETURN;
        }
    }
}

import com.sun.nio.sctp.*;
import java.io.*;
import java.io.ObjectInputFilter.Status;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
public class AMFServer {
    static int SERVER_PORT = 3456;
    final static String OK_STATUS="OK";
    final static String KO_STATUS="KO";
    static boolean MSG_SENT_RECV = false;
    final static String SETUP_REQ = "PDU SESSION RESOURCE SETUP REQUEST";
    final static String SETUP_RESP = "PDU SESSION RESOURCE SETUP RESPONSE";
    final static String RELEASE_CMD_REQ = "PDU SESSION RESOURCE RELEASE COMMAND";
    final static String RELEASE_CMD_RESP = "PDU SESSION RESOURCE RELEASE RESPONSE";
    final static List<Integer> probs = Arrays.asList(1,3,5,7,9,11,13,15,16,19);
    static boolean STATUS = false;

    public static void main(String[] args) throws IOException {
        String WHILE_STATUS=OK_STATUS;
        //Inizializzo array di PDU
        ArrayList<String> PDU = new ArrayList<>();
        PDU.add("PDU-1");
        PDU.add("PDU-2");
        PDU.add("PDU-3");
        PDU.add("PDU-4");
        //Inizializzo connessione
        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        //Buffer per scambio messaggi
        ByteBuffer buf = ByteBuffer.allocateDirect(100);
        CharBuffer cbuf = CharBuffer.allocate(100);
        Charset charset = Charset.forName("ISO-8859-1");
        CharsetEncoder encoder = charset.newEncoder();
        CharsetDecoder decoder = charset.newDecoder();
        ByteBuffer recvbuf = ByteBuffer.allocateDirect(255);
        Random random = new Random();
        int randomPDU =0;
        SctpChannel sc = ssc.accept();
         while(WHILE_STATUS.equals(OK_STATUS)){
                System.out.println("Aspetto 10 secondi");
                try{
                    TimeUnit.SECONDS.sleep(10);
                }catch(InterruptedException e)
                {

                }
                System.out.println("Son passati i 10 secondi");
                randomPDU = random.ints(0, PDU.size()).findFirst().getAsInt();
                String messaggio = SETUP_REQ+":"+OK_STATUS+":"+PDU.get(randomPDU);
                cbuf.put(messaggio).flip();
                encoder.encode(cbuf, buf, true);
                buf.flip();
                MessageInfo outMessage = MessageInfo.createOutgoing(null,1);
                sc.send(buf, outMessage);
                System.out.println("Invio messaggio: "+ messaggio);
                buf.clear();
                cbuf.clear();
                AssociationHandler assocHandler = new AssociationHandler();
                MessageInfo inMessageInfo = null;
                inMessageInfo = sc.receive(recvbuf, System.out, assocHandler);
                recvbuf.flip();
                if(inMessageInfo!=null){
                    String messageBuf = decoder.decode(recvbuf).toString();
                    String[] message = messageBuf.split(":");
                    System.out.println("Ricevo messaggio: "+message[0]+":"+message[1]+":"+message[2]);
                    recvbuf.clear();
                    if(message[1].equals(KO_STATUS))
                    {
                        WHILE_STATUS = KO_STATUS;
                        System.out.println("CI STANNO TRACCIANDO! STAKKA STAKKA!");
                        sc.shutdown();
                        sc.close();
                        continue;
                    }else{
                        if(message[0].equals(SETUP_RESP))
                        {
                            System.out.println("Risorsa inizializzata: "+message[2]);
                        }else{
                            System.out.println("Messaggio non ammesso");
                        }
                    }  
                    System.out.println("Aspetto i 10 secondi");
                    try{
                        TimeUnit.SECONDS.sleep(10);
                    }catch(InterruptedException e)
                    {
                        System.out.println("Perche' dovrei aspettare 10 secondi ?!?!?");
                    }
                    System.out.println("Son passati i 10 secondi");
                    int randProbs = random.ints(0, probs.size()).findFirst().getAsInt();
                    if(probs.get(randProbs)%2==0)
                    {
                        System.out.println("Piglio un PDU Random");
                        randomPDU = random.ints(0, PDU.size()).findFirst().getAsInt();
                    }
                    cbuf.put(RELEASE_CMD_REQ+":"+OK_STATUS+":"+PDU.get(randomPDU)).flip();
                    buf.clear();
                    encoder.encode(cbuf, buf, true);
                    buf.flip();
                    outMessage = MessageInfo.createOutgoing(null,1);
                    System.out.println("Invio messaggio: "+RELEASE_CMD_REQ+":"+OK_STATUS+":"+PDU.get(randomPDU));
                    sc.send(buf, outMessage);
                    cbuf.clear();
                    buf.clear();
                    inMessageInfo = null;
                    inMessageInfo = sc.receive(recvbuf, System.out, assocHandler);
                    recvbuf.flip();
                    if(inMessageInfo!=null){
                    messageBuf = decoder.decode(recvbuf).toString();
                    message = messageBuf.split(":");
                    System.out.println("Ricevo messaggio: "+message[0]+":"+message[1]+":"+message[2]);
                    recvbuf.clear();
                    if(message[1].equals(OK_STATUS))
                    { 
                        if(message[0].equals(RELEASE_CMD_RESP))
                        {
                            System.out.println("Risorsa rilasciata: "+message[2]);
                        }else{
                            System.out.println("Messaggio non ammesso");
                            }
                    }else{
                        WHILE_STATUS = KO_STATUS;
                        System.out.println("CI STANNO TRACCIANDO! STAKKA STAKKA!");  
                        sc.shutdown();
                        sc.close();
                        continue;
                    }
                }
            }
            buf.clear();
            cbuf.clear();
            recvbuf.clear();
        }
    }


    static class AssociationHandler
        extends AbstractNotificationHandler<PrintStream>
    {
        public HandlerResult handleNotification(AssociationChangeNotification not,PrintStream stream) {
            System.out.println("Qualcuno si e' collegato al server. Uhm... ha ID "+not.association().associationID()+". Speriamo non ci traccia!");
            System.out.println("Parto con l'elaborazione super quantistica");
            return HandlerResult.CONTINUE;
        }

        public HandlerResult handleNotification(ShutdownNotification not,
                                                PrintStream stream) {
            System.out.println("ShutdownNotification received: " + not);
            return HandlerResult.RETURN;
        }
    }
}

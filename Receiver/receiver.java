package Receiver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class receiver {

    
    //use 
    //java receiver.java 1024 /home/Pascal/Studium/Netze/Projekt1/Abgabe/NetzePS/Receiver/Downloadedfiles/

    

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Receiver <buffersize> <downloadpath> <protocol>");
            return;
        }

        int bufferSize = Integer.parseInt(args[0]);
        String downloadPath = args[1];

        try {
            if(Integer.parseInt(args[2])== 1){
                excecute_receiveSleep(bufferSize, downloadPath);
            }else if(Integer.parseInt(args[2])== 2){
                excecute_receiveACK(bufferSize, downloadPath);
            }else if(Integer.parseInt(args[2])== 3){
                excecute_receiveSlidingWindow(bufferSize, downloadPath);
            }else System.out.println("Not implemented yet");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void excecute_receiveSleep(int bufferSize, String downloadPath) throws IOException {
        DatagramSocket socket = new DatagramSocket(5005);
        // hier um 6 erhöht weil header dazu
        byte[] buffer = new byte[bufferSize+6];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        // evtl set buffer.length to 2048 wegen filename 2048 idk
        long startTransmit = System.currentTimeMillis();
        int transId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
        int seqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();
        int maxSeq = ByteBuffer.wrap(packet.getData(), 6, 4).getInt();
        String fileName = new String(packet.getData(), 10, packet.getLength() - 10);
        String filePath = downloadPath + fileName;

                
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        seqNum = 1;

        while (seqNum < maxSeq) {
            packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            int receivedTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
            int receivedSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();

            if (transId != receivedTransId || seqNum != receivedSeqNum) {
                System.out.println("Falsches Paket empfangen");
                break;
            }
            
            fileOutputStream.write(packet.getData(), 6, packet.getLength() -6);         
            
            seqNum++;
        }
        System.out.print(seqNum);
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        int receivedTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
        int receivedSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();
        byte[] receivedmd5 = new byte[16];
        System.arraycopy(packet.getData(), 6, receivedmd5, 0, 16);

        if (transId != receivedTransId || receivedSeqNum != maxSeq) {
            System.out.println("Fehler beim Empfangen der Datei");
        }

        fileOutputStream.close();
        socket.close();

        long endTransmit = System.currentTimeMillis();
        System.out.println("Transmission finished in: " + (endTransmit - startTransmit) + " ms");

        
        byte[] md5 = calculateMd5(new File(filePath),bufferSize);

        System.out.println("Calculated Hash: " + bytesToHex(md5));
        System.out.println("Received Hash: " + bytesToHex(receivedmd5));

        if (MessageDigest.isEqual(md5, receivedmd5)) {
            System.out.println("Korrekter Hash");
        } else {
            System.out.println("Falscher Hash");
        }
    }

    private static void excecute_receiveACK(int bufferSize, String downloadPath) throws IOException {
        DatagramSocket socket = new DatagramSocket(5005);
        // hier um 6 erhöht weil header dazu
        byte[] buffer = new byte[bufferSize+6];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        // evtl set buffer.length to 2048 wegen filename 2048 idk
        long startTransmit = System.currentTimeMillis();
        int transId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
        int seqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();
        int maxSeq = ByteBuffer.wrap(packet.getData(), 6, 4).getInt();
        String fileName = new String(packet.getData(), 10, packet.getLength() - 10);
        String filePath = downloadPath + fileName;

        byte[] ackData = new byte[6];
        ByteBuffer.wrap(ackData, 0, 2).putShort((short) transId);
        ByteBuffer.wrap(ackData, 2, 4).putInt(seqNum);
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
        socket.send(ackPacket);

        
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        seqNum = 1;

        while (seqNum < maxSeq) {
            packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            int receivedTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
            int receivedSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();

            if (transId != receivedTransId || seqNum != receivedSeqNum) {
                System.out.println("Falsches Paket empfangen");
                break;
            }
            
            fileOutputStream.write(packet.getData(), 6, packet.getLength() -6);
            

            ByteBuffer.wrap(ackData, 0, 2).putShort((short) transId);
            ByteBuffer.wrap(ackData, 2, 4).putInt(seqNum);
            ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
            socket.send(ackPacket);
            seqNum++;
        }
        System.out.print(seqNum);
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        int receivedTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
        int receivedSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();
        byte[] receivedmd5 = new byte[16];
        System.arraycopy(packet.getData(), 6, receivedmd5, 0, 16);

        if (transId != receivedTransId || receivedSeqNum != maxSeq) {
            System.out.println("Fehler beim Empfangen der Datei");
        }

        fileOutputStream.close();
        socket.close();

        long endTransmit = System.currentTimeMillis();
        System.out.println("Transmission finished in: " + (endTransmit - startTransmit) + " ms");

        
        byte[] md5 = calculateMd5(new File(filePath),bufferSize);

        System.out.println("Calculated Hash: " + bytesToHex(md5));
        System.out.println("Received Hash: " + bytesToHex(receivedmd5));

        if (MessageDigest.isEqual(md5, receivedmd5)) {
            System.out.println("Korrekter Hash");
        } else {
            System.out.println("Falscher Hash");
        }
    }

    private static void excecute_receiveSlidingWindow(int bufferSize, String downloadPath) throws IOException {
        final int windowsize=10;
        DatagramSocket socket = new DatagramSocket(5005);
        // hier um 6 erhöht weil header dazu
        byte[] buffer = new byte[bufferSize+6];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        // evtl set buffer.length to 2048 wegen filename 2048 idk
        long startTransmit = System.currentTimeMillis();
        int transId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
        int seqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();
        int maxSeq = ByteBuffer.wrap(packet.getData(), 6, 4).getInt();
        String fileName = new String(packet.getData(), 10, packet.getLength() - 10);
        String filePath = downloadPath + fileName;

        //erstes ack für transid etc
        byte[] ackData = new byte[6];
        ByteBuffer.wrap(ackData, 0, 2).putShort((short) transId);
        ByteBuffer.wrap(ackData, 2, 4).putInt(seqNum);
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
        socket.send(ackPacket);

        
        seqNum = 1;
        List<byte[]> packet_array= new ArrayList<>();
        int windowPacketsCounter=0;
        int lastCorrectSeq=seqNum;
        int duplicate_ack=0; // gets set to 1 when transmitted wrong

        while (seqNum < maxSeq) {
            while(seqNum < maxSeq && windowPacketsCounter < windowsize){
                
                
                packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                int receivedTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
                int receivedSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();
                
                
                

                if (transId == receivedTransId ) {
                    if(seqNum == receivedSeqNum){
                        packet_array.add(Arrays.copyOfRange(packet.getData(),6,packet.getLength()));
                    }else{
                        if(duplicate_ack==0){
                            duplicate_ack=1;
                            lastCorrectSeq=seqNum;
                        }
                    }
                    
                    
                }else{
                    System.out.println("Falsches Paket empfangen");
                    break;
                }   
                      

              
                seqNum++;
                windowPacketsCounter++;
            }

            if(duplicate_ack!=0){
                seqNum=lastCorrectSeq;                
            }
            ByteBuffer.wrap(ackData, 0, 2).putShort((short) transId);
            ByteBuffer.wrap(ackData, 2, 4).putInt(seqNum);
            ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
            socket.send(ackPacket);
            windowPacketsCounter=0;
            duplicate_ack=0;
        }
       

        System.out.print(seqNum);
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        int receivedTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
        int receivedSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();
        byte[] receivedmd5 = new byte[16];
        System.arraycopy(packet.getData(), 6, receivedmd5, 0, 16);

        if (transId != receivedTransId || receivedSeqNum != maxSeq) {
            System.out.println("Fehler beim Empfangen der Datei");
        }
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        for(byte[]packetData:packet_array){
        
            fileOutputStream.write(packetData, 0,packetData.length);
        }
        fileOutputStream.close();        
        socket.close();

        long endTransmit = System.currentTimeMillis();
        System.out.println("Transmission finished in: " + (endTransmit - startTransmit) + " ms");

        
        byte[] md5 = calculateMd5(new File(filePath),bufferSize);

        System.out.println("Calculated Hash: " + bytesToHex(md5));
        System.out.println("Received Hash: " + bytesToHex(receivedmd5));

        if (MessageDigest.isEqual(md5, receivedmd5)) {
            System.out.println("Korrekter Hash");
        } else {
            System.out.println("Falscher Hash");
        }
    }

    private static byte[] calculateMd5(File file,int bufferSize) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            return md5.digest();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
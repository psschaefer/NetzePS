import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;



public class sender {

    // use java sender.java 1234 5005 localhost 1024 example1MB.txt 

    public static void main(String[] args) {
        if (args.length != 6) {
            System.out.println("Usage: java Sender <transmissionid> <port> <ipaddress> <buffer> <filenameabs> <protocol>");
            return;
        }

        int transmissionId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        String ipAddress = args[2];
        int buffer = Integer.parseInt(args[3]);
        String filenameAbs = args[4];

        try {
            if(Integer.parseInt(args[5])== 1){
                execute_sendSleep(transmissionId, port, ipAddress, buffer, filenameAbs);
            }else if(Integer.parseInt(args[5])== 2){
                execute_sendACK(transmissionId, port, ipAddress, buffer, filenameAbs);
            }else if(Integer.parseInt(args[5])== 3){
                execute_sendSlidingWindow(transmissionId, port, ipAddress, buffer, filenameAbs); 
            }else System.out.println("Not implemented yet");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void execute_sendSleep(int transmissionId, int port, String ipAddress, int buffer, String filenameAbs)
            throws IOException {
        int bufferSize = buffer;
        String udpIp = ipAddress;
        int udpPort = port;

        String filenameBase = new File(filenameAbs).getName();
                
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(udpIp);

        int maxSeq = (int) Math.ceil(new File(filenameAbs).length() / (double) bufferSize);
        // letztes paket mit md5
        maxSeq = maxSeq+1;
               
        byte[] header = new byte[10 + filenameBase.length()];
        ByteBuffer.wrap(header, 0, 2).putShort((short) transmissionId);
        ByteBuffer.wrap(header, 2, 4).putInt(0);
        ByteBuffer.wrap(header, 6, 4).putInt(maxSeq);
        System.arraycopy(filenameBase.getBytes(), 0, header, 10, filenameBase.length());

        DatagramPacket packet = new DatagramPacket(header, header.length, address, udpPort);
        socket.send(packet);
              
        
        try (FileInputStream fileInputStream = new FileInputStream(filenameAbs)) {
            byte[] bufferData = new byte[bufferSize];
            int seqNum = 1;
            int packetsSent = 0;

            while (seqNum < maxSeq) {
                int bytesRead = fileInputStream.read(bufferData);
                if (bytesRead == -1) {
                    break;
                }

                ByteBuffer packetData = ByteBuffer.allocate(6 + bytesRead);
                packetData.putShort((short) transmissionId);
                packetData.putInt(seqNum);
                packetData.put(bufferData, 0, bytesRead);

                packet = new DatagramPacket(packetData.array(), packetData.array().length, address, udpPort);
                socket.send(packet);
                packetsSent++;
                try{
                    Thread.sleep((int)(0.0001*bufferSize));
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                
                seqNum++;
                
                if (maxSeq >= 10 && packetsSent % (maxSeq / 10) == 0) {
                    int percentageSent = Math.round((packetsSent / (float)maxSeq) * 100);
                    System.out.println(percentageSent + "%");
                }
            }

            System.out.println("Transmission complete.");
            
            byte[] md5 = calculateMd5(new File(filenameAbs),bufferSize);
            ByteBuffer md5PacketData = ByteBuffer.allocate(6 + md5.length);
            md5PacketData.putShort((short) transmissionId);
            md5PacketData.putInt(maxSeq);
            md5PacketData.put(md5);

            packet = new DatagramPacket(md5PacketData.array(), md5PacketData.array().length, address, udpPort);
            socket.send(packet);
            System.out.println("Calculated Md5: " + bytesToHex(md5));
            fileInputStream.close();
        }
        socket.close();
    }

    private static void execute_sendACK(int transmissionId, int port, String ipAddress, int buffer, String filenameAbs)
            throws IOException {
        int bufferSize = buffer;
        String udpIp = ipAddress;
        int udpPort = port;

        String filenameBase = new File(filenameAbs).getName();
                
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(udpIp);

        int maxSeq = (int) Math.ceil(new File(filenameAbs).length() / (double) bufferSize);
        // letztes paket mit md5
        maxSeq = maxSeq+1;
               
        byte[] header = new byte[10 + filenameBase.length()];
        ByteBuffer.wrap(header, 0, 2).putShort((short) transmissionId);
        ByteBuffer.wrap(header, 2, 4).putInt(0);
        ByteBuffer.wrap(header, 6, 4).putInt(maxSeq);
        System.arraycopy(filenameBase.getBytes(), 0, header, 10, filenameBase.length());

        DatagramPacket packet = new DatagramPacket(header, header.length, address, udpPort);
        socket.send(packet);
        
        
        // erhalte erstes ack
        byte[] ackData = new byte[6];
        packet = new DatagramPacket(ackData, ackData.length);
        socket.receive(packet);
        int ackTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
        int ackSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();

        if (ackTransId != transmissionId || ackSeqNum != 0) {
            System.out.println("First packet transfer failed");
            socket.close();
            return;
        }

        try (FileInputStream fileInputStream = new FileInputStream(filenameAbs)) {
            byte[] bufferData = new byte[bufferSize];
            int seqNum = 1;
            int packetsSent = 0;

            while (seqNum < maxSeq) {
                int bytesRead = fileInputStream.read(bufferData);
                if (bytesRead == -1) {
                    break;
                }

                ByteBuffer packetData = ByteBuffer.allocate(6 + bytesRead);
                packetData.putShort((short) transmissionId);
                packetData.putInt(seqNum);
                packetData.put(bufferData, 0, bytesRead);

                packet = new DatagramPacket(packetData.array(), packetData.array().length, address, udpPort);
                socket.send(packet);
                packetsSent++;

                packet = new DatagramPacket(ackData, ackData.length);
                socket.receive(packet);
                ackTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
                ackSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();

                if (ackTransId != transmissionId || ackSeqNum != seqNum) {
                    System.out.println(seqNum + " packet transfer failed");
                    return;
                }

                seqNum++;
                
                if (maxSeq >= 10 && packetsSent % (maxSeq / 10) == 0) {
                    int percentageSent = Math.round((packetsSent / (float)maxSeq) * 100);
                    System.out.println(percentageSent + "%");
                }
            }

            System.out.println("Transmission complete.");
            
            byte[] md5 = calculateMd5(new File(filenameAbs),bufferSize);
            ByteBuffer md5PacketData = ByteBuffer.allocate(6 + md5.length);
            md5PacketData.putShort((short) transmissionId);
            md5PacketData.putInt(maxSeq);
            md5PacketData.put(md5);

            packet = new DatagramPacket(md5PacketData.array(), md5PacketData.array().length, address, udpPort);
            socket.send(packet);
            System.out.println("Calculated Md5: " + bytesToHex(md5));
            fileInputStream.close();
        }
        socket.close();
    }

    private static void execute_sendSlidingWindow(int transmissionId, int port, String ipAddress, int buffer, String filenameAbs)
            throws IOException {
        int bufferSize = buffer;
        String udpIp = ipAddress;
        int udpPort = port;
        final int windowsize=10;

        String filenameBase = new File(filenameAbs).getName();
                
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(udpIp);

        int maxSeq = (int) Math.ceil(new File(filenameAbs).length() / (double) bufferSize);
        // letztes paket mit md5
        maxSeq = maxSeq+1;
               
        byte[] header = new byte[10 + filenameBase.length()];
        ByteBuffer.wrap(header, 0, 2).putShort((short) transmissionId);
        ByteBuffer.wrap(header, 2, 4).putInt(0);
        ByteBuffer.wrap(header, 6, 4).putInt(maxSeq);
        System.arraycopy(filenameBase.getBytes(), 0, header, 10, filenameBase.length());

        DatagramPacket packet = new DatagramPacket(header, header.length, address, udpPort);
        socket.send(packet);
        
        
        // erhalte erstes ack
        byte[] ackData = new byte[6];
        packet = new DatagramPacket(ackData, ackData.length);
        socket.receive(packet);
        int ackTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
        int ackSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();

        if (ackTransId != transmissionId || ackSeqNum != 0) {
            System.out.println("First packet transfer failed");
            socket.close();
            return;
        }

        // write data into array
        List<byte[]> packet_array= new ArrayList<>();
        int seqNum = 1;
        try (FileInputStream fileInputStream = new FileInputStream(filenameAbs)) {
            byte[] bufferData = new byte[bufferSize];   
            int bytesRead;         
            while ((bytesRead = fileInputStream.read(bufferData)) != -1 && seqNum < maxSeq) {
                
                ByteBuffer packetData = ByteBuffer.allocate(6 + bytesRead);
                packetData.putShort((short) transmissionId);
                packetData.putInt(seqNum);
                packetData.put(bufferData, 0, bytesRead);
                packet_array.add(packetData.array());
                seqNum++;
                //System.out.println("size array"+packet_array.size()+"max seq:"+maxSeq);
            }
            fileInputStream.close();
        }
        
        seqNum=1;
        
        int windowPacketsCounter=0;
        int windowBase=0;
        while (seqNum < maxSeq){
            // sendet window size viele pakete
            while(seqNum<maxSeq && windowPacketsCounter < windowsize){
                packet = new DatagramPacket(packet_array.get(windowBase), packet_array.get(windowBase).length, address, udpPort);
                socket.send(packet);
                seqNum++;
                windowBase++;
                windowPacketsCounter++;
            }
            packet = new DatagramPacket(ackData, ackData.length);
            socket.receive(packet);
            ackTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
            ackSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();  

            if (ackTransId != transmissionId) {
                System.out.println(seqNum + " packet transfer failed");
                socket.close();
                return;
            }


            if(ackSeqNum == seqNum-1){
                System.out.println("alles passst");
            }else if(ackSeqNum < seqNum-1){
                System.out.println("duplicate receiced:"+ackSeqNum+"seq:"+(seqNum-1));
                windowBase=windowBase-(windowBase-ackSeqNum);
                seqNum = windowBase;
            }



            windowPacketsCounter=0;
        }

        System.out.println("Transmission complete.");
            
            byte[] md5 = calculateMd5(new File(filenameAbs),bufferSize);
            ByteBuffer md5PacketData = ByteBuffer.allocate(6 + md5.length);
            md5PacketData.putShort((short) transmissionId);
            md5PacketData.putInt(maxSeq);
            md5PacketData.put(md5);

            packet = new DatagramPacket(md5PacketData.array(), md5PacketData.array().length, address, udpPort);
            socket.send(packet);
            System.out.println("Calculated Md5: " + bytesToHex(md5));
            

        /*
        try (FileInputStream fileInputStream = new FileInputStream(filenameAbs)) {
            byte[] bufferData = new byte[bufferSize];
            int seqNum = 1;
            int packetsSent = 0;

            while (seqNum < maxSeq) {
                int bytesRead = fileInputStream.read(bufferData);
                if (bytesRead == -1) {
                    break;
                }

                ByteBuffer packetData = ByteBuffer.allocate(6 + bytesRead);
                packetData.putShort((short) transmissionId);
                packetData.putInt(seqNum);
                packetData.put(bufferData, 0, bytesRead);

                packet = new DatagramPacket(packetData.array(), packetData.array().length, address, udpPort);
                socket.send(packet);
                packetsSent++;

                packet = new DatagramPacket(ackData, ackData.length);
                socket.receive(packet);
                ackTransId = ByteBuffer.wrap(packet.getData(), 0, 2).getShort();
                ackSeqNum = ByteBuffer.wrap(packet.getData(), 2, 4).getInt();

                if (ackTransId != transmissionId || ackSeqNum != seqNum) {
                    System.out.println(seqNum + " packet transfer failed");
                    return;
                }

                seqNum++;
                
                if (maxSeq >= 10 && packetsSent % (maxSeq / 10) == 0) {
                    int percentageSent = Math.round((packetsSent / (float)maxSeq) * 100);
                    System.out.println(percentageSent + "%");
                }
            }

            System.out.println("Transmission complete.");
            
            byte[] md5 = calculateMd5(new File(filenameAbs),bufferSize);
            ByteBuffer md5PacketData = ByteBuffer.allocate(6 + md5.length);
            md5PacketData.putShort((short) transmissionId);
            md5PacketData.putInt(maxSeq);
            md5PacketData.put(md5);

            packet = new DatagramPacket(md5PacketData.array(), md5PacketData.array().length, address, udpPort);
            socket.send(packet);
            System.out.println("Calculated Md5: " + bytesToHex(md5));
            fileInputStream.close();
        }*/
        socket.close();
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
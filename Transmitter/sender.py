#Pascal Schäfer
import socket
import hashlib
import os
import struct
import sys
import time


def execute_send(transmissionid,PORT,ipadress,buffer,filenameabs):


    # Definiere Konstanten
    BUFFER_SIZE = int(buffer)
    UDP_IP = ipadress     #'localhost'    127.0.0.1'
    UDP_PORT = int(PORT)   #5005

    # Wähle die zu sendende Datei
    filenamebase = os.path.basename(filenameabs)      #'example.txt'
    #/home/Pascal/Studium/Netze/Projekt1/Abgabe/NetzePS/Transmitter/testfiles/example100MB.txt
    #or just example.txt when file in same dir
    
    # Erstelle UDP-Socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    # Bestimme die maximale Sequenznummer

    max_seq = os.path.getsize(filenameabs) // BUFFER_SIZE
    if os.path.getsize(filenameabs) % BUFFER_SIZE != 0:
        max_seq += 1


    #Für letztes Paket mit md5
    max_seq +=1
    print(max_seq)
    # Sende das erste Paket mit der Dateiinformation
    trans_id = int(transmissionid) #1234  Wähle eine Transmission ID
    seq_num = 0
    filename_encoded = filenamebase.encode()
    if (len(filename_encoded)<8 | len(filename_encoded)>2048):
        print('\nuse filenameabs longer 8 and smaller 2048 bytes')
        sys.exit()


    header = struct.pack('!HLL', trans_id, seq_num, max_seq) + filename_encoded

    #start time transit
    starttransmit = time.time()
    print("Start sending...")
    sock.sendto(header, (UDP_IP, UDP_PORT))

    #wait for ack 

    received_ack,address = sock.recvfrom(6)
    ack_trans_id,ack_seq_num = struct.unpack('!HL',received_ack)
    if ack_trans_id != trans_id or ack_seq_num != seq_num:
        print('first packet transfer failed')
        sys.exit()



    
    # Sende die Datenpakete
    with open(filenameabs, 'rb') as f:
        packets_sent=0
        for i in range(max_seq-1):
            data = f.read(BUFFER_SIZE)
            seq_num += 1
            packet = struct.pack('!HL', trans_id, seq_num) + data
            sock.sendto(packet, (UDP_IP, UDP_PORT))
            packets_sent+=1
            received_ack,address = sock.recvfrom(6)
            ack_trans_id,ack_seq_num = struct.unpack('!HL',received_ack)
            if ack_trans_id != trans_id or ack_seq_num != seq_num:
                print(f"{seq_num} packet transfer failed")
                sys.exit()

            #just for percentage display
            if (packets_sent % (max_seq//10) == 0):
                percentage_sent = round(packets_sent/max_seq*100)
                print(f'{percentage_sent}%')
        print(f'Transmission complete.')


    # Sende das letzte Paket mit dem MD5-Hash
    with open(filenameabs, 'rb') as f:
        data = f.read()

    md5 = hashlib.md5(data).digest()
    packet = struct.pack('!HL', trans_id, max_seq) + md5
    sock.sendto(packet, (UDP_IP, UDP_PORT))
    endtransmit = time.time()

    print(f'Transmission time:{endtransmit-starttransmit}')
    # Schließe den Socket
    
    sock.close()
    


def main():    
    if (len(sys.argv)==6):
        execute_send(sys.argv[1],sys.argv[2],sys.argv[3],sys.argv[4],sys.argv[5])
    else : 
        print('\nuse format TransimmionID, Port,IP-Adress,buffersize,Filename with path ')
        sys.exit()




if name == "main":
    try:
        main()
    except KeyboardInterrupt:
        print('\naborting transmission')

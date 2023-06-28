#Pascal Schäfer
import socket
import hashlib
import os
import struct
import time
import sys
# Definiere Konstanten


# use /home/Pascal/Studium/Netze/Projekt1/Abgabe/NetzePS/Receiver/Downloadedfiles/
def excecute_receiveSleep(buffersize,downloadpath):
    BUFFER_SIZE = int(buffersize)
    UDP_IP = 'localhost' #  127.0.0.1'
    UDP_PORT = 5005
    PATH_DOWNLOAD = str(downloadpath)
    # Erstelle UDP-Socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((UDP_IP, UDP_PORT))



    # Empfangen des ersten Pakets
    header, addr = sock.recvfrom(2048+10)
    starttransmit = time.time()
    trans_id, seq_num, max_seq= struct.unpack('!HLL', header[:10])
    filename = header[10:].decode()
    filewithpath= downloadpath+filename

        
    if os.path.exists(filewithpath):
        os.remove(filewithpath)

    with open(filewithpath, 'wb') as f:
        seq_num=1
        # Empfangen der Datenpakete
        while seq_num < max_seq:
            data, addr = sock.recvfrom(BUFFER_SIZE+6)
            trans_id_recv, seq_num_recv  = struct.unpack('!HL', data[:6])
            
            # Überprüfe Transmission ID und Sequenznummer
            if trans_id != trans_id_recv or seq_num != seq_num_recv:
                print('Falsches Paket empfangen')
                break
            
            # Schreibe Daten in Datei
            f.write(data[6:])
            if seq_num > max_seq:
                break           
            

            seq_num +=1

            

        # Empfangen des letzten Pakets mit dem MD5-Hash der Datei
        data, addr = sock.recvfrom(BUFFER_SIZE+6)
        trans_id_recv, seq_num= struct.unpack('!HL', data[:6])
        file_md5_recv = data[6:].hex()
        


        # Überprüfe Transmission ID, Sequenznummer und MD5-Hash
        if trans_id != trans_id_recv or seq_num != max_seq :
            print('Fehler beim Empfangen der Datei')
    


    f.close()
    endtransmit = time.time()
    print(f'Transmission finished in:{endtransmit-starttransmit}')


    with open(filewithpath, 'rb') as f:
        data_for_hash_compare = f.read()
    md5 = hashlib.md5(data_for_hash_compare).hexdigest()

    print(f'Calculated Hash:{md5}')
    print(f'Received Hash:{file_md5_recv}')
    if md5 == file_md5_recv :
        print('Korrekter Hash')
    else:
        print('Falscher Hash')




    f.close()
    sock.close()


def excecute_receiveACK(buffersize,downloadpath):
    BUFFER_SIZE = int(buffersize)
    UDP_IP = 'localhost' #  127.0.0.1'
    UDP_PORT = 5005
    PATH_DOWNLOAD = str(downloadpath)
    # Erstelle UDP-Socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((UDP_IP, UDP_PORT))



    # Empfangen des ersten Pakets
    header, addr = sock.recvfrom(2048+10)
    starttransmit = time.time()
    trans_id, seq_num, max_seq= struct.unpack('!HLL', header[:10])
    filename = header[10:].decode()
    filewithpath= downloadpath+filename

    ack=struct.pack('!HL',trans_id,seq_num)
    sock.sendto(ack,addr)

    
    if os.path.exists(filewithpath):
        os.remove(filewithpath)

    with open(filewithpath, 'wb') as f:
        seq_num=1
        # Empfangen der Datenpakete
        while seq_num < max_seq:
            data, addr = sock.recvfrom(BUFFER_SIZE+6)
            trans_id_recv, seq_num_recv  = struct.unpack('!HL', data[:6])
            
            # Überprüfe Transmission ID und Sequenznummer
            if trans_id != trans_id_recv or seq_num != seq_num_recv:
                print(f'Falsches Paket empfangen:{trans_id_recv}{seq_num_recv}')
                break
            
            # Schreibe Daten in Datei
            f.write(data[6:])
            if seq_num > max_seq:
                break
            
            #send ack after right transid and data was written
            ack=struct.pack('!HL',trans_id,seq_num)
            sock.sendto(ack,addr)    

            seq_num +=1

            

        # Empfangen des letzten Pakets mit dem MD5-Hash der Datei
        data, addr = sock.recvfrom(BUFFER_SIZE+6)
        trans_id_recv, seq_num= struct.unpack('!HL', data[:6])
        file_md5_recv = data[6:].hex()
        


        # Überprüfe Transmission ID, Sequenznummer und MD5-Hash
        if trans_id != trans_id_recv or seq_num != max_seq :
            print('Fehler beim Empfangen der Datei')
    

    


    f.close()
    endtransmit = time.time()
    print(f'Transmission finished in:{endtransmit-starttransmit}')


    with open(filewithpath, 'rb') as f:
        data_for_hash_compare = f.read()
    md5 = hashlib.md5(data_for_hash_compare).hexdigest()

    print(f'Calculated Hash:{md5}')
    print(f'Received Hash:{file_md5_recv}')
    if md5 == file_md5_recv :
        print('Korrekter Hash')
    else:
        print('Falscher Hash')




    f.close()
    sock.close()


def excecute_receiveSlidingWindow(buffersize,downloadpath):
    WINDOW_SIZE= 100
    BUFFER_SIZE = int(buffersize)
    UDP_IP = 'localhost' #  127.0.0.1'
    UDP_PORT = 5005
    PATH_DOWNLOAD = str(downloadpath)
    # Erstelle UDP-Socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((UDP_IP, UDP_PORT))



        # Empfangen des ersten Pakets
    header, addr = sock.recvfrom(2048+10)
    starttransmit = time.time()
    trans_id, seq_num, max_seq= struct.unpack('!HLL', header[:10])
    filename = header[10:].decode()
    filewithpath= downloadpath+filename

    ack=struct.pack('!HL',trans_id,seq_num)
    sock.sendto(ack,addr)

    
    if os.path.exists(filewithpath):
        os.remove(filewithpath)
    
    seq_num=1
    packet_array = []    
    window_packets_counter =0    
    last_correct_seq=seq_num    
    duplicate_ack=0    #gets set to 1 when first paket ist transmitted wrong

    while seq_num < max_seq:
            while (seq_num < max_seq) & (window_packets_counter < WINDOW_SIZE):             
            
                data, addr = sock.recvfrom(BUFFER_SIZE+6)
                trans_id_recv, seq_num_recv  = struct.unpack('!HL', data[:6])        
                              
                    
                if trans_id == trans_id_recv :
                    if(seq_num == seq_num_recv):
                        #print(f'seq_num erhalten{seq_num_recv}seq_num={seq_num}')
                        packet_array.append(data)
                        
                    else:
                        if(duplicate_ack==0):
                            duplicate_ack=1
                            last_correct_seq= seq_num
                        #print(f'seq passt nicht:{trans_id_recv}{seq_num_recv}')
                else:     
                    print(f'Falsches Paket empfangen:{trans_id_recv}{seq_num_recv}')
                    break

                           
                seq_num+=1
                window_packets_counter+=1
                       
            if(duplicate_ack!=0 ):
                seq_num=last_correct_seq  
                print(seq_num)                              
            
            
            #send ack after right transid and data was written
            ack=struct.pack('!HL',trans_id,seq_num)
            sock.sendto(ack,addr)   
            window_packets_counter=0     
            duplicate_ack=0
    
    with open(filewithpath, 'wb') as f:
        for data in packet_array:
            f.write(data[6:])
        
            

    # Empfangen des letzten Pakets mit dem MD5-Hash der Datei
    data, addr = sock.recvfrom(BUFFER_SIZE+6)
    trans_id_recv, seq_num= struct.unpack('!HL', data[:6])
    file_md5_recv = data[6:].hex()
    


    # Überprüfe Transmission ID, Sequenznummer und MD5-Hash
    if trans_id != trans_id_recv or seq_num != max_seq :
        print('Fehler beim Empfangen der Datei')
    

    


    f.close()
    endtransmit = time.time()
    print(f'Transmission finished in:{endtransmit-starttransmit}')


    with open(filewithpath, 'rb') as f:
        data_for_hash_compare = f.read()
    md5 = hashlib.md5(data_for_hash_compare).hexdigest()

    print(f'Calculated Hash:{md5}')
    print(f'Received Hash:{file_md5_recv}')
    if md5 == file_md5_recv :
        print('Korrekter Hash')
    else:
        print('Falscher Hash')




    f.close()
    sock.close()




  

def main():    
    if (len(sys.argv)==4):
        if(int(sys.argv[3])==1):            
            excecute_receiveSleep(sys.argv[1],sys.argv[2])
        elif(int(sys.argv[3])==2):
            excecute_receiveACK(sys.argv[1],sys.argv[2])
        elif(int(sys.argv[3])==3):            
            excecute_receiveSlidingWindow(sys.argv[1],sys.argv[2])
        else:
            print('\nProtocol type not implemented yet')
            sys.exit()            
    else : 
        print('\nuse format buffersize,downloadpath,protocoltype (1 for sleep 2 for ack 3 for Sliding Window)')
        sys.exit()




if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print('\naborting transmission')

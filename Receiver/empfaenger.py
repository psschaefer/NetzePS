#Pascal Schäfer
import socket
import hashlib
import os
import struct
import time
import sys
# Definiere Konstanten


# use /home/Pascal/Studium/Netze/Projekt1/Abgabe/NetzePS/Receiver/Downloadedfiles/
def excecute_receive(buffersize,downloadpath):
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
                print('Falsches Paket empfangen')
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


def main():    
    if (len(sys.argv)==3):
        excecute_receive(sys.argv[1],sys.argv[2])
    else : 
        print('\nuse format buffersize,downloadpath ')
        sys.exit()




if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print('\naborting transmission')

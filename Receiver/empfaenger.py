#Pascal Schäfer
import socket
import hashlib
import os
import struct
import time
# Definiere Konstanten
BUFFER_SIZE = 1000
UDP_IP = 'localhost' #  127.0.0.1'
UDP_PORT = 5005

# Erstelle UDP-Socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))



# Empfangen des ersten Pakets
header, addr = sock.recvfrom(BUFFER_SIZE*2+10)
starttransmit = time.time()
trans_id, seq_num, max_seq= struct.unpack('!HLL', header[:10])
filename = header[10:].decode()




with open(filename, 'wb') as f:
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


with open(filename, 'rb') as f:
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
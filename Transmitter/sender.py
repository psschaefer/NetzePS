#Pascal Schäfer
import socket
import hashlib
import os
import struct

# Definiere Konstanten
BUFFER_SIZE = 1024
UDP_IP = 'localhost'    #127.0.0.1'
UDP_PORT = 5005

# Wähle die zu sendende Datei
filename = 'example.txt'

# Erstelle UDP-Socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# Bestimme die maximale Sequenznummer

max_seq = os.path.getsize(filename) // BUFFER_SIZE
if os.path.getsize(filename) % BUFFER_SIZE != 0:
    max_seq += 1


#Für letztes Paket mit md5
max_seq +=1

# Sende das erste Paket mit der Dateiinformation
trans_id = 1234 # Wähle eine Transmission ID
seq_num = 0
filename_len = len(filename)
header = struct.pack('!HLL', trans_id, seq_num, max_seq) + filename.encode()
sock.sendto(header, (UDP_IP, UDP_PORT))




# Sende die Datenpakete
with open(filename, 'rb') as f:
    for i in range(max_seq-1):
        data = f.read(BUFFER_SIZE)
        seq_num += 1
        packet = struct.pack('!HL', trans_id, seq_num) + data
        sock.sendto(packet, (UDP_IP, UDP_PORT))




# Sende das letzte Paket mit dem MD5-Hash
with open(filename, 'rb') as f:
    data = f.read()

md5 = hashlib.md5(data).digest()
packet = struct.pack('!HL', trans_id, max_seq) + md5
sock.sendto(packet, (UDP_IP, UDP_PORT))
print()
# Schließe den Socket
sock.close()

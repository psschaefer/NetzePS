# how to run rn

- might change later

run in seperate terminals:

1. execute 'python empfaenger.py' with buffersize(1024) and the downloadpath you want to store the file at
2. execute 'python sender.py 1234 5005 127.0.0.1 example.txt' 
with (1234 as TransmissionID) (5005 as port) (127.0.0.1 as IP-addres)-here buffersie(1024) localhost (example.txt as filname with type ending)

or javac and then :
1. java receiver.java 1024 Downloadfolderpath
2. java sender.java 1234 5005 localhost 1024 testfilepath 


# Battleship (Amiral Battı)

Bu proje Bilgisayar Ağları dersi kapsamında geliştirilmiştir.  
Java kullanılarak Server-Client mimarisi ile gerçek zamanlı oyun oynanabilir.

## Çalıştırmak için:

1. `server/BattleshipServer.java` dosyasını çalıştırın (AWS veya lokal).
2. İki adet `client/BattleshipClient.java` çalıştırarak bağlanın.
3. Gemi yerleştirin, sıra size gelince atış yapın!

## Kullanılan Teknolojiler:
- Java
- Swing
- Socket Programming
- AWS

## AWS :

![image](https://github.com/user-attachments/assets/6e8b0332-b2d6-4e1b-8514-db355df5f96c)

![image](https://github.com/user-attachments/assets/5b8bdc16-2bcf-40cb-ab4c-c086c11829c8)

![image](https://github.com/user-attachments/assets/961187ce-e666-4649-b85e-457874beead2)

![image](https://github.com/user-attachments/assets/1dec4858-36e5-4054-b3f9-ba09b60f7d4d)

![image](https://github.com/user-attachments/assets/2a85bae9-2a1c-427a-adca-485e1877d45d)

![image](https://github.com/user-attachments/assets/1d1cbe02-d5ae-499a-8c1a-94445e241e35)


## AWS CONNECTION :
1 : 
sudo apt update
sudo apt install default-jdk git -y
2 :
git clone https://github.com/aymansaeid/BattleshipGame.git
3 :
cd ~/BattleshipGame
4 : compile the Java code
cd src/main/java
javac server/*.java common/*.java
5 : Run the server
java server.BattleshipServer
## for Update and Relaunch Your Game :

cd ~/BattleshipGame

git pull

cd src/main/java
javac server/*.java common/*.java

java server.BattleshipServer

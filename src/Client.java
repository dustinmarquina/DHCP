import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;

public class Client {
    private InetAddress host;
    private int port;
    private static ClientEventListener clientEventListener;
    static ArrayList<MyFile> myFiles = new ArrayList<>();
    private DatagramSocket client;
    private static int fileIdCounter = 1;
    private DatagramSocket secondarySocket; // For UDP
    private Thread secondarySocketThread;
    private String serverPort;// Thread for handling it

    private void notifyServerConnected(String initialAddress) {
        clientEventListener.onServerConnected(initialAddress);
    }

    private static void notifyServerDisconnected(String clientName) {
        clientEventListener.onServerDisconnected(clientName);
    }

    private static void notifyServerUpdated(String assignedIP) {
        clientEventListener.onServerUpdated(assignedIP);
    }

    public void setClientEventListener(ClientEventListener clientEventListener) {
        Client.clientEventListener = clientEventListener;
    }

    public Client(InetAddress host, int port) {
        this.host = host;
        this.port = port;
    }

    public void execute() throws IOException {
        client = new DatagramSocket();
        handleOption1();

        startSecondarySocket();

        Alive alive = new Alive(client, host, port);
        alive.start();
    }

    private void startSecondarySocket() {
        try {
            secondarySocket = new DatagramSocket();
            serverPort = String.valueOf(secondarySocket.getLocalPort());
            secondarySocketThread = new Thread(() -> {
                try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                        secondarySocket.receive(packet);

                        String message = new String(packet.getData(), 0, packet.getLength());
                        System.out.println("Received on secondary socket: " + message);
                        startSendingMyFiles(packet.getAddress(), packet.getPort());

                } catch (IOException e) {
                    System.out.println("Error in secondary socket: " + e.getMessage());
                } finally {
                    if (secondarySocket != null && !secondarySocket.isClosed()) {
                        secondarySocket.close();
                    }
                }
            });
            // Start the thread
            secondarySocketThread.start();
            System.out.println("Secondary socket listening on port 5555...");
        } catch (SocketException e) {
            System.out.println("Failed to start secondary socket: " + e.getMessage());
        }
    }


    public void startSendingMyFiles(InetAddress targetHost, int targetPort) {
        try (DatagramSocket socket = new DatagramSocket()) {
            for (MyFile file : myFiles) {
                byte[] fileData = serialize(file);

                DatagramPacket packet = new DatagramPacket(fileData, fileData.length, targetHost, targetPort);
                socket.send(packet);
                System.out.println("Sent file: " + file.getName());
            }
        } catch (IOException e) {
            System.out.println("Error in SendThread: " + e.getMessage());
        }
    }

    private byte[] serialize(MyFile file) throws IOException {
        try (java.io.ByteArrayOutputStream byteStream = new java.io.ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(file);
            return byteStream.toByteArray();
        }
    }

    public void uploadFile() {
        System.out.println("Select a file to upload:");
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                String fileName = file.getName();
                String fileExtension = getFileExtension(fileName);

                MyFile myFile = new MyFile(fileIdCounter++, fileName, fileData, fileExtension);
                myFiles.add(myFile);

                clientEventListener.updateTable(myFile);

                System.out.println("File uploaded successfully: " + fileName);
            } catch (IOException e) {
                System.out.println("Failed to upload file: " + e.getMessage());
            }
        } else {
            System.out.println("File selection cancelled.");
        }
    }



    private String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1).toLowerCase();
        }
        return "unknown";
    }

    public void handleOption1() {
        try {
            sendDiscover();
            String offerResponse = receive(client).trim();
            System.out.println("Server Response (Offer): " + offerResponse);

            if (offerResponse.startsWith("OFFER")) {
                String offeredIp = offerResponse.split(" ")[1];

                sendRequest(offeredIp);
                String ackResponse = receive(client).trim();
                System.out.println("Server Response (ACK/DECLINE): " + ackResponse);

                notifyServerUpdated(ackResponse);

                if (ackResponse.startsWith("ACK")) {
                    System.out.println("IP successfully leased: " + offeredIp);
                } else if (ackResponse.equals("DECLINED")) {
                    System.out.println("Server declined the IP. Retrying...");
                    handleOption1();
                }
            } else {
                System.out.println("No IPs available or invalid offer.");
            }
        } catch (IOException e) {
            System.out.println("Error during DHCP workflow: " + e.getMessage());
        }
    }


    private String receive(DatagramSocket server) throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        server.receive(packet);
        String response = new String(packet.getData(), 0, packet.getLength());
        notifyServerUpdated(response);
        return response;
    }

    private DatagramPacket createPacket(String value) {
        byte[] buffer = value.getBytes();
        return new DatagramPacket(buffer, buffer.length, host, port);
    }


    private void sendDiscover() throws IOException {
        client.send(createPacket("DISCOVER"));
        System.out.println("Sent DHCP discover.");
    }

    private void sendRequest(String offeredIp) throws IOException {
        client.send(createPacket("REQUEST " + offeredIp));
        System.out.println("Sent DHCP request for IP: " + offeredIp);
    }


    public void sendDnsQuery(String domainName) {
        try {
            client.send(createPacket("DNS_RESOLVE " + domainName));
            String response = receive(client).trim();
            System.out.println("DNS Query Response: " + response);
        } catch (IOException e) {
            System.out.println("Error sending DNS query: " + e.getMessage());
        }
    }

    public void sendDnsRegister(String domainName, String ipAddress) {
        try {
            client.send(createPacket("DNS_REGISTER " + domainName + " " + ipAddress + " " + serverPort));
            String response = receive(client).trim();
            System.out.println("DNS Registration Response: " + response);
        } catch (IOException e) {
            System.out.println("Error registering domain: " + e.getMessage());
        }
    }

}
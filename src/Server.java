import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    public static final int DEFAULT_LEASE_DURATION = 10;
    private int port;
    private InetAddress clientIP;
    private int clientPort;
    public static Map<Data, String> listSocket;
    private static List<ServerEventListener> listeners;

    private static final Map<String, ClientInfo> dnsTable = new HashMap<>();

    private static final Set<String> ipPool = new HashSet<>(Arrays.asList(
            "192.168.1.2", "192.168.1.3", "192.168.1.4", "192.168.1.5",
            "192.168.1.6", "192.168.1.7", "192.168.1.8", "192.168.1.9"
    ));
    private final Set<String> offeredIps = new HashSet<>();

    private static final Set<String> publicIpPool = new HashSet<>(Arrays.asList(
            "203.0.113.1", "203.0.113.2", "203.0.113.3", "203.0.113.4", "203.0.113.5"
    ));
    private static final Map<String, String> natTable = new HashMap<>();

    public Server(int port) {
        this.port = port;
        listSocket = new HashMap<>();
        listeners = new ArrayList<>();
    }



    public void execute() throws IOException {
        DatagramSocket server = new DatagramSocket(port);
        System.out.println("Listening on port " + port);

        ClientAlive clientAlive = new ClientAlive();
        clientAlive.start();

        while (true) {
            String data = receive(server).trim();
            System.out.println("Received: " + data);

            String clientName = clientIP.getHostAddress() + ":" + clientPort;

            String[] parts = data.split(" ", 2);
            String command = parts[0];
            String argument = (parts.length > 1) ? parts[1] : null;

            if (command.equals("alive")) {
                for (Map.Entry<Data, String> entry : listSocket.entrySet()) {
                    InetAddress hostTemp = entry.getKey().getHost();
                    int portTemp = entry.getKey().getPort();
                    if (hostTemp.equals(clientIP) && portTemp == clientPort) {
                        entry.getKey().setTimeRemain(DEFAULT_LEASE_DURATION);
                        System.out.println(clientName + " alive");
                    }
                }
                continue;
            }

            if (!checkExistClient(clientIP, clientPort)) {
                Data dataTemp = new Data(clientIP, clientPort);
                listSocket.put(dataTemp, "");
                notifyClientConnected(clientName, "Connected", "N/A", 0);
            }

            switch (command) {
                case "DISCOVER" -> handleDiscover(server);
                case "REQUEST" -> {
                    if (argument != null) {
                        handleRequest(server, argument);
                    } else {
                        System.out.println("Invalid REQUEST command: Missing IP argument.");
                    }
                }
                case "DNS_REGISTER" -> {
                    if (argument != null) {
                        String[] args = argument.split(" ");
                        if (args.length == 3) {
                            handleDnsRegister(server, args[0], args[1], args[2]); // domainName, ipAddress
                        } else {
                            System.out.println("Invalid DNS_REGISTER command format.");
                        }
                    } else {
                        System.out.println("Invalid DNS_REGISTER command: Missing arguments.");
                    }
                }
                case "DNS_RESOLVE" -> {
                    if (argument != null) {
                        handleDnsResolve(server, argument); // domainName
                    } else {
                        System.out.println("Invalid DNS_RESOLVE command: Missing domain name.");
                    }
                }
                case "3" -> handleExit(clientName);
                case "NAT_QUERY" -> handleNatQuery(server);
                default -> System.out.println("Invalid command received: " + command);
            }
        }
    }

    private void handleDnsRegister(DatagramSocket server, String domainName, String ipAddress, String serverPort) throws IOException {
        synchronized (dnsTable) {
            dnsTable.put(domainName, new ClientInfo(ipAddress, clientIP, clientPort, serverPort)); // Store registering client info
            sendData("REGISTERED: " + domainName + " -> " + ipAddress + " " + serverPort, server, clientIP, clientPort);
            System.out.println("Domain registered: " + domainName + " -> " + ipAddress);
        }
    }

    private void handleDnsResolve(DatagramSocket server, String domainName) throws IOException {
        synchronized (dnsTable) {
            ClientInfo clientInfo = dnsTable.get(domainName);
            System.out.println("Domain resolved: " + clientInfo.getClientIP());
            if (clientInfo != null) {
                String accessedMessage = "RESOLVED: " + domainName + " -> " + clientInfo.getResolvedIp() +
                        " " + clientInfo.getServerPort();
                sendData(accessedMessage, server, clientIP, clientPort);
                System.out.println("Resolved domain: " + domainName + " -> " + clientInfo.getResolvedIp());

            } else {
                sendData("ERROR: Domain not found", server, clientIP, clientPort);
                System.out.println("Failed to resolve domain: " + domainName);
            }
        }
    }





    private void handleNatQuery(DatagramSocket server) throws IOException {
        synchronized (natTable) {
            StringBuilder response = new StringBuilder("NAT Table:\n");
            for (Map.Entry<String, String> entry : natTable.entrySet()) {
                response.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
            }
            sendData(response.toString(), server, clientIP, clientPort);
            System.out.println("NAT Table sent to client.");
        }
    }

    private String getClientIp(InetAddress clientIP, int clientPort) {
        for (Map.Entry<Data, String> entry : listSocket.entrySet()) {
            Data data = entry.getKey();
            if (data.getHost().equals(clientIP) && data.getPort() == clientPort) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void handleExit(String clientName) {
        listSocket.entrySet().removeIf(entry -> entry.getKey().equals(new Data(clientIP, clientPort)));
        notifyClientDisconnected(clientName);
        System.out.printf("Disconnected: %s%n", clientName);
    }

    private void updateClientData(String allocatedIp) {
        for (Map.Entry<Data, String> entry : listSocket.entrySet()) {
            if (entry.getKey().equals(new Data(clientIP, clientPort))) {
                entry.setValue(allocatedIp);
            }
        }
    }


    private String receive(DatagramSocket server) throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        server.receive(packet);
        clientPort = packet.getPort();
        clientIP = packet.getAddress();
        return new String(packet.getData()).trim();
    }

    private void handleDiscover(DatagramSocket server) throws IOException {
        String offeredIp = allocateIp();
        if (offeredIp == null) {
            sendData("No IPs available", server, clientIP, clientPort);
            return;
        }

        offeredIps.add(offeredIp); // Add to the offered set
        sendData("OFFER " + offeredIp, server, clientIP, clientPort);
        System.out.println("Offered IP: " + offeredIp);
    }

    private void handleRequest(DatagramSocket server, String requestedIp) throws IOException {
        System.out.println("Received REQUEST for IP: " + requestedIp);

        synchronized (offeredIps) {
            if (!offeredIps.contains(requestedIp)) {
                sendData("DECLINED", server, clientIP, clientPort);
                System.out.println("Declined IP not in offered set: " + requestedIp);
                return;
            }
            offeredIps.remove(requestedIp);
        }

        String oldIp = getClientIp(clientIP, clientPort);
        if (oldIp != null && !oldIp.isEmpty()) {
            releasePublicIp(oldIp);
            ipPool.add(oldIp);
            System.out.printf("Returned old IP: %s for client: %s%n", oldIp, clientIP.getHostAddress());
        }

        updateClientData(requestedIp);

        String publicIp = allocatePublicIp(requestedIp);
        if (publicIp == null) {
            sendData("No Public IPs available", server, clientIP, clientPort);
            System.out.println("Public IP pool exhausted.");
            return;
        }

        sendData("ACK " + requestedIp + " -> " + publicIp, server, clientIP, clientPort);
        System.out.printf("Acknowledged IP: %s with Public IP: %s%n", requestedIp, publicIp);

        String clientName = clientIP.getHostAddress() + ":" + clientPort;
        notifyClientUpdated(clientName,  requestedIp , publicIp, DEFAULT_LEASE_DURATION);
    }



    public String allocateIp() {
        if (ipPool.isEmpty()) return null;
        Iterator<String> iterator = ipPool.iterator();
        String ip = iterator.next();
        ipPool.remove(ip);
        return ip;
    }

    private String allocatePublicIp(String privateIp) {
            if (natTable.containsKey(privateIp)) {
                return natTable.get(privateIp);
            }
            if (!publicIpPool.isEmpty()) {
                Iterator<String> iterator = publicIpPool.iterator();
                String publicIp = iterator.next();
                publicIpPool.remove(publicIp);
                natTable.put(privateIp, publicIp);
                return publicIp;
            }
            return null;
    }

    private static void releasePublicIp(String privateIp) {
            if (natTable.containsKey(privateIp)) {
                String publicIp = natTable.remove(privateIp);
                publicIpPool.add(publicIp);
                System.out.printf("Released Public IP: %s%n", publicIp);
        }
    }


    private void sendData(String value, DatagramSocket server, InetAddress clientIP, int clientPort) throws IOException {
        byte[] buffer = value.getBytes();
        server.send(new DatagramPacket(buffer, buffer.length, clientIP, clientPort));
    }

    private boolean checkExistClient(InetAddress clientIP, int clientPort) {
        return listSocket.containsKey(new Data(clientIP, clientPort));
    }



    public void addServerEventListener(ServerEventListener listener) {
        listeners.add(listener);
    }

    private void notifyClientConnected(String clientName, String status, String assignedIP, int leaseTimeLeft) {
        for (ServerEventListener listener : listeners) {
            listener.onClientConnected(clientName, status, assignedIP, leaseTimeLeft);
        }
    }

    private static void notifyClientDisconnected(String clientName) {
        for (ServerEventListener listener : listeners) {
            listener.onClientDisconnected(clientName);
        }
    }

    private static void notifyClientUpdated(String clientName, String status, String assignedIP, int leaseTimeLeft) {
        for (ServerEventListener listener : listeners) {
            listener.onClientUpdated(clientName, status, assignedIP, leaseTimeLeft);
        }
    }

    static class ClientAlive extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (Server.listSocket) {
                        reclaimExpiredLeases();
                    }
                    Thread.sleep(1000); // Check leases every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void reclaimExpiredLeases() {
            Iterator<Map.Entry<Data, String>> iterator = Server.listSocket.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Data, String> entry = iterator.next();
                Data client = entry.getKey();
                if (client.getTimeRemain() <= 0) {
                    String releasedIp = entry.getValue();
                    if (releasedIp != null && !releasedIp.isEmpty()) {
                        ipPool.add(releasedIp);
                        releasePublicIp(releasedIp);
                        System.out.printf("Reclaimed IP: Private %s%n", releasedIp);
                    }
                    iterator.remove();
                    Server.notifyClientDisconnected(client.getHost().getHostAddress() + ":" + client.getPort());
                }
            }
        }
    }
}

class Data {
    private InetAddress host;
    private int port;
    private int timeRemain;

    public Data(InetAddress host, int port) {
        this.host = host;
        this.port = port;
        this.timeRemain = Server.DEFAULT_LEASE_DURATION;
    }

    public InetAddress getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeRemain() {
        return timeRemain;
    }

    public void setTimeRemain(int timeRemain) {
        this.timeRemain = timeRemain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return port == data.port && host.equals(data.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
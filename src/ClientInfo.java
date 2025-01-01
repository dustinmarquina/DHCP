import java.net.InetAddress;

class ClientInfo {
    private String resolvedIp;
    private InetAddress clientIP;
    private int clientPort;
    private String serverPort;


    public ClientInfo(String resolvedIp, InetAddress clientIP, int clientPort, String serverPort) {
        this.resolvedIp = resolvedIp;
        this.clientIP = clientIP;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }

    public String getResolvedIp() {
        return resolvedIp;
    }

    public InetAddress getClientIP() {
        return clientIP;
    }

    public int getClientPort() {
        return clientPort;
    }

    public String getServerPort() {
        return serverPort;
    }
}
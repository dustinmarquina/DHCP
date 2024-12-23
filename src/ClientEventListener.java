public interface ServerEventListener {
    void onClientConnected(String clientName, String status, String assignedIP, int leaseTimeLeft);
    void onClientDisconnected(String clientName);
    void onClientUpdated(String clientName, String status, String assignedIP, int leaseTimeLeft);
}

public interface ClientEventListener {
    void onServerConnected(String initialAddress);
    void onServerDisconnected(String clientName);
    void onServerUpdated(String assignedAddress);
    void updateTable(MyFile file);
}

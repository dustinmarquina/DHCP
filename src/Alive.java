import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class Alive extends Thread{
    private DatagramSocket client;
    private InetAddress host;
    private int port;

    public Alive(DatagramSocket client, InetAddress host, int port) {
        this.client = client;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run()  {
        while(true){
            try {
                client.send(createPacket("alive"));
                Thread.sleep(5000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private DatagramPacket createPacket(String value) {
        byte[] buffer = value.getBytes();
        return new DatagramPacket(buffer, buffer.length, host, port);
    }
}

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;

public class ServerGUI extends JFrame implements ServerEventListener {
    private JTable clientTable;
    private JPanel mainPanel;
    private JTable table1;
    private DefaultTableModel tableModel;

    public ServerGUI() {
        mainPanel = new JPanel(new BorderLayout());
        clientTable = new JTable();
        initializeTable();
        mainPanel.add(new JScrollPane(clientTable), BorderLayout.CENTER);

        setContentPane(mainPanel);
        setTitle("DHCP Server");
        setSize(450, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void initializeTable() {
        tableModel = new DefaultTableModel(
                new Object[][]{},
                new String[]{"Client Name", "Private IP", "Public IP", "Lease Time Left"}
        );
        clientTable.setModel(tableModel);
    }

    @Override
    public void onClientConnected(String clientName, String status, String assignedIP, int leaseTimeLeft) {
                tableModel.addRow(new Object[]{clientName, status, assignedIP, leaseTimeLeft});
    }

    @Override
    public void onClientDisconnected(String clientName) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0).equals(clientName)) {
                    tableModel.removeRow(i);
                    break;
                }
            }
    }

    @Override
    public void onClientUpdated(String clientName, String status, String assignedIP, int leaseTimeLeft) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0).equals(clientName)) {
                    tableModel.setValueAt(status, i, 1);
                    tableModel.setValueAt(assignedIP, i, 2);
                    tableModel.setValueAt(leaseTimeLeft, i, 3);
                }
            }
    }

    private void createUIComponents() {
        clientTable = new JTable();
    }

    public static void main(String[] args) {
        Server server = new Server(15797);
        ServerGUI serverGUI = new ServerGUI();

        server.addServerEventListener(serverGUI);

        new Thread(() -> {
            try {
                server.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;

public class ClientGUI extends JFrame implements ClientEventListener{
    private JButton automateButton;
    private JButton domainNameButton;
    private JButton exitButton;
    private JTextField textField1;
    private JPanel rightPanel;
    private JPanel leftPanel;
    private JLabel currentIPLb;
    private JLabel privateIpLbl;
    private JLabel desiredIPtxt;
    private JTextField domainNamelTf;
    private JPanel mainPane;
    private JLabel publicIpLbl;
    private JLabel domainNameTxt;
    private JButton queryButton;
    private JButton chooseFileButton;
    private JTable table1;
    private JPanel bottomPanel;
    private DefaultTableModel tableModel;
    private static Client client;

    public ClientGUI() {
        automateButton.setPreferredSize(new Dimension(100, 50)); // Set width: 100, height: 50
        domainNameButton.setPreferredSize(new Dimension(100, 50));
        queryButton.setPreferredSize(new Dimension(100, 50));
        exitButton.setPreferredSize(new Dimension(100, 50));
        tableModel = new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Name", "Extension"}
        );
        table1 = new JTable();
        table1.setModel(tableModel);
        mainPane = new JPanel();
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(table1), BorderLayout.CENTER);
        bottomPanel.add(chooseFileButton, BorderLayout.EAST);
        mainPane.add(leftPanel, BorderLayout.CENTER);
        mainPane.add(rightPanel, BorderLayout.WEST);
        mainPane.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(mainPane);
        setTitle("DHCP Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);

        automateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.handleOption1();
            }
        });
        domainNameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!domainNamelTf.getText().isEmpty())
                System.out.println("domain name: " + domainNamelTf.getText());
                client.sendDnsRegister(domainNamelTf.getText(),privateIpLbl.getText().trim());
                System.out.println("private Iplbl: " + privateIpLbl.getText().trim());
                domainNamelTf.setText("");
            }
        });
        queryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!domainNamelTf.getText().isEmpty()){
                    System.out.println("domain name: " + domainNamelTf.getText());
                    client.sendDnsQuery(domainNamelTf.getText().trim());
                    domainNamelTf.getText();
                }
            }
        });
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.uploadFile();
            }
        });
    }

    @Override
    public void onServerConnected(String initialAddress) {
    }

    @Override
    public void onServerDisconnected(String clientName) {

    }

    @Override
    public void onServerUpdated(String response) {
        String[] parts = response.split(" ");
        System.out.println("ON server update " + response);
        if(response.startsWith("ACK")){
            System.out.println("Received ACK");
            String sourceIP = parts[1];
            String destinationIP = (response.length() > 1) ? parts[3] : "";

            System.out.println("Source IP: " + sourceIP);
            System.out.println("Destination IP: " + destinationIP);
            privateIpLbl.setText(sourceIP);
            if(destinationIP != "") {
                publicIpLbl.setText(destinationIP);
            }
        }
        else if(response.startsWith("REGISTERED:")){
            System.out.println("register: "+ parts[1] + " " + parts[3]);
            domainNameTxt.setText(parts[1]);
        }else if (response.startsWith("RESOLVED:")){
            System.out.println("RESOLVED: "+ parts[4]);
            new ClientHostGUI(parts[1], parts[3], parts[4]);
        }
    }

    @Override
    public void updateTable(MyFile myFile) {
        tableModel.addRow(new Object[]{myFile.getId(), myFile.getName(), myFile.getFileExtension()});
    }

    private void createUIComponents() {
        automateButton = new JButton("Automate");
    }

    public static void main(String[] args) {
        client = new Client(InetAddress.getLoopbackAddress(), 15797);
        ClientGUI clientGUI = new ClientGUI();

        client.setClientEventListener(clientGUI);

        new Thread(() -> {
            try {
                client.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}

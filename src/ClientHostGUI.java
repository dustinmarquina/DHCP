import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ClientHostGUI extends JFrame{
    private JLabel domainLbl;
    private JLabel desiredIPtxt;
    private JTextField domainNameLbl;
    private JLabel currentIPLb;
    private JPanel mainPane;
    private JLabel ipLbl;
    private JPanel leftPanel;
    private JPanel bottomPn;
    private JScrollPane jScrollPane;
    private static Client client;
    private static List<MyFile> myFiles = new ArrayList<>();
    private int fileId = 0;
    private String serverPort;



    public ClientHostGUI(String domainName, String ip, String serverPort) {
        this.serverPort = serverPort;
        startReceivingMyFiles();
        bottomPn.setLayout(new BoxLayout(bottomPn, BoxLayout.Y_AXIS));

        domainLbl.setText(domainName);
        ipLbl.setText(ip);
        setContentPane(mainPane);
        setTitle("Client host GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setVisible(true);
    }

    public void startReceivingMyFiles() {
        try {
            ReceiveThread receiveThread = new ReceiveThread();
            receiveThread.start(); // Start the receiving thread
        } catch (IOException e) {
            System.out.println("Error starting ReceiveThread: " + e.getMessage());
        }
    }




    public static void main(String[] args) {
//        ClientHostGUI clientGUI = new ClientHostGUI();

    }
    class ReceiveThread extends Thread {
        private DatagramSocket receiveSocket;

        public ReceiveThread() throws IOException {
            this.receiveSocket = new DatagramSocket();
        }

        private DatagramPacket createPacket(String value) {
            byte[] buffer = value.getBytes();
            return new DatagramPacket(buffer, buffer.length, InetAddress.getLoopbackAddress(), Integer.parseInt(serverPort));
        }

        @Override
        public void run() {
            try {
                while (true) {
                    receiveSocket.send(createPacket("A di da phat"));
                    byte[] buffer = new byte[65535];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    receiveSocket.receive(packet);

                    try (ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
                         ObjectInputStream objectStream = new ObjectInputStream(byteStream)) {
                        MyFile receivedFile = (MyFile) objectStream.readObject();
                        String fileName = receivedFile.getName();
                        System.out.println("Received file: " + receivedFile.getName());
                        JPanel jpFileRow = new JPanel();
                        jpFileRow.setLayout(new BoxLayout(jpFileRow, BoxLayout.X_AXIS));
                        JLabel jlFileName = new JLabel(fileName);
                        jlFileName.setFont(new Font("Arial", Font.BOLD, 20));
                        jlFileName.setBorder(new EmptyBorder(10,0, 10,0));
                        if (getFileExtension(fileName).equalsIgnoreCase("txt")) {
                            jpFileRow.setName((String.valueOf(fileId)));
                            jpFileRow.addMouseListener(getMyMouseListener());
                            jpFileRow.add(jlFileName);
                            bottomPn.add(jpFileRow);
                            bottomPn.validate();
                        } else {
                            jpFileRow.setName((String.valueOf(fileId)));
                            jpFileRow.addMouseListener(getMyMouseListener());
                            jpFileRow.add(jlFileName);
                            bottomPn.add(jpFileRow);
                            bottomPn.validate();
                        }
                        myFiles.add(new MyFile(fileId, fileName, receivedFile.getData(), getFileExtension(fileName)));
                        fileId++;

                        for(MyFile file : myFiles) {
                            System.out.println(file);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error in ReceiveThread: " + e.getMessage());
            } finally {
                receiveSocket.close();
            }
        }
        }
    public static String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        } else {
            return "No extension found.";
        }
    }

    public static MouseListener getMyMouseListener() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPanel jPanel = (JPanel) e.getSource();
                int fileId = Integer.parseInt(jPanel.getName());
                for (MyFile myFile : myFiles) {
                    if (myFile.getId() == fileId) {
                        JFrame jfPreview = createFrame(myFile.getName(), myFile.getData(), myFile.getFileExtension());
                        jfPreview.setVisible(true);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        };
    }

    public static JFrame createFrame(String fileName, byte[] fileData, String fileExtension) {

        JFrame jFrame = new JFrame("File Downloader");
        jFrame.setSize(400, 400);

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));

        // Title above panel.
        JLabel jlTitle = new JLabel("WittCode's File Downloader");
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        jlTitle.setFont(new Font("Arial", Font.BOLD, 25));
        jlTitle.setBorder(new EmptyBorder(20,0,10,0));

        JLabel jlPrompt = new JLabel("Are you sure you want to download " + fileName + "?");
        jlPrompt.setFont(new Font("Arial", Font.BOLD, 20));
        jlPrompt.setBorder(new EmptyBorder(20,0,10,0));
        jlPrompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbYes = new JButton("Yes");
        jbYes.setPreferredSize(new Dimension(150, 75));
        jbYes.setFont(new Font("Arial", Font.BOLD, 20));

        JButton jbNo = new JButton("No");
        jbNo.setPreferredSize(new Dimension(150, 75));
        jbNo.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel jlFileContent = new JLabel();
        jlFileContent.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel jpButtons = new JPanel();
        jpButtons.setBorder(new EmptyBorder(20, 0, 10, 0));
        jpButtons.add(jbYes);
        jpButtons.add(jbNo);

        if (fileExtension.equalsIgnoreCase("txt")) {
            jlFileContent.setText("<html>" + new String(fileData) + "</html>");
        } else {
            jlFileContent.setIcon(new ImageIcon(fileData));
        }

        jbYes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File fileToDownload = new File(fileName);
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                    fileOutputStream.write(fileData);
                    fileOutputStream.close();
                    jFrame.dispose();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }
        });

        // No so close window.
        jbNo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jFrame.dispose();
            }
        });

        jPanel.add(jlTitle);
        jPanel.add(jlPrompt);
        jPanel.add(jlFileContent);
        jPanel.add(jpButtons);

        jFrame.add(jPanel);

        return jFrame;

    }

    }


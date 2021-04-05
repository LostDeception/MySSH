import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.prompt.BuddySupport;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class main extends JFrame {

    // Static SSH session variable
    private static Session session;
    private static ChannelExec channel;
    private final JSch jsch = new JSch();
    private String password = "";

    // Static list to hold all user input controls
    private static final List<JXTextField> userInput = new ArrayList<>();

    // Static list to hold user SSH credentials
    private static final List<objCredentials> credentials = new ArrayList<>();

    // Color variables
    private final Color header = new Color(43, 50, 61);
    private final Color body = new Color(55, 65, 79);
    private final Color footer = new Color(43, 50, 61);
    private final Color border = new Color(73, 85, 105);
    private final Color caret = Color.WHITE;
    private final Color foreground = new Color(162, 185, 219);
    private final Color textSelection = new Color(55, 65, 79);

    // JFrame controls
    private JPanel panel1, panel2, panel3;
    private JXButton btnConnect;
    private JXTextArea console;
    private JXTextField input;
    private JXTextField iHost, iUser, iPw, iPort;


    // Local storage variables
    private final Preferences userPreferences = Preferences.userRoot();

    public static void main(String[] args) {
        new main().setVisible(true);
    }

    private main() {
        super("MySSH");
        createWindow();
        windowListeners();
    }

    private void createWindow() {
        setSize(900, 600);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        panel1 = new JPanel();
        panel2 = new JPanel();
        panel3 = new JPanel();

        // Panel #1 Visual/Components
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        flowLayout.setVgap(10);
        flowLayout.setHgap(10);
        panel1.setLayout(flowLayout);
        panel1.setPreferredSize(new Dimension(this.getWidth(), 50));
        panel1.setBackground(header);
        iHost = createCredential("Host:");
        iUser = createCredential("User:");
        iPw = createCredential("Password:");
        iPort = createCredential("Port:");
        iPort.setPreferredSize(new Dimension(100, 30));
        btnConnect = new JXButton("Connect");
        btnConnect.setPreferredSize(new Dimension(96, 30));
        btnConnect.setToolTipText("Start Server");
        panel1.add(btnConnect);
        this.add(panel1, BorderLayout.NORTH);

        // Panel #2 Visual/Components
        panel2.setLayout(new BorderLayout());
        console = new JXTextArea();
        console.setEditable(false);
        console.setFocusable(false);
        console.setBackground(body);
        Border bodyBorder = new LineBorder(body, 10, false);
        console.setBorder(bodyBorder);
        console.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        console.setForeground(foreground);
        console.setSelectionColor(textSelection);
        console.setSelectedTextColor(foreground);
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(console);
        Border bodyScrollBorder = new LineBorder(border, 1, false);
        scrollPane.setBorder(bodyScrollBorder);
        panel2.add(scrollPane);
        this.add(panel2, BorderLayout.CENTER);

        // Panel #3 Visual/Components
        panel3.setLayout(new BorderLayout());
        panel3.setPreferredSize(new Dimension(this.getWidth(), 35));
        panel3.setBackground(footer);
        input = new JXTextField();
        input.setForeground(foreground);
        input.setBackground(footer);
        input.setSelectionColor(textSelection);
        input.setSelectedTextColor(foreground);
        input.setCaretColor(caret);
        Border primary = new MatteBorder(0,1,1,1, border);
        Border empty = new EmptyBorder(0, 5, 0, 0);
        Border border = new CompoundBorder(primary, empty);
        input.setBorder(border);
        input.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        panel3.add(input, BorderLayout.CENTER);
        this.add(panel3, BorderLayout.SOUTH);
    }

    /**
     * create JLabel and JTextarea for user to
     * input SSH credentials into. Return textfield
     * for getting and modifying values.
     */
    private JXTextField createCredential(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.BLACK);
        label.setBorder(BorderFactory.createCompoundBorder(
                label.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JXTextField field = new JXTextField();
        field.addBuddy(label, BuddySupport.Position.LEFT);
        field.setPreferredSize(new Dimension(210, 30));
        field.setForeground(Color.BLACK);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setMargin(new Insets(3,3,3,3));
        field.setBorder(null);
        userInput.add(field);
        panel1.add(field);
        return field;
    }

    private void windowListeners(){

        // When window first opens up focus input
        this.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e){
                iHost.setText(userPreferences.get("SESSION_HOST", ""));
                iUser.setText(userPreferences.get("SESSION_USER", ""));
                password = userPreferences.get("SESSION_PW", "");
                iPw.setText("*".repeat(password.length()));
                iPort.setText(String.valueOf(userPreferences.getInt("SESSION_PORT", 0)));
                input.requestFocus();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                userPreferences.put("SESSION_HOST", iHost.getText());
                userPreferences.put("SESSION_USER", iUser.getText());
                userPreferences.put("SESSION_PW", iPw.getText());
                if(!iPort.getText().equals("")) {
                    userPreferences.putInt("SESSION_PORT", Integer.parseInt(iPort.getText()));
                }
            }
        });

        // When user clicks the console focus the input
        console.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                input.requestFocus();
            }
        });

        // When user clicks button attempt to connect to SSH from given credentials
        btnConnect.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                sessionConnect();
            }
        });

        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == 10) {
                    executeCommand(input.getText());
                    input.setText("");
                }
            }
        });

        iPw.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if(Character.isLetter(e.getKeyChar()) ||
                    Character.isDigit(e.getKeyChar()) ||
                    containsSpecialCharacter(String.valueOf(e.getKeyChar()))){
                    password = iPw.getText();
                    String mask = password.length() > 0 ? "*".repeat(password.length()) : "*".repeat(0);
                    iPw.setText(mask);
                }
            }
        });
    }

    private void sessionConnect() {
        try {
            String username = iUser.getText();
            String host = iHost.getText();
            int port = !iPort.getText().equals("") ? Integer.parseInt(iPort.getText()) : 0;
            String password = iPw.getText();
            if(port != 0) {
                session = jsch.getSession(username, host, port);
            } else {
                session = jsch.getSession(username, host);
            }
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            if(session.isConnected()) {
                sendConsoleMsg("Session successfully connected!");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            sendConsoleMsg(e.toString());
        }
    }

    private void executeCommand(String command){

        if(!session.isConnected()) {
            sessionConnect();
        }

        byte[] buffer = new byte[1024];

        try{
            channel = (ChannelExec) session.openChannel("exec");
            InputStream in = channel.getInputStream();
            channel.setCommand(command);
            channel.connect();

            String line = "";
            while (true){
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    line = new String(buffer, 0, i);
                    sendConsoleMsg(line);
                }

                if(line.contains("logout")){
                    break;
                }

                if (channel.isClosed()){
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored){}
            }
        }catch(Exception e){
            System.out.println("Error while reading channel output: "+ e);
        }
    }

    private void sendConsoleMsg(String text) {
        String msg = StringUtils.normalizeSpace(text);
        console.append(msg+"\n");
    }

    public boolean containsSpecialCharacter(String s) {
        return s != null && s.matches("[^A-Za-z0-9 ]");
    }
}

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.TimerTask;
import javax.swing.*;

public class TestClient extends JFrame {

    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private JPanel panel;
    private JButton[] buttons;
    private JLabel[][] matris;
    private Socket socket;
    private int port = 4567;
    private String ip = "10.2.1.122";
    private static int idCounter = 0;
    private int id;
    private volatile int x = 0;
    private volatile int y = 0;
    private Buffer<String> stringBuffer = new Buffer<>();
    private Timer timer;
    private JTextField textField;
    private JButton doneButton;
    private JButton startButton;
    private Game game;
    private JLabel pointsLabel;
    private JLabel points;
    private String name;

    public TestClient(Game game){
        super("Pong Panic");
        this.game = game;
        panel = new JPanel();
        panel.setLayout(new GridLayout(7,5));
        addLabels();
        addButtons();
        addTextField();
        addTwoButtons();
        addTwoLabels();
        add(panel);
        setResizable(false);
        setSize(500, 700);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ActionListener listener = new Bounce();
        addBounce(listener);
        try{
            socket = new Socket(ip, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
            new Thread(new Writer()).start();
            ois = new ObjectInputStream(socket.getInputStream());
            new Thread(new Reader()).start();
        } catch(IOException e){
            e.getStackTrace();
        }
        id = idCounter;
        idCounter++;
    }

    public void addLabels(){
        matris = new JLabel[5][5];
        for (int y = 0; y < 5; y++) {
            for(int x = 0; x < 5; x++){
                matris[x][y] = new JLabel();
                matris[x][y].setPreferredSize(new Dimension((100),(100)));
                matris[x][y].setHorizontalAlignment(SwingConstants.CENTER);
                matris[x][y].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                panel.add(matris[x][y]);
            }
        }
        setVisible(true);
    }

    public void addButtons(){
        buttons = new JButton[5];
        for (int i = 0; i < 5; i++) {
            buttons[i] = new JButton();
            buttons[i].setPreferredSize(new Dimension(100,100));
            buttons[i].setEnabled(true);
            panel.add(buttons[i]);
        }
    }
    public void addTwoButtons() {
        doneButton = new JButton();
        doneButton.setEnabled(true);
        doneButton.setText("DONE");
        doneButton.addActionListener(new SendName());
        panel.add(doneButton);
        startButton = new JButton();
        startButton.setEnabled(true);
        startButton.setText("START");
        startButton.addActionListener(new StartGame());
        panel.add(startButton);
    }
    public void addTwoLabels() {
        pointsLabel = new JLabel();
        pointsLabel.setPreferredSize(new Dimension((100),(100)));
        pointsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pointsLabel.setText("POÄNG:");
        panel.add(pointsLabel);
        points = new JLabel();
        points.setPreferredSize(new Dimension((100),(100)));
        points.setHorizontalAlignment(SwingConstants.CENTER);
        points.setText("0");
        panel.add(points);
    }
    public void addTextField() {
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(100,100));
        panel.add(textField);
    }
    private class SendName implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            name = textField.getText();
            stringBuffer.put(textField.getText());
        }
    }
    private class StartGame implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            stringBuffer.put("start");
        }
    }
    public void addBounce(ActionListener listener){
        for (int i = 0; i < 5; i++) {
            buttons[i].addActionListener(listener);
        }
    }

    private class Bounce implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < 5; i++) {
                if (buttons[i] == e.getSource() && ((i == x && id == 0 && y == 9) || (i == 4-x && id == 1 && y == 0))) {
                    int time = timer.getCounter();
                    if (time <= game.getDelay()) {
                        stringBuffer.put("timer:" + time + ":" + id);
                    }
                    return;
                }
            }
        }
    }

    public class Writer implements Runnable {
        @Override
        public void run() {
            try{
                while(true) {
                    oos.writeObject(stringBuffer.get());
                }
            } catch(IOException e){
                e.getStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public class Reader implements Runnable {

        @Override
        public void run() {
            String inputLine;
            try{
                while(true) {
                    inputLine = (String) ois.readObject();
                    if (inputLine.equals("reset")) {
                        resetLights();
                    } else if (inputLine.startsWith("points")) {
                        String[] array = inputLine.split(":");
                        String sentPoints = array[1];
                        int sentY = Integer.parseInt(array[2]);
                        if ((sentY == 1 && id == 1) || (sentY == 8 && id == 0)) {
                            points.setText(sentPoints);
                        }
                    } else {
                        String[] array = inputLine.split(",");
                        x = Integer.parseInt(array[0]);
                        y = Integer.parseInt(array[1]);
                        if (id == 0 && y > 4) {
                            matris[x][y-5].setText("O");
                        } else if(id == 1 && y < 5 ) {
                            matris[4-x][4-y].setText("O");
                        }
                        if (y == 0 || y == 9) {
                            timer = new Timer();
                        }
                    }
                }
            }catch(IOException | ClassNotFoundException e){
                e.getStackTrace();
            }
        }
    }
    private void resetLights() {
        for (int i = 0; i < matris.length; i++) {
            for (int j = 0; j < matris[i].length; j++) {
                matris[i][j].setText("");
            }
        }
    }
}

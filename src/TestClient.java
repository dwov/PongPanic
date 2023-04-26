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
    private String ip = "192.168.1.41";
    private static int idCounter = 0;
    private int id;
    private volatile int x = 0;
    private volatile int y = 0;
    private Buffer<String> stringBuffer = new Buffer<>();
    private Timer timer;

    public TestClient(){
        super("Pong Panic");
        panel = new JPanel();
        panel.setLayout(new GridLayout(6,5));
        addLabels();
        addButtons();
        add(panel);
        setResizable(false);
        setSize(500, 600);
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
    public void addBounce(ActionListener listener){
        for (int i = 0; i < 5; i++) {
            buttons[i].addActionListener(listener);
        }
    }

    private class Bounce implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < 5; i++) {
                if (buttons[i] == e.getSource() && ((i == x && id == 0 && y == 9) || (i == 4-x && id == 1 && y == 0))) {
                    stringBuffer.put("timer:" + timer.getCounter() + ":" + id);
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
            Object o;
            try{
                while(true) {
                    o = ois.readObject();
                    if (id == 0 && y > 4) {
                        matris[x][y-5].setText("");
                    } else if(id == 1 && y < 5) {
                        matris[4-x][(4-y)].setText("");
                    }
                    if(o instanceof String){
                        String[] array = ((String) o).split(",");
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
}

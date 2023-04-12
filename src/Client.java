import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import javax.swing.*;

public class Client extends JFrame {

    private ObjectInputStream ois;
    private JPanel panel;
    private JButton[] buttons;
    private JLabel[][] matris;

    private Socket socket;
    private int port = 4567;
    private String ip = "10.2.16.19";
    private static int idCounter = 0;
    private int id;

    public Client(){
        super("Pong-tjofräs");
        panel = new JPanel();
        panel.setLayout(new GridLayout(6,5));
        addLabels();
        addButtons();
        add(panel);
        setResizable(false);
        setSize(500, 600);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        try{
            socket = new Socket(ip, port);
            ois = new ObjectInputStream(socket.getInputStream());
            new Thread(new Connection()).start();
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

    public class Connection implements Runnable {

        @Override
        public void run() {
            Object o;
            int x = 0;
            int y = 0;
            try{
                while(true){
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
                           matris[4-x][(4-y)].setText("O");
                       }
                    }
                }
            }catch(IOException | ClassNotFoundException e){
                e.getStackTrace();
            }
        }
    }
}

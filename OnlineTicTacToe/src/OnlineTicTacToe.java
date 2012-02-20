/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author hildebj
 */
public class OnlineTicTacToe implements ActionListener {
  private final int INTERVAL = 1000; // 1 second
  private final int NBUTTONS = 9; // #bottons
  private ObjectInputStream input = null; // input from my counterpart
  private ObjectOutputStream output = null; // output from my counterpart
  private JFrame window = null; // the tic-tac-toe window
  private JButton[] button = new JButton[NBUTTONS]; // button[0] - button[9]
  private boolean[] myTurn = new boolean[1]; // T: my turn, F: your turn
  private String myMark = null; // "O" or "X"
  private String yourMark = null; // "X" or "O"


  /**
  * Prints out the usage.
  */
  private static void usage( ) {
    System.err.
    println( "Usage: java OnlineTicTacToe ipAddr ipPort(>=5000)" );
    System.exit( -1 );
  }
  /**
  * Prints out the stack trace upon a given error and quits the application.
  * @param an exception
  */
  private static void error( Exception e ) {
    e.printStackTrace();
    System.exit(-1);
  }
  
  /**
  * Starts the online tic-tac-toe game.
  * @param args[0]: my counterpart's ip address, args[0]: his/her port
  */
  public static void main( String[] args ) {
    // verify the number of arguments
    if ( args.length != 2 ) {
      usage( );
    }
    
    // verify the correctness of my counterpart address
    InetAddress addr = null;
    try {
      addr = InetAddress.getByName( args[0] );
    } catch ( UnknownHostException e ) {
      error( e );
    }
    
    // verify the correctness of my counterpart port
    int port = 0;
    try {
      port = Integer.parseInt( args[1] );
    } catch (NumberFormatException e) {
      error( e );
    }
    if ( port < 5000 ) {
      usage( );
    }
    
    // now start the application
    OnlineTicTacToe game = new OnlineTicTacToe( addr, port );
  }
  
  /**
  * Is the constructor that sets up a TCP connection with my counterpart,
  * brings up a game window, and starts a slave thread for listening to
  * my counterpart.
  * @param my counterpart's ip address
  * @param my counterpart's port
  */
  public OnlineTicTacToe( InetAddress addr, int port ) {
    // set up a TCP connection with my counterpart
    boolean isConnected = createConnection( addr, port );
    
    // set up a window
    makeWindow( !isConnected ); // or makeWindow( false );
    
    // start my counterpart thread
    Counterpart counterpart = new Counterpart( );
    counterpart.start();
  }
  
  private boolean createConnection( InetAddress addr, int port ) {
    ServerSocket serverSocket = null;
    boolean selfReady = false;
    boolean peerReady = false;
    
    try {
      serverSocket = new ServerSocket( port );
      
      // Disable blocking I/O operations, by specifying a timeout
      serverSocket.setSoTimeout( INTERVAL );
    } catch (Exception e) {
      selfReady = true;
    }
    
    try {
      InetAddress peerInetAddress = InetAddress.getLocalHost();
      String peerName = addr.getHostName();
      if (peerName.equals("localhost") || 
              peerName.equals(peerInetAddress.getHostName())) {
        peerReady = true;
      }
    } catch (Exception e) {
      error( e );
    }
    
    Socket socket = null;
    boolean amServer = false;
    
    // round-robin
    while (true) {
      if ( !selfReady ) {
        try {
          socket = serverSocket.accept();
        } catch (SocketTimeoutException ste) {
          // normal for non-blocking accept
          // do nothing
        } catch (IOException ioe) {
          error(ioe);
        }
        if (socket != null) {
          amServer = true;
          break;
        }
      }
      
      if (!selfReady && peerReady) continue;
      try {
        socket = new Socket( addr, port );
      } catch (IOException ioe) {}
      if (socket == null) continue;
      amServer = false;
    }
    
    System.out.println(new StringBuilder().append("amServer = ").
            append(amServer).toString());
    try {
      if (amServer) {
        // if server, read from input stream
        input = new ObjectInputStream(socket.getInputStream());
        output = new ObjectOutputStream(socket.getOutputStream());
        System.out.println((String)input.readObject());
      } else {
        // client will write to output stream
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
        output.writeObject("Hello!");
      }
    } catch (IOException | ClassNotFoundException e) {
      error(e);
    }
    
    return amServer;
  }
  
  /**
  * Creates a 3x3 window for the tic-tac-toe game
  * @param true if this window is created by the 1st player, false by
  * the 2nd player
  */
  private void makeWindow( boolean amFormer ) {
    myTurn[0] = amFormer;
    myMark = ( amFormer ) ? "O" : "X"; // 1st person uses "O"
    yourMark = ( amFormer ) ? "X" : "O"; // 2nd person uses "X"
    
    // create a window
    window = new JFrame("OnlineTicTacToe(" +
             ((amFormer) ? "former)" : "latter)" ) + myMark );
    
    window.setSize(300, 300);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.setLayout(new GridLayout(3, 3));
    
    // initialize all nine cells.
    for (int i = 0; i < NBUTTONS; i++) {
      button[i] = new JButton();
      window.add(button[i]);
      button[i].addActionListener(this);
    }
    
    // make it visible
    window.setVisible(true);
  }
  
  /**
  * Marks the i-th button with mark ("O" or "X")
  * @param the i-th button
  * @param a mark ( "O" or "X" )
  * @param true if it has been marked in success
  */
  private boolean markButton( int i, String mark ) {
    if ( button[i].getText( ).equals( "" ) ) {
      button[i].setText( mark );
      button[i].setEnabled( false );
      return true;
    }
    return false;
  }
  
  /**
  * Checks which button has been clicked
  * @param an event passed from AWT
  * @return an integer (0 through to 8) that shows which button has been
  * clicked. -1 upon an error.
  */
  private int whichButtonClicked( ActionEvent event ) {
    for ( int i = 0; i < NBUTTONS; i++ ) {
      if ( event.getSource( ) == button[i] )
        return i;
      }
    return -1;
  }
  
  /**
  * Checks if the i-th button has been marked with mark( "O" or "X" ).
  * @param the i-th button
  * @param a mark ( "O" or "X" )
  * @return true if the i-th button has been marked with mark.
  */
  private boolean buttonMarkedWith( int i, String mark ) {
    return button[i].getText( ).equals( mark );
  }
  
  /**
  * Pops out another small window indicating that mark("O" or "X") won!
  * @param a mark ( "O" or "X" )
  */
  private void showWon( String mark ) {
    JOptionPane.showMessageDialog( null, mark + " won!" );
  }
  
  /**
  * Is called by AWT whenever any button has been clicked. You have to:
  * <ol>
  * <li> check if it is my turn,
  * <li> check which button was clicked with whichButtonClicked( event ),
  * <li> mark the corresponding button with markButton( buttonId, mark ),
  * <li> check which button was clicked with whichButtonClicked( event ),
  * <li> mark the corresponding button with markButton( buttonId, mark ),
  * <li> send this information to my counterpart,
  * <li> checks if the game was completed with
  * buttonMarkedWith( buttonId, mark )
  * <li> shows a winning message with showWon( )
  */
  @Override
  public void actionPerformed( ActionEvent event ) {
    synchronized (myTurn) {
      while (!myTurn[0]) {
        try {
          myTurn.wait();
        } catch (InterruptedException ie) {
          error(ie);
        }
      }
        int i = whichButtonClicked( event );
        if ( markButton( i, myMark ) ) {
        try {
          output.writeInt(i);
          output.flush();
          System.out.println(new StringBuilder().append("wrote ").append(i).
                  append(" to counterpart").toString());
        } catch (IOException ioe) {
          error(ioe);
        }
      } else {
        return;
      }
    
      checkWon(myMark);
    
      myTurn[0] = false;
      myTurn.notify();
    }
  }
  
  private void checkWon(String mark) {
    // check vertical
    for (int i = 0; i < 3; i++) {
      if (buttonMarkedWith(i, mark) && buttonMarkedWith(i + 3, mark) && 
              buttonMarkedWith(i + 6, mark)) {
        showWon(mark);
        return;
      }
    }
    // check horizontal
    for (int i = 0; i < 7; i = i + 3) {
      if (buttonMarkedWith(i, mark) && buttonMarkedWith(i + 1, mark) && 
              buttonMarkedWith(i + 2, mark)) {
        showWon(mark);
        return;
      }
    }
    // check diagonal 0 4 8
    if (buttonMarkedWith(0, mark) && buttonMarkedWith(4, mark) && 
            buttonMarkedWith(8, mark)) {
      showWon(mark);
      return;
    }
    // check diagonal 2 4 6
    if (buttonMarkedWith(2, mark) && buttonMarkedWith(4, mark) && 
            buttonMarkedWith(6, mark)) {
      showWon(mark);
    }
  }
  
  /**
  * This is a reader thread that keeps reading from and behaving as my
  * counterpart.
  */
  
  private class Counterpart extends Thread {
    // prevent Counterpart being explicitly instantiated by callers
    // only constructed internally by parent class
    private Counterpart() {}
    
    @Override
    public void run( ) {
      while (true)
        try {
          System.out.println("waiting for counterpart...");
          int i = input.readInt();
          System.out.println("counterpart's position = " + i);
          
          synchronized (myTurn) {
            if (!myTurn[0])
            try {
              // wait for peer to act
              myTurn.wait();
            } catch (InterruptedException ie) {
              // woken up by peer
            } 
            markButton(i, yourMark);
            checkWon(yourMark);
            myTurn[0] = true;
            myTurn.notify();
          }
       } catch (EOFException eofe) {
       } catch (IOException ioe) {}
    }
  }
}

/**
 * This program runs as a server and controls the force to be applied to balance the Inverted Pendulum system running on the clients.
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Math;

public class ControlServer {

    private static ServerSocket serverSocket;
    private static final int port = 25533;

    /**
     * Main method that creates new socket and PoleServer instance and runs it.
     */
    public static void main(String[] args) throws IOException {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            System.out.println("unable to set up port");
            System.exit(1);
        }
        System.out.println("Waiting for connection");
        do {
            Socket client = serverSocket.accept();
            System.out.println("\nnew client accepted.\n");
            PoleServer_handler handler = new PoleServer_handler(client);
        } while (true);
    }
}

/**
 * This class sends control messages to balance the pendulum on client side.
 */
class PoleServer_handler implements Runnable {
    // Set the number of poles
    private static final int NUM_POLES = 1;

    static ServerSocket providerSocket;
    Socket connection = null;
    ObjectOutputStream out;
    ObjectInputStream in;
    String message = "abc";
    static Socket clientSocket;
    Thread t;
    double initialPos;
    boolean initialSet;
    double tempPos;
    double targetPos = 0.5;
    int frameCounter = 0;
    boolean fulfilled_a;
    boolean fulfilled_b;
    boolean stop;
    boolean phase_1;
    boolean phase_2;
    /**
     * Class Constructor
     */
    public PoleServer_handler(Socket socket) {

        t = new Thread(this);
        clientSocket = socket;

        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        t.start();
    }
    double angle, angleDot, pos, posDot, action = 0, i = 0;

    /**
     * This method receives the pole positions and calculates the updated value
     * and sends them across to the client.
     * It also sends the amount of force to be applied to balance the pendulum.
     * @throws ioException
     */
    void control_pendulum(ObjectOutputStream out, ObjectInputStream in) {
        try {
            while(true){
                System.out.println("-----------------");

                // read data from client
                Object obj = in.readObject();

                // Do not process string data unless it is "bye", in which case,
                // we close the server
                if(obj instanceof String){
                    System.out.println("STRING RECEIVED: "+(String) obj);
                    if(obj.equals("bye")){
                        break;
                    }
                    continue;
                }
                
                double[] data= (double[])(obj);
                assert(data.length == NUM_POLES * 4);
                double[] actions = new double[NUM_POLES];
 
                // Get sensor data of each pole and calculate the action to be
                // applied to each inverted pendulum
                // TODO: Current implementation assumes that each pole is
                // controlled independently. This part needs to be changed if
                // the control of one pendulum needs sensing data from other
                // pendulums.
                for (int i = 0; i < NUM_POLES; i++) {
                  angle = data[i*4+0];
                  angleDot = data[i*4+1];
                  pos = data[i*4+2];
                  posDot = data[i*4+3];
                  
                  //System.out.println("server < pole["+i+"]: "+angle+"  "
                  //    +angleDot+"  "+pos+"  "+posDot);
		  
		  // Set the initial pos of the cart
                  if (!initialSet) {
                    initialSet = true;
                    initialPos = pos;
		  
                    if (pos == targetPos)
                      tempPos = pos;
                    else
                      tempPos = pos < targetPos ? pos - 1.0 : pos + 1.0;
                  }

                  frameCounter++;
                  actions[i] = calculate_action(angle, angleDot, pos, posDot);
                  System.out.println("Angle: " + angle + " AngleVel: " + angleDot + 
                    " Pos: " + pos + " PosVel: " + posDot);
                  System.out.println("Action: " + actions[i]);

                }

                sendMessage_doubleArray(actions);

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            if (clientSocket != null) {
                System.out.println("closing down connection ...");                
                out.writeObject("bye");
                out.flush();
                in.close();
                out.close();
                clientSocket.close();
            }
        } catch (IOException ioe) {
            System.out.println("unable to disconnect");
        }

        System.out.println("Session closed. Waiting for new connection...");

    }

    /**
     * This method calls the controller method to balance the pendulum.
     * @throws ioException
     */
    public void run() {

        try {
            control_pendulum(out, in);

        } catch (Exception ioException) {
            ioException.printStackTrace();
        } finally {
        }

    }

    // Calculate the actions to be applied to the inverted pendulum from the
    // sensing data.
    // TODO: Current implementation assumes that each pole is controlled
    // independently. The interface needs to be changed if the control of one
    // pendulum needs sensing data from other pendulums.
    double calculate_action(double angle, double angleDot, double pos, double posDot) {
      double action = 0;
       // if (angle > 0 && angleDiff < 0) {
      
    // angleDot = Math.abs(angleDot);
       if (angle > 0) {
           if (angle > 65 * 0.01745) {
               action = 10;
           } else if (angle > 60 * 0.01745) {
               action = 8;
           } else if (angle > 50 * 0.01745) {
               action = 7.5;
           } else if (angle > 30 * 0.01745) {
               action = 4;
           } else if (angle > 20 * 0.01745) {
               action = 2;
           } else if (angle > 10 * 0.01745) {
               action = 0.5;
           } else if(angle >5*0.01745 && posDot < 0.6){
               action = 0.2;
           } else if(angle >5*0.01745 && posDot > 0.6){
               action = -0.1;
           } else if(angle > 2*0.01745 && posDot < 0.6){
               action = 0.1;
           } else if(angle > 2*0.01745 && posDot > 0.6){
               action = 0;
           } else {
               action = 0;
           }
       } else if (angle < 0) {
           if (angle < -65 * 0.01745) {
               action = -10;
           } else if (angle < -60 * 0.01745) {
               action = -8;
           } else if (angle < -50 * 0.01745) {
               action = -7.5;
           } else if (angle < -30 * 0.01745) {
               action = -4;
           } else if (angle < -20 * 0.01745) {
               action = -2;
           } else if (angle < -10 * 0.01745) {
               action = -0.5;
           } else if(angle <-5*0.01745 && posDot > -0.6){
               action = -0.2;
           } else if(angle <-5*0.01745 && posDot < -0.6){
               action = 0.1;
           } else if(angle <-2*0.01745 && posDot > -0.6){
               action = -0.1;
           } else if(angle <-2*0.01745 && posDot < -0.6){
               action = 0;
           } else {
               action = 0;
           }
       } else {
           action = 0;
       }

       // Add force to the cart every 10 frames if within the range of its initial position, else 30 frames
       int frameStop;
       if (initialPos < targetPos - .2 || initialPos > targetPos + .2) {
        frameStop = 10;
       } else {
        //action = 0;
        frameStop = 10;
       }

        
      if (frameCounter > frameStop) {
          frameCounter = 0;


       // TODO: Try to ease the cart to the targetPos by setting a temporary target position that is only 1.0 away from current pos (or less if the target Position is less than 1.0 away)
       // So instead of the line below, this is supposed to set another temp position when it passes the old temp position checkpoint

/*        tempPos = pos;
        if (pos > targetPos) {
          if(pos - 1 > targetPos){
            tempPos = pos - 1;
          }
          else{
            tempPos = targetPos;
          }
        } else if (pos < targetPos) {
          if(pos + 1 < targetPos){
            tempPos = pos + 1;
          }
          else{
            tempPos = targetPos;
          }
        }*/
        tempPos = targetPos;


        // TODO I set the targetPos to -1.0 (which is the initialPos). Change it to 2.0 to see behavior when it tries to move to target. If targetPos is set to initial, it just goes closer to the left over time. If target is set to 2.0, it eventually crashes into a wall after reaching targetPos.
	// This was our attempt of slowing down the cart as it gets closer to the checkpoint.

        if((pos + 0.75 < targetPos || pos- 0.75 > targetPos)){
/*            for(int i = 0; i < 20; i++)
            System.out.println("ALL ABOARD\n");*/

/*            if (pos < tempPos) {*/
                if (pos  < tempPos - 1.0){
                  action = -0.20;
                }
                else if(pos < tempPos - 0.75){
                  action = -0.17;
                }
                else if (pos < tempPos - 0.5) {
                  action = -0.14;
                }
                else if(pos < tempPos - 0.3){
                  action = -0.11;
                }
                else if(pos < tempPos - 0.2){
                  action = -0.08;
                }
                else if(pos < tempPos - 0.1){
                  action = -0.05;
                }
/*            } else if (pos > tempPos) {
                if (pos > tempPos + 1.0){
                  action = 0.05;
                }
                else if(pos > tempPos + 0.75){
                  action = 0.1;
                }
                else if (pos > tempPos + 0.5) {
                  action = 0.15;
                }
                else if(pos > tempPos + 0.3){
                  action = 0.25;
                }
                else if(pos > tempPos + 0.2){
                  action = 0.375;
                }
                else if(pos > tempPos + 0.1){
                  action = 0.5;
                }
            } */
          }






 /*        else if((pos > targetPos - 0.25 && pos < targetPos + 0.25)){
            for(int i = 0; i<20; i++)
              System.out.println("WELCOMETOTHESHADOWREALM\n");
            action = 0;
          } 
          else if((pos > targetPos - 0.75 && pos < targetPos + 0.25)){
            for(int i = 0; i<20; i++)
              System.out.println("slowdownfam\n");
            double ratio = 1.2;
            if (pos < tempPos) {
                if (pos < tempPos - 0.1){
                  action = ratio * 0.05;
                }
                else if(pos < tempPos - 0.2){
                  action = ratio *0.1;
                }
                else if (pos < tempPos - 0.3) {
                  action = ratio *0.15;
                }
                else if(pos < tempPos - 0.5){
                  action = ratio *0.25;
                }
                else if(pos < tempPos - 0.75){
                  action = ratio * 0.375;
                }
                else if(pos < tempPos - 1.0){
                  action = ratio * 0.5;
                }
            } else if (pos > tempPos) {
                if (pos > tempPos + 0.1){
                  action = -ratio *0.05;
                }
                else if(pos > tempPos + 0.2){
                  action = -ratio *0.1;
                }
                else if (pos > tempPos + 0.3) {
                  action = -ratio *0.15;
                }
                else if(pos > tempPos + .5){
                  action = -ratio *0.25;
                }
                else if(pos > tempPos + 0.75){
                  action = -ratio *0.375;
                }
                else if(pos > tempPos + 1.0){
                  action = -ratio *0.5;
                }
            } 
          } 
          else if((pos > targetPos + 0.25)){
            for(int i = 0; i<20; i++)
              System.out.println("gohomefam\n");
            double ratio = 1.2;
            if (pos < tempPos) {
                if (pos < tempPos - 0.1){
                  action = ratio * 0.05;
                }
                else if(pos < tempPos - 0.2){
                  action = ratio *0.1;
                }
                else if (pos < tempPos - 0.3) {
                  action = ratio *0.15;
                }
                else if(pos < tempPos - 0.5){
                  action = ratio *0.25;
                }
                else if(pos < tempPos - 0.75){
                  action = ratio * 0.375;
                }
                else if(pos < tempPos - 1.0){
                  action = ratio * 0.5;
                }
            } else if (pos > tempPos) {
                if (pos > tempPos + 0.1){
                  action = ratio *0.05;
                }
                else if(pos > tempPos + 0.2){
                  action = ratio *0.1;
                }
                else if (pos > tempPos + 0.3) {
                  action = ratio *0.15;
                }
                else if(pos > tempPos + .5){
                  action = ratio *0.25;
                }
                else if(pos > tempPos + 0.75){
                  action = ratio *0.375;
                }
                else if(pos > tempPos + 1.0){
                  action = ratio *0.5;
                }
            } 
          } */

    } 
// Old code
/*
       if(pos < 0 && angle > 2 * 0.01745){
          action = action * .75;
       }
       else if (pos > 0 && angle < -2 * 0.01745){
          action = action * .75;
       }

      //action = (angle * 7.5);
      if (angle == 0) return 0;

      // Parabolic equation based upon values we were given initially.
      action = .109834 * angle + 8.69182 * angle; 
      if (angle < 0) {
        if (angleDot < .25) {
          action = 2;
        }
      } else {
        if (angleDot > .25) {
          action = -2;
        }
      }
*/
/*      if(pos + 0.25 >= targetPos){
        action = 0;
        for(int i = 0; i < 50; i++)
          System.out.println("@@@###@@#@#@#\n");
      }*/

/*      if(!fulfilled_a && pos > targetPos - 0.25)
      {
        fulfilled_a = true;
        action = -posDot;
        for(int i = 0; i < 50; i++)
            System.out.println("AAAAAAAAAAAAAAAAAA");
      }
      if(!fulfilled_b && pos > targetPos - 0.125)
      {
        fulfilled_b = true;
        action = 1.5 * posDot;
        for(int i = 0; i < 50; i++)
          System.out.println("BBBBBBBBBBBBBBBBBBBB");
      }*/
        
      if(pos > targetPos - 0.375 && !phase_1 && !phase_2)
      {
          for(int i = 0; i < 50; i++)
            System.out.println("PHASE 1 BEGIN \n");
            phase_1 = true;
      }
      if(phase_1)
      {
        for(int i = 0; i < 50; i++)
            System.out.println("APPLIED PHSYICS \n");
        action =  0.38;
      }
      if(phase_1 && posDot >= 1.00){
        for(int i = 0; i < 50; i++)
            System.out.println("PHASE 1 ENDED \n");
        phase_1 = false;
        phase_2 = true;
      }
/*      if(stop)
        action = 0;
      else if(pos > targetPos - 0.25)
      {
        action = -posDot;
        stop = true;
      }*/
      return action;
   }

    /**
     * This method sends the Double message on the object output stream.
     * @throws ioException
     */
    void sendMessage_double(double msg) {
        try {
            out.writeDouble(msg);
            out.flush();
            //System.out.println("server>" + msg);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * This method sends the Double message on the object output stream.
     */
    void sendMessage_doubleArray(double[] data) {
        try {
            out.writeObject(data);
            out.flush();
            
            //System.out.print("server> ");
            for(int i=0; i< data.length; i++){
                //System.out.print(data[i] + "  ");
            }
            System.out.println();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }


}

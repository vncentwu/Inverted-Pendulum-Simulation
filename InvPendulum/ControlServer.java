/**
 * This program runs as a server and controls the force to be applied to balance the Inverted Pendulum system running on the clients.
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Math;
import java.net.InetAddress;
import java.util.GregorianCalendar;

public class ControlServer {

    private static ServerSocket serverSocket;
    private static final int port = 25533;
    long startTime;
    long firstResponse;
    

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
    boolean turningLeft = false;
    boolean turningRight = false;
    double targetPos = 0.0;
    int anchor = 0;
    int frameCounter = 0;
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
                  /*if (!initialSet) {
                    initialSet = true;
                    initialPos = pos;
		  
                    if (pos == targetPos)
                      tempPos = pos;
                    else
                      tempPos = targetPos < pos ? pos - 1.0 : pos + 1.0;
                  }*/
                  /*if (!initialSet) {
                     if (pos < targetPos || turningRight) {
                        actions[i] = turnRight(angle, angleDot, posDot);
                     } else if (pos > targetPos || turningLeft) {
                        actions[i] = turnLeft(angle, angleDot, posDot);
                     }
                     initialSet = true; 
                  } else if (turningLeft) {
                     actions[i] = turnLeft(angle, angleDot, posDot);
                  } else if (turningRight) {
                     actions[i] = turnRight(angle, angleDot, posDot);  
                  } else {
		  */
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
 
   enum Direction {
     CENTER, LEFT, RIGHT
   }

   Direction curDirection;

   Direction getDirection(double pos, double target, double range) { 
      if (target - range <= pos && target + range >= pos) 
         return Direction.CENTER;
               
      else if (target < pos) 
         return Direction.LEFT;
      
      else 
         return Direction.RIGHT;
   }

  long getDelay()
   {
      try {
        String ipAddress = "127.0.0.1";
        InetAddress inet = InetAddress.getByName(ipAddress);
   
        System.out.println("Sending Ping Request to " + ipAddress);
   
        long finish = 0;
        long start = System.nanoTime();
   
        if (inet.isReachable(5000)){
          finish = System.nanoTime();
          return (finish - start)/1000;
/*          for(int i = 0; i < 1000; i++)
            System.out.println("Ping RTT: " + (finish - start + "ns"));*/
        } else {
          System.out.println(ipAddress + " NOT reachable.");
        }
    } catch ( Exception e ) {
      System.out.println("Exception:" + e.getMessage());
    }
    return 0;
  }
   

   double balance(double mr_angle, double angleDot, double posDot) {
      System.out.println("Dropping anchor.");
     
      double angle = mr_angle + angleDot * getDelay()/1000;



      double action = 0.0;

      if (posDot > 3.0) {
         return -posDot;
      } 

      else if (posDot < -3.0) {
         return -posDot;
      }

      if(isAngleDanger(angle, angleDot)) {
 	 if (angle > 60 * 0.01745) {
            action = 5.0;
	 } else if (angle > 30 * 0.01745) {
	    action = 3.0;
	 } else if (angle < -60 * 0.01745) {
            action = -5.0;
	 } else if (angle < -30 * 0.01745) {
            action = -3.0;
	 }
      }

      else if (Math.abs(angleDot) < 1.0) {  
         if (angle > 10 * 0.01745) {
            action = 1.0;
         } else if (angle > 0) {
            action = 0.5;
         } else if (angle < -10 * .01745) {
            action = -1.0;
         } else if (angle < 0) {
            action = -0.5;
         }
      } else {
         if ((angleDot) < 0) {
            action = -1.0;
         } else {
            action = 1.0;
         }
      }
 
      return action;
   }
   
   double left(double angle, double angleDot) {
      System.out.println("LEFT.");
      if (angle > -5 * 0.01745) {
         return 0.3;
      }
      else
        return -100; /*else if (angle > 0.0 && angleDot > 0) {
         return angleDot;
      }

      return -.3;*/
   }

   double right(double angle, double angleDot) {
      System.out.println("RIGHT.");
      if (angle < 5 * 0.01745) {
         return -.3;
      }
      else
        return -100;

     /* else if (angle < 0.0 && angleDot < 0) {
         return angleDot;
      } */
/*
      return .3;*/
   }

   double speed(double angle, double angleDot, double pos, double posDot) {
      	 
      Direction dir = getDirection(pos, targetPos, 0.3);     
      if (dir == Direction.CENTER || (anchor > 0 && anchor < 50)) {
         anchor += 1;
         return balance(angle, angleDot, posDot);
      } else if (anchor >= 15) {
        anchor = 0;
	      return balance(angle, angleDot, posDot);
      } else if (dir == Direction.LEFT) {
         double temp = left(angle, angleDot);
         if(temp == -100)
            return balance(angle, angleDot, posDot);
          else
            return temp; 
      } else {
         double temp = right(angle, angleDot);
         if(temp == -100)
            return balance(angle, angleDot, posDot);
          else
            return temp; 
      }

   }
 
   boolean isAngleSafe(double angle, double angleDot) {
      return Math.abs(angle) < 15 * 0.01745 && Math.abs(angleDot) < 0.5;  
   }

   boolean isAngleDanger(double angle, double angleDot) {
      return Math.abs(angle) > 30 * 0.1745 && Math.abs(angleDot) < 1.0;
   }

   boolean isAngleVelSafe(double angleDot) {
      return Math.abs(angleDot) < 0.5;
   }

   boolean isPosSafe(double pos, double posDot) {
      return Math.abs(pos) < 4.0;
   }

   boolean isPosVelSafe(double posDot) {
      return Math.abs(posDot) < 0.5;
   }

    // Calculate the actions to be applied to the inverted pendulum from the
    // sensing data.
    // TODO: Current implementation assumes that each pole is controlled
    // independently. The interface needs to be changed if the control of one
    // pendulum needs sensing data from other pendulums.
    double calculate_action(double angle, double angleDot, double pos, double posDot) {
/*      try{
        Thread.sleep(200);
      }
      catch(InterruptedException ex){

      }*/
      return speed(angle, angleDot, pos, posDot);
      

      // 2. Penedulum cannot fall over.
/*      if (!isPosSafe(pos)) {
         System.out.println("POSITION UNSAFE.");
         if (pos > 4.5) {
            posCheck = 1;
            return revPosDot(-0.5, posDot);
         } else if (posDot < -4.5) {
            return revPosDot(0.5, posDot);
         }

      }
      
      // 3. Keep cart velocity low.
      else if (!isPosVelSafe(posDot)) {
         System.out.println("POSITION VELOCITY UNSAFE.");
         if (posDot > .5) {
            return revPosDot(-0.15, posDot);
         } else if (posDot < -.5) {
            return revPosDot(0.15, posDot);
         } else {
            System.out.println("ERROR: Position Left Unsafe!");
         } 

      }

      // 4. Keep angle velocity low.
      else if (!isAngleVelSafe(angleDot)){ 
         System.out.println("ANGLE VELOCITY UNSAFE.");
         if (angle < 0) {
            return revAngleDot(1.5, angleDot); 
         } else if (angle > 0) {
            return revAngleDot(-1.5, angleDot); 
         } else {
            System.out.println("ERROR: Angle Left Unsafe!");
         }
      } */
      
      // 5. Move cart to target position.
/*      Direction move = getDirection(pos, targetPos, 0.1);
      if (move == Direction.LEFT) {
         action = -1.0;
      } else if (move == Direction.RIGHT) {
         action = 1.0;
      } 
*/
       // if (angle > 0 && angleDiff < 0) {
       /*int angleFrameRate = isAngleSafe(angle, angleDot) ? 10 : 2;
       int posFrameRate = isPosSafe(pos, posDot) ? 15 : 5;
       if (frameCounter / angleFrameRate == 1) {
          double targetDot = angleDot < 0 ? 20 * 0.01745 : -20 * 0.01745; 
          action = revAngleDot(targetDot, angleDot);
       } else if (frameCounter++ / posFrameRate == 1) {
 	  Direction dirToTarget = getDirection(pos, targetPos, 0.1); 
          if (dirToTarget == Direction.CENTER) {
             System.out.println("AT CENTER.");
             tempPos = targetPos;
             action = revPosDot(0.0, posDot);
          } else if (dirToTarget == Direction.LEFT) { 
             System.out.println("GOING LEFT.");
             action = revPosDot(-1.0, posDot);
          } else if(dirToTarget == Direction.RIGHT) {
             System.out.println("GOING RIGHT.");
             action = revPosDot(1.0, posDot);
          }
       }*/
       //action = revPosDot(2.0, angleDot);
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

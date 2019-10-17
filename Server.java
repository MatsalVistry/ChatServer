import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class Server {
    private static int uniqueId;
    private ArrayList<ClientThread> al;

    private SimpleDateFormat sdf;
    private int port;
    private boolean keepGoing;



    public Server(int port) {

        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        al = new ArrayList<ClientThread>();
    }

    public void start() {
        keepGoing = true;
        try
        {
            ServerSocket serverSocket = new ServerSocket(port);
            while(keepGoing)
            {
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();
                if(!keepGoing)
                    break;

                ClientThread t = new ClientThread(socket);

                al.add(t);
                int g=0;
                for(int x=0;x<al.size()-1;x++)
                {
                    if(al.get(al.size()-1).username.equals(al.get(x).username))
                    {
                      al.get(al.size()-1).close();
                      al.remove(al.size()-1);
                      g=1;
                      break;



                    }
                }
                if(g==0)
                t.start();
                g=0;
            }
            try {
                serverSocket.close();
                for(int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch(IOException ioE) {
                    }
                }
            }
            catch(Exception e) {

            }
        }
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: "+ "\n";
            display(msg);
        }
    }

    protected void stop() {
        keepGoing = false;

        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {

        }
    }

    private void display(String msg) {
        String time =  msg;

    }

    private synchronized void broadcast(String message) {
        String time = sdf.format(new Date());
        String messageLf = message + "\n";



        for(int i = al.size(); --i >= 0;) {
            ClientThread ct = al.get(i);
            if(!ct.writeMsg(messageLf)) {
                al.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    synchronized void remove(int id) {
        for(int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // found it
            if(ct.id == id) {
                al.remove(i);
                return;
            }
        }
    }


    public static void main(String[] args) {
        int portNumber = 1500;
        switch(args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");

                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        Server server = new Server(portNumber);
        server.start();
    }

    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        String date;

        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                display(username + " just connected.");
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }

            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        public void run() {
            boolean keepGoing = true;
            while(keepGoing) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }
                String message = cm.getMessage();

                switch(cm.getType()) {

                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected ");
                        for(int x=0;x<=al.size()-1;x++)
                        {
                            al.get(x).writeMsg(username + " disconnected \n");
                        }
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected "+ "\n");
                        for(int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            writeMsg((i+1) + ") " + ct.username+"\n");
                        }
                        break;
                }
            }

            remove(id);
            close();
        }

        private void close() {
            try {
                if(sOutput != null) sOutput.close();
            }
            catch(Exception e) {}
            try {
                if(sInput != null) sInput.close();
            }
            catch(Exception e) {};
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {}
        }


        private boolean writeMsg(String msg) {
            if(!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            }
            catch(IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}


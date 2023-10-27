package Main;

import java.io.*;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author khoi
 */
public class ClientHandler extends Observable {

    String serverName = "localhost";
    int port = 1106;
    Socket mSocket;
    public String mClientName;
    BufferedWriter mBufferedWriter;
    DataInputStream mDataInputStream;
    Thread mThreadWait;
    public ClientHandler(Observer obs) {
        this.addObserver(obs);
    }

    public void Dispose() {
        if(mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        if (mThreadWait != null) {
            mThreadWait.interrupt();
        }
    }

    public boolean Connect() {
        try {
            mSocket = new Socket(serverName, port);
            mDataInputStream = new DataInputStream(mSocket.getInputStream());
            mBufferedWriter = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            WaitResult();
            return true;
        } catch (IOException e) {
            Result result = new Result("", ResultType.ERROR, "Can't connect to server");
            notifyObservers(result);
            return false;
        }
    }

    @Override
    public void notifyObservers(Object arg) {
        super.setChanged();
        super.notifyObservers(arg);
    }

    private void WaitResult() {
        mThreadWait = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String[] sort = mDataInputStream.readUTF().split(";", -1);
                        Result result;
                        if (sort.length == 3) {
                            result = new Result(sort[0], sort[1], sort[2]);
                        } else {
                            String content = "";
                            for (int i=2; i < sort.length; i++) {
                                content += sort[i] + ";";
                            }
                            result = new Result(sort[0], sort[1], content);
                        }
                        notifyObservers(result);
                    }
                } catch (IOException e) {
                    Result result = new Result("",ResultType.ERROR,"Can't connect to server");
                }
            }
        };
        mThreadWait.start();
    }

    public void Login(String clientName) throws UnsupportedEncodingException{
        String request = ActionType.LOGIN + ";" + clientName;
        try {
            mBufferedWriter.write(request + "\n");
            mBufferedWriter.flush();
        } catch (IOException e) {
            Result result = new Result("", ResultType.ERROR, "Cant' connect to server");
            notifyObservers(result);
        }
    }

    public void GetListRoom() {
        String request = ActionType.GET_LIST_ROOM + ";";
        try {
            mBufferedWriter.write(request + "\n");
            mBufferedWriter.flush();
        } catch (IOException e) {
            Result result = new Result("", ResultType.ERROR, "Can't connect to server");
            notifyObservers(result);
        }
    }

    public void JoinRoom(String roomCode) {
        String request = ActionType.JOIN_ROOM + ";" + roomCode;
        try {
            mBufferedWriter.write(request + "\n");
            mBufferedWriter.flush();
        } catch (IOException e) {
            Result result = new Result("", ResultType.ERROR, "Can't connect to server");
            notifyObservers(result);
        }
    }

    public void CreateRoom(String roomName) {
        String request = ActionType.CREATE_ROOM + ";" + roomName;
        try {
            mBufferedWriter.write(request + "\n");
            mBufferedWriter.flush();
        } catch (IOException e) {
            Result result = new Result("", ResultType.ERROR, "Can't connect to server");
            notifyObservers(result);
        }
    }


    public void SendMess(String msg) {
        msg = msg.replaceAll("\n", "<br>");
        String request = ActionType.SEND_MESSAGE + ";" + msg;
        try {
            mBufferedWriter.write(request + "\n");
            mBufferedWriter.flush();
        } catch (IOException e) {
            Result result = new Result("", ResultType.ERROR, "Can't connect to server");
            notifyObservers(result);
        }
    }

    public void LeaveRoom() {
        String request = ActionType.LEAVE_ROOM + ";null";
        try
        {
            mBufferedWriter.write(request + "\n");
            mBufferedWriter.flush();
        } catch (IOException e) {
            Result result = new Result("", ResultType.ERROR, "Can't connect to server");
            notifyObservers(result);
        }
    }

    public void Logout() {
        String request = ActionType.LOGOUT + ";null";
        try {
            mBufferedWriter.write(request + "\n");
            mBufferedWriter.flush();
        } catch (IOException e) {
            Result result = new Result("", ResultType.ERROR, "Can't connect to server");
            notifyObservers(result);
        }
    }
}

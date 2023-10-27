/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Main;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author khoi
 */

public class Client {
    Socket mSocket;
    BufferedReader mBufferedReader;
    DataOutputStream mDataOutputStream;
    public String mClientName;
    public Room mRoom;
    public boolean mLogined = false;

    public Client(Socket socket) throws IOException {
        mSocket = socket;
        mBufferedReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream(), "UTF8"));
        mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
    }

    public String Read() throws IOException {
        if(mBufferedReader.ready()) {
            return mBufferedReader.readLine();
        }
        return null;
    }

    public Boolean Send(String actionType, String resultType, String content) {
        try {
            mDataOutputStream.writeUTF(actionType + ";" + resultType + ";" + content);
            return true;
        } catch (IOException e) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
    }

    public Boolean IsOnline() {
        return Send(ActionType.CHECK_ONLINE, ResultType.OK, "");
    }
}

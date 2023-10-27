package Main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author khoi
 */

public class ServerManager extends Observable {
    int Port = 1106;
    ServerSocket mServerSocket;
    Thread mThreadListen, mThreadProcess;
    ArrayList<Client> mListClient = new ArrayList<>();
    ArrayList<Room> mListRoom = new ArrayList<>();
    ArrayList<Client> mListClientWaitLogout = new ArrayList<>();
    
    public ServerManager(Observer observer) {
        this.addObserver(observer);
    }

    public boolean CreateServerSocket() {
        try {
            mServerSocket = new ServerSocket(Port);
            ListenConnection();
            ListenRequest();
            notifyObservers("Create a Server Socket successful. Waiting for connections ...");
            return true;
        } catch (IOException e) {
            notifyObservers("Can't create a Server Socket");
            return false;
        }
    }

    private void ListenConnection() {
        mThreadListen = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Socket socket = mServerSocket.accept();
                        Client newClient = new Client(socket);
                        mListClient.add(newClient);
                    }
                } catch (IOException e) {
                    notifyObservers("Connection Error");
                }
            }
        });
        mThreadListen.start();
    }

    private void ListenRequest() {
        mThreadProcess = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        CheckRequest();
                        if (mListClientWaitLogout.size() > 0)
                            RemoveClientLogout();
                        Thread.sleep(0);
                    }
                } catch (InterruptedException | IOException e) {
                    Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        };
        mThreadProcess.start();
    }

    void RemoveClientLogout() {
        int size = mListClientWaitLogout.size();
        for (int i=0; i < size; i ++) {
            Client client = mListClientWaitLogout.get(i);
            mListClient.remove(client);
        }
        mListClientWaitLogout.clear();
    }

    private void CheckRequest() throws IOException {
        int size = mListClient.size();
        for (int i=0; i < size; i++) {
            Client client = mListClient.get(i);
            String request = client.Read();
            if (request != null)
                HandleRequest(client, request);
        }
    }


    void HandleRequest(Client client, String request) {
        String[] sort = request.split(";");
        String actionType = sort[0];
        switch (actionType) {
            case ActionType.LOGIN: {
                String clientName = sort[1]; 
                if (CheckExist(clientName)) {
                    client.mClientName = clientName;
                    client.mLogined = true; 
                    client.Send(actionType, ResultType.OK, "OK");
                    notifyObservers(client.mClientName + " has just successfully login");
                } else {
                    client.Send(actionType, ResultType.ERROR, "That name has already been registered");
                }
                break;
            }
            case ActionType.CREATE_ROOM: {
                String roomName = sort[1]; 
                Room room = Generate(roomName);
                mListRoom.add(room);
                client.mRoom = room;
                if (client.Send(actionType, ResultType.OK, room.mRoomCode)) {
                    room.AddClient(client);
                    notifyObservers(client.mClientName + " just create room " + room.mRoomName);
                }
                break;
            }
            case ActionType.GET_LIST_ROOM: { 
                int size = mListRoom.size();
                int rowsPerBlock = 100;
                if (size > 0) {
                    String listRoom = "";
                    int start = 0;
                    int end = 0;
                    int numberBlock = (int) Math.floor(size/(double)rowsPerBlock);
                    for (int i = 0; i < numberBlock; i++) {
                        start = i*rowsPerBlock;
                        end = start + rowsPerBlock;
                        listRoom = "";
                        for (int j = start; j < end; j++) {
                            Room room = mListRoom.get(j);
                            listRoom += room.mRoomCode + "<col>" + room.mRoomName + "<col>" + room.CountClient() + "<col>" + "<row>";
                        }
                        System.out.println("Have sent " + i + " times");
                        client.Send(actionType, ResultType.OK, listRoom); 
                    }
                    for (int i = end; i < size; i++) {
                        Room room = mListRoom.get(i);
                        listRoom += room.mRoomCode + "<col>" + room.mRoomName + "<col>" + room.CountClient() + "<col>" + "<row>";
                    }
                    client.Send(actionType, ResultType.OK, listRoom);
                } else {
                    client.Send(actionType, ResultType.OK, "");
                }
                notifyObservers(client.mClientName + " has just got the room list");
                break;

            }
            case ActionType.JOIN_ROOM: {
                String roomCode = sort[1]; // [7] query: ActionType ; mRoomCode
                boolean success = false;
                int size = mListRoom.size();
                for (int i=0; i < size; i++) {
                    Room room = mListRoom.get(i);
                    if (room.mRoomCode.equals(roomCode)) {
                        room.AddClient(client); 
                        client.mRoom = room;
                        client.Send(actionType, ResultType.OK, roomCode);
                        notifyObservers(client.mClientName + " has joined room " + room.mRoomName);
                        client.mRoom.UpdateNumberClient();
                        client.mRoom.NotifyJustJoinRoom(client);
                        success = true;
                    }
                }
                if (!success) {
                    client.Send(actionType, ResultType.ERROR, "Cant' find the room");
                    notifyObservers(client.mClientName + " can't join room " + roomCode + "\n");
                }
                break;
            }
            case ActionType.SEND_MESSAGE: {
                String contentMsg = "";
                if (sort.length >= 2)
                    contentMsg = sort[1]; 
                client.mRoom.SendToAllClient(client.mClientName, contentMsg);
                notifyObservers(client.mClientName + " sent a message");
                break;
            }
            case ActionType.LEAVE_ROOM: { 
                Room room = client.mRoom;
                room.RemoveClient(client);
                if (room.CountClient() > 0) {
                    room.NotifyJustLeaveRoom(client);
                    room.UpdateNumberClient();
                } else {
                    mListRoom.remove(room);
                }
                client.mRoom = null;
                notifyObservers(client.mClientName + " just left room");
                break;
            }
            case ActionType.LOGOUT: { 
                Room room = client.mRoom;
                if (room != null) {
                    room.RemoveClient(client);
                    if (room.CountClient() > 0) {
                        room.NotifyJustLeaveRoom(client);
                        room.UpdateNumberClient();
                    } else {
                        mListRoom.remove(room);
                    }
                }
                mListClientWaitLogout.add(client);
                notifyObservers(client.mClientName + " just logged out");
                break;
            }
        }
    }

    private Room Generate(String roomName) {
        Room room = new Room();
        room.mRoomName = roomName;
        room.mNoClient = 1;
        room.mRoomCode = GeneralRoomCode();
        return room;
    }

    private String GeneralRoomCode() {
        int countRandom = 0;
        int maxChar = 3;
        String roomCode = "";
        do {
            if(countRandom>50)
                maxChar++;
            roomCode = RandomString(maxChar);
            countRandom++;
        } while (!CheckRoomCode(roomCode));
        return roomCode;
    }

    boolean CheckRoomCode(String roomCode) {
        for (Room room : mListRoom) {
            if(room.mRoomCode.equals(roomCode))
                return false;
        }
            return true;
        }

    String RandomString(int length) {
        String data = "1234567890qwertyuiopasdfghjklzxcvbnm";
        int sizeData = data.length();
        String result = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            result += data.charAt(random.nextInt(sizeData));
        }
        return result;
    }

    private boolean CheckExist(String name) {
        if (mListClient != null) {
            for (Client client : mListClient) {
                if (name.equals(client.mClientName))
                    return false;
            }
            return true;
        }
        return true;
    }
    
    @Override
    public void notifyObservers(Object arg) {
        super.setChanged();
        super.notifyObservers(arg);
    }

    public int countClient() {
        return mListClient.size();
    }

    public int countRoom() {
        return mListRoom.size();
    }

    public void Dispose() throws IOException {
        if(mThreadListen != null) {
            mThreadListen.interrupt();
            mThreadProcess.interrupt();
            mServerSocket.close();
        }
    }


}

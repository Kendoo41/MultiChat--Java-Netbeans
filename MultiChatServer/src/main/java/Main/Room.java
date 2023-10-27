package Main;

import java.util.ArrayList;
/**
 *
 * @author khoi
 */

public class Room {
    public String mRoomCode;
    public String mRoomName;
    public int mNoClient;
    ArrayList<Client> mListClient = new ArrayList<>();

    public void AddClient(Client client) {
        mListClient.add(client);
    }

    public void RemoveClient(Client client) {
        mListClient.remove(client);
    }

    public int CountClient() {
        return mListClient.size();
    }

    public void SendToAllClient (String sender, String content) {
        for (Client client : mListClient) {
            if (!client.Send(ActionType.SEND_MESSAGE, ResultType.OK, sender + ";" + content)) {
                NotifyJustLeaveRoom(client);
            }
        }
    }

    public void NotifyJustLeaveRoom(Client clientLeft) {
        int size = mListClient.size();
        for (int i=0; i < size; i++) {
            Client client = mListClient.get(i);
            if (client != clientLeft) {
                client.Send(ActionType.NOTIFY_JUST_LEAVE_ROOM, ResultType.OK, clientLeft.mClientName);
            }
        }
    }

    public void NotifyJustJoinRoom(Client clientJoin) {
        int size = mListClient.size();
        for (int i = 0; i < size; i++) {
            Client client = mListClient.get(i);
            if (client != clientJoin)
                client.Send(ActionType.NOTIFY_JUST_JOIN_ROOM, ResultType.OK, clientJoin.mClientName);
        }
    }

    public void UpdateNumberClient() {
        int size = mListClient.size();
        for (int i=0; i < size; i++) {
            Client client = mListClient.get(i);
            if (!client.Send(ActionType.UPDATE_NUMBER_CLIENT, ResultType.OK, size + ""))
                NotifyJustLeaveRoom(client);
        }
    }
    
}

package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;


public class SimpleDhtProvider extends ContentProvider {

    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    boolean standAloneMode = false;
    static String node_id = "";
    static TextView tv = null;
    static String firstAVD = "11108";
    static String myPort = "";
    DataBaseHelper helper = null;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    static  Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
    static String successorNode = "";
    static String predecessorNode = "";
    static String greatestNode = "";
    static String smallestNode = "";
    class KeyValue
    {
        String key;
        String value;
        KeyValue(String inKey,String inValue)
        {
            key = inKey;
            value = inValue;
        }
    }

    TreeSet<String> treeOfHashedNodes = new TreeSet<String>();
    TreeSet<String> treeOfActiveNodes = new TreeSet<String>();
    String arrayOfPorts[] = {"5554", "5556", "5558", "5560", "5562"};
    HashMap<String, String> hashToPort = new HashMap<String, String>();

    private static Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    ArrayList<String> socketArrayList = new ArrayList<String>();

    Socket getSocket(int portNumber) {
        try {
            //Log.e(TAG,String.valueOf(portNumber));
            Socket tempSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    portNumber);
            //tempSocket.setSoTimeout(timeoutInMilliseconds);
            return tempSocket;
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Socket Timeout Exception 1");
            e.printStackTrace();
            return null;
        } catch (StreamCorruptedException e) {
            Log.e(TAG, "Stream Corrupted Exception");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            Log.e(TAG, "IOException2");
            e.printStackTrace();
            return null;
        }
    }
    static int clientPort = 0;
    public void startClientServer() {
             {
                 clientPort = Integer.parseInt(hashToPort.get(successorNode));
                 clientPort = (clientPort * 2);
             }
    }

    public String getPredecessor(String node_id) {
        String predecessor = treeOfHashedNodes.lower(node_id);
        if (null == predecessor) {
            predecessor = treeOfHashedNodes.last();
        }
        return predecessor;
    }

    public String getSuccessor(String node_id) {
        String successor = treeOfHashedNodes.higher(node_id);
        if (null == successor) {
            successor = treeOfHashedNodes.first();
        }
        return successor;
    }
    public String getGreatest() {
        return treeOfHashedNodes.last();
    }

    public String getSmallest() {
        return treeOfHashedNodes.first();
    }



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int numberOfRowsDeleted = helper.removeData(selection);
        Log.e(TAG, "delete " + selection + " rows deleted " + numberOfRowsDeleted);
        return numberOfRowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //hash the hashOfKey , if the value of the hash is
        // greater than the current node then send it to its successor
        // If the value of the hash is greater than the greatest node then send it to the root node directly

        //0 if the argument is a string lexicographically equal to this string;
        //a value less than 0 if the argument is a string lexicographically greater than this string
        //and a value greater than 0 if the argument is a string lexicographically less than this string
        String key = (String) values.get(KEY_FIELD);
        String hashOfKey = "";
        Log.e(TAG, "Someone inserting " + key);
        try {
            hashOfKey = genHash(key);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.toString());
        }
        if(standAloneMode ==true)
        {
            //Log.v(TAG, "STANDALONE here insert" + values.toString() + " hash " + hashOfKey);
            Log.v(TAG,"Insert here successful");
            helper.addData(uri, values);
        }
        else if((0 == myPort.compareToIgnoreCase(firstAVD)) &&
        (true == treeOfActiveNodes.isEmpty()))
        {
                Log.v(TAG,"Maybe Insert here successful");
                //Log.v(TAG, "WHY IS THIS STANDALONE insert" + values.toString() + " hash " + hashOfKey);
                helper.addData(uri, values);
        }
        else {
            try {
                //Log.v(TAG, "insert " + key + " hash " + hashOfKey);
                if (successorNode.isEmpty() && predecessorNode.isEmpty()) {
                    helper.addData(uri, values);
                } else {
                    Log.e(TAG, " node_id " + node_id + " predecessorNode " + predecessorNode + "successorNode " + successorNode +
                            " hash key " + hashOfKey + "Actual key " + key + "Greatest Key " + greatestNode);
                    //If first node
                    boolean flag = true;
                    if(hashOfKey.compareTo(greatestNode)>0)
                    {
                        //If it is the greatest node
                        // Insert it at the first node
                        if (node_id.equals(smallestNode) )
                        {
                            helper.addData(uri, values);
                            flag = false;
                        }
                    }
                    else if(hashOfKey.compareTo(smallestNode)<0)
                    {
                        //If it is the greatest node
                        // Insert it at the first node
                        if (node_id.equals(smallestNode) )
                        {
                            helper.addData(uri, values);
                            flag= false;
                        }
                    }
                    else if ((node_id.compareTo(hashOfKey) > 0) &&
                            predecessorNode.compareTo(hashOfKey)<0)
                    {
                        helper.addData(uri, values);
                        flag = false;
                    }
                    if(flag == true)
                    {
                        MsgInterchangeFormat hMsg = new MsgInterchangeFormat(myPort, node_id);
                        hMsg.mtype = MessageType.DATA_TO_INSERT;
                        hMsg.hashOfKey = hashOfKey;
                        hMsg.values = values;
                        Gson gson = new Gson();
                        String constructMessageToSend = gson.toJson(hMsg);
                        Log.e(TAG, "Message Being sent is " + constructMessageToSend);
                        Socket initialMessageSocket = getSocket(clientPort);
                        DataOutputStream outputControl = new DataOutputStream(initialMessageSocket.getOutputStream());
                        outputControl.writeBytes(constructMessageToSend + "\n");
                    }
                }
            }catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
        return uri;
    }

    @Override
    public boolean onCreate() {

        helper = new DataBaseHelper(getContext());
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            for (String port : arrayOfPorts) {
                hashToPort.put(genHash(port), port);
            }
            node_id = genHash(portStr);

            if (myPort.equals(firstAVD)) {
                treeOfHashedNodes.add(node_id);
            } else {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException Exception Thrown " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return false;
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        if(selection.equals("*"))
        {
            if (successorNode.isEmpty() && predecessorNode.isEmpty()) {
                Cursor cursor = helper.queryData(uri, projection, "#", selectionArgs, sortOrder);
                return cursor;
            } else {
                try {
                    MsgInterchangeFormat hMsg = new MsgInterchangeFormat(myPort, node_id);
                    hMsg.mtype = MessageType.QUERY_ALL;
                    hMsg.selection = "*";
                    hMsg.portNumber = myPort;
                    hMsg.predecessor = predecessorNode;
                    Gson gson = new Gson();
                    String constructMessageToSend = gson.toJson(hMsg);
                    Log.e(TAG, "Message Being sent is " + constructMessageToSend);
                    Socket initialMessageSocket = getSocket(Integer.parseInt(firstAVD));
                    DataOutputStream outputControl = new DataOutputStream(initialMessageSocket.getOutputStream());
                    outputControl.writeBytes(constructMessageToSend + "\n");

                    BufferedReader inputFromServer = new BufferedReader(new InputStreamReader(initialMessageSocket.getInputStream()));
                    String messageSentFromServer = inputFromServer.readLine();
                    MsgInterchangeFormat msg = gson.fromJson(messageSentFromServer, MsgInterchangeFormat.class);
                    Log.e(TAG, "Value received at original server is for Query All is " + messageSentFromServer);
                    //http://stackoverflow.com/questions/9917935/adding-rows-into-cursor-manually
                    MergeCursor mergeCursor = null;
                    Cursor cursor = null;
                    MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
                    for(KeyValue kv : msg.alist)
                    {
                        if(kv.key.equals("#"))
                        {
                            continue;
                        }
                        matrixCursor.addRow(new Object[]{kv.key, kv.value});
                    }
                    mergeCursor = new MergeCursor(new Cursor[]{matrixCursor, cursor});
                    return mergeCursor;
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }
        Cursor cursor = helper.queryData(uri, projection, selection, selectionArgs, sortOrder);
        Log.e(TAG, "query "+selection);
        if(selection.equals("@") )
        {
            return cursor;
        }
        if(selection.equals("#") &&
                ((cursor != null) &&
                        (cursor.getCount() == 0)))
        {
            return cursor;
        }
        if((cursor != null) && (cursor.getCount() > 0))
        {
            Log.e(TAG,"Query Successful"+cursor.toString());
            return cursor;
        }
        else
        {
            try {
                MsgInterchangeFormat hMsg = new MsgInterchangeFormat(myPort, node_id);
                hMsg.mtype = MessageType.QUERY;
                hMsg.selection = selection;
                hMsg.portNumber = myPort;
                Gson gson = new Gson();
                String constructMessageToSend = gson.toJson(hMsg);
                Log.e(TAG, "Message Being sent is " + constructMessageToSend);
                Socket initialMessageSocket = getSocket(clientPort);
                DataOutputStream outputControl = new DataOutputStream(initialMessageSocket.getOutputStream());
                outputControl.writeBytes(constructMessageToSend + "\n");

                BufferedReader inputFromServer = new BufferedReader(new InputStreamReader(initialMessageSocket.getInputStream()));
                String messageSentFromServer = inputFromServer.readLine();
                Log.e(TAG,"Value received at original server is " + messageSentFromServer);

                //http://stackoverflow.com/questions/9917935/adding-rows-into-cursor-manually

                MatrixCursor matrixCursor = new MatrixCursor(new String[] { KEY_FIELD, VALUE_FIELD });
                matrixCursor.addRow(new Object[] { selection, messageSentFromServer });
                MergeCursor mergeCursor = new MergeCursor(new Cursor[] { matrixCursor, cursor });
                return mergeCursor;
            }
            catch (IOException e)
            {
                Log.e(TAG,e.toString());
            }
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public enum MessageType {
        HELLO,
        SENDING_SUCC_PRED,
        SENDING_SUCC_PRED_UPDATE,
        QUERY,
        QUERY_ALL,
        DATA_TO_INSERT,
        RETURN_QUERY_ALL,
        INVALID
    }

    private class MsgInterchangeFormat {
        String hashOfKey;
        String portNumber;
        String hashedPortNumber;
        String successor;
        String predecessor;
        String greatest;
        String smallest;
        ContentValues values;
        MessageType mtype;
        String selection;
        ArrayList<KeyValue> alist = new ArrayList<KeyValue>();

        MsgInterchangeFormat() {
            hashOfKey = "";
            portNumber = "";
            hashedPortNumber = "";
            successor = "";
            predecessor = "";
            mtype = MessageType.INVALID;
        }

        MsgInterchangeFormat(String inPortNumber, String inHashedPortNumber) {
            this();
            portNumber = inPortNumber;
            hashedPortNumber = inHashedPortNumber;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            int count = 0;
            Socket initialMessageSocket = getSocket(Integer.parseInt(firstAVD));
            if(null == initialMessageSocket)
            {
                Log.e(TAG,"Socket is NULL @@@");
                standAloneMode = true;
            }
            try {
                while (initialMessageSocket == null && count < 2) {
                    Thread.sleep(500);
                    count++;
                    initialMessageSocket = getSocket(Integer.parseInt(firstAVD));
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted Exception" + e.toString());
            }
            if (initialMessageSocket == null) {
                standAloneMode = true;
            } else {
                try {
                    standAloneMode = false;
                    MsgInterchangeFormat hMsg = new MsgInterchangeFormat(myPort, node_id);
                    hMsg.mtype = MessageType.HELLO;
                    Gson gson = new Gson();
                    String constructMessageToSend = gson.toJson(hMsg);

                    DataOutputStream outputControl = new DataOutputStream(initialMessageSocket.getOutputStream());
                    outputControl.writeBytes(constructMessageToSend + "\n");

                    BufferedReader inputFromServer = null;
                    inputFromServer = new BufferedReader(new InputStreamReader(initialMessageSocket.getInputStream()));
                    String messageSentFromServer = inputFromServer.readLine();
                    MsgInterchangeFormat propReplyMsg = gson.fromJson(messageSentFromServer, MsgInterchangeFormat.class);

                    Log.e(TAG, "Server Message received!@#!@#!@# From " + firstAVD + " " + propReplyMsg.successor + " " + propReplyMsg.predecessor);
                    successorNode= propReplyMsg.successor;
                    predecessorNode= propReplyMsg.predecessor;
                    greatestNode = propReplyMsg.greatest;
                    smallestNode = propReplyMsg.smallest;
                    Log.e(TAG, "Starting clientServer");
                    startClientServer();
                } catch (Exception e) {
                    Log.e(TAG, "Exception occured at Client Task" + e.toString());
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onProgressUpdate(Void... strings) {
            return;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, MsgInterchangeFormat, Void> {
        @Override
        /*
        Override this method to perform a computation on a background thread.
        The specified parameters are the parameters passed to execute(Params...) by the caller of this task
        This method can call publishProgress(Progress...) to publish updates on the UI thread.
         */
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Scanner sc = null;

            try {
                //while (true) {
                Log.e(TAG, "Server Thread Invoked " + myPort);
                Socket listenSocket = null;
                while (true) {
                        listenSocket = serverSocket.accept();
                        BufferedReader inputFromClient = new BufferedReader(new InputStreamReader(listenSocket.getInputStream()));
                        String messageSentFromClient = inputFromClient.readLine();
                        Gson gson = new Gson();
                        MsgInterchangeFormat msg = gson.fromJson(messageSentFromClient, MsgInterchangeFormat.class);
                        Log.e(TAG, msg.mtype.toString() + "Client Message received From " + msg.portNumber + " and " + msg.hashedPortNumber);
                        switch (msg.mtype) {
                            case HELLO:
                                treeOfHashedNodes.add(msg.hashedPortNumber);
                                treeOfActiveNodes.add(msg.portNumber);
                                predecessorNode = getPredecessor(node_id);
                                successorNode = getSuccessor(node_id);
                                greatestNode = getGreatest();
                                smallestNode = getSmallest();
                                startClientServer();
                                Log.e(TAG, "Current Contents of Priority Queue Are " + treeOfHashedNodes.size());
                                for (String treeOfHashedNode : treeOfHashedNodes) {
                                    Log.e(TAG, treeOfHashedNode);
                                }
                                //Send a reply to everybody
                                MsgInterchangeFormat hMsgReply = new MsgInterchangeFormat();
                                hMsgReply.predecessor = getPredecessor(msg.hashedPortNumber);
                                hMsgReply.successor = getSuccessor(msg.hashedPortNumber);
                                hMsgReply.greatest = getGreatest();
                                hMsgReply.smallest = getSmallest();
                                smallestNode = getSmallest();
                                String constructMessageToSend = gson.toJson(hMsgReply);
                                Log.e(TAG, "Message being sent is " + constructMessageToSend);
                                DataOutputStream outputControl = new DataOutputStream(listenSocket.getOutputStream());
                                outputControl.writeBytes(constructMessageToSend + "\n");

                                for (String port : socketArrayList) {
                                    MsgInterchangeFormat hMsg = new MsgInterchangeFormat(myPort, node_id);
                                    hMsg.mtype = MessageType.SENDING_SUCC_PRED_UPDATE;
                                    int portNumber = (Integer.parseInt(port)) / 2;
                                    String hashedPortNumber = genHash(String.valueOf(portNumber));
                                    hMsg.hashedPortNumber = hashedPortNumber;
                                    hMsg.predecessor = getPredecessor(hashedPortNumber);
                                    hMsg.successor = getSuccessor(hashedPortNumber);
                                    hMsg.greatest = getGreatest();
                                    hMsg.smallest = getSmallest();
                                    constructMessageToSend = gson.toJson(hMsg);
                                    Log.e(TAG, "UPDATED Message being sent is " + constructMessageToSend);
                                    Socket msgSocket = getSocket(Integer.parseInt(port));
                                    outputControl = new DataOutputStream(msgSocket.getOutputStream());
                                    outputControl.writeBytes(constructMessageToSend + "\n");
                                }
                                socketArrayList.add(msg.portNumber);
                                break;
                            case SENDING_SUCC_PRED_UPDATE:
                                Log.e(TAG, msg.mtype.toString() + " Updated Successor and Predecessor " + msg.successor + msg.predecessor);
                                predecessorNode = msg.predecessor;
                                successorNode = msg.successor;
                                greatestNode = msg.greatest;
                                smallestNode = msg.smallest;
                                startClientServer();
                                break;
                            case DATA_TO_INSERT:
                                Log.e(TAG, msg.mtype.toString() + " Received data to insert " + (String) msg.values.get(KEY_FIELD));
                                insert(mUri,msg.values);
                                break;
                            case QUERY:
                                Log.e(TAG, msg.mtype.toString() + "Received Query" + msg);
                                Cursor resultCursor = query(mUri, null,
                                        msg.selection, null, null);
                                int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
                                resultCursor.moveToFirst();
                                String returnValue = resultCursor.getString(valueIndex);
                                Log.e(TAG, "Message being sent is " + returnValue);
                                outputControl = new DataOutputStream(listenSocket.getOutputStream());
                                outputControl.writeBytes(returnValue + "\n");
                                break;
                            case RETURN_QUERY_ALL:
                                MsgInterchangeFormat tempReply = new MsgInterchangeFormat();
                                resultCursor = query(mUri, null,
                                        "#", null, null);
                                if (resultCursor.moveToFirst() ){
                                    String[] columnNames = resultCursor.getColumnNames();
                                    do {
                                        String key = "";
                                        for (String name: columnNames) {
                                            if(name.equals("_id"))
                                            {
                                                continue;
                                            }
                                            else if(name.equals(KEY_FIELD))
                                            {
                                                key = resultCursor.getString(resultCursor.getColumnIndex(name));
                                            }
                                            else
                                            {
                                                KeyValue kv = new KeyValue(key,resultCursor.getString(resultCursor.getColumnIndex(name)));
                                                tempReply.alist.add(kv);
                                            }
                                        }
                                    } while (resultCursor.moveToNext());
                                }
                                constructMessageToSend = gson.toJson(tempReply);
                                Log.e(TAG, "Message being RETURN_QUERY_ALL is " + constructMessageToSend);
                                outputControl = new DataOutputStream(listenSocket.getOutputStream());
                                outputControl.writeBytes(constructMessageToSend + "\n");
                                break;
                            case QUERY_ALL:
                                MsgInterchangeFormat finalReply = new MsgInterchangeFormat();
                                for (String port : socketArrayList) {
                                    MsgInterchangeFormat hMsg = new MsgInterchangeFormat(myPort, node_id);
                                    hMsg.mtype = MessageType.RETURN_QUERY_ALL;
                                    constructMessageToSend = gson.toJson(hMsg);
                                    Log.e(TAG, "RETURN_QUERY_ALL Message being sent is " + constructMessageToSend);
                                    Socket msgSocket = getSocket(Integer.parseInt(port));
                                    outputControl = new DataOutputStream(msgSocket.getOutputStream());
                                    outputControl.writeBytes(constructMessageToSend + "\n");
                                    BufferedReader inputFromServer = null;
                                    inputFromServer = new BufferedReader(new InputStreamReader(msgSocket.getInputStream()));
                                    String tempStr = inputFromServer.readLine();
                                    MsgInterchangeFormat tempMsg = gson.fromJson(tempStr, MsgInterchangeFormat.class);
                                    finalReply.alist.addAll(tempMsg.alist);
                                    //messageSentFromServer += inputFromServer.readLine();
                                }
                                //Add your own messages also

                                resultCursor = query(mUri, null,
                                        "#", null, null);
                                if (resultCursor.moveToFirst() ){
                                    String[] columnNames = resultCursor.getColumnNames();
                                    do {
                                        String key = "";
                                        for (String name: columnNames) {
                                            if(name.equals("_id"))
                                            {
                                                continue;
                                            }
                                            else if(name.equals(KEY_FIELD))
                                            {
                                                key = resultCursor.getString(resultCursor.getColumnIndex(name));
                                            }
                                            else
                                            {
                                                KeyValue kv = new KeyValue(key,resultCursor.getString(resultCursor.getColumnIndex(name)));
                                                finalReply.alist.add(kv);
                                            }
                                        }
                                    } while (resultCursor.moveToNext());
                                }
                                constructMessageToSend = gson.toJson(finalReply);
                                Log.e(TAG, "QUERY_ALL Message being sent is " + constructMessageToSend);
                                outputControl = new DataOutputStream(listenSocket.getOutputStream());
                                outputControl.writeBytes(constructMessageToSend + "\n");
                                break;
                        }
                        /*for (sockNPort currSocket : socketArrayList) {
                            HelloMessageReply hMsgReply = new HelloMessageReply();
                            Log.e(TAG, String.valueOf(currSocket.port));
                            int portNumber = (Integer.parseInt(currSocket.port)) / 2;
                            String hashedPortNumber = genHash(String.valueOf(portNumber));
                            hMsgReply.predecessor = getPredecessor(hashedPortNumber);
                            hMsgReply.successor = getSuccessor(hashedPortNumber);
                            String constructMessageToSend = gson.toJson(hMsgReply);
                            Log.e(TAG, "Message being sent is " + constructMessageToSend);
                            DataOutputStream outputControl = new DataOutputStream(currSocket.socket.getOutputStream());
                            outputControl.writeBytes(constructMessageToSend + "\n");
                        }*/
                }
            } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "No such Algorithm Exception");
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Socket Timeout Exception 4");
                    e.printStackTrace();
                } catch (StreamCorruptedException e) {
                    Log.e(TAG, "Stream Corrupted Exception");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "IO Exception 6");
                    e.printStackTrace();
                }
            catch (Exception e) {
                Log.e(TAG, "Server Exception"+ e.toString());
                e.printStackTrace();
            }
            return null;
        }

        /*
        Runs on the UI thread after publishProgress(Progress...) is invoked.
        The specified values are the values passed to publishProgress(Progress...).
        */
        protected void onProgressUpdate(MsgInterchangeFormat... strings) {
            Gson gson = new Gson();
            String constructMessageToSend = gson.toJson(strings[0]);
            String strReceived = "Successor " + strings[0].successor +
                    "Predecessor " + strings[0].predecessor;
            tv.append("\t\t\t\t" + strReceived + "\t\n");
            return;
        }
    }
}

package bill.bai.bluetoothtexts;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    //For debugging
    private static final String TAG = "BTConnectionService";
    //Config
    private static final String appName = "MYAPP";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("935b85f4-c3cb-44cd-96f0-cf5634ca9ea1");
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    // Accept Thread
    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;


    public BluetoothConnectionService(Context mContext) {
        this.mContext = mContext;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    /**
     * Listens for incoming connections.
     */
    private class AcceptThread extends Thread{

        private final BluetoothServerSocket mServerSocket;

        public AcceptThread(){
            BluetoothServerSocket temp = null;

            try {
                temp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up server using: " + MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.d(TAG, "AcceptThread: IOException" + e.getMessage());
                e.printStackTrace();
            }

            mServerSocket = temp;
        }

        @Override
        public void run() {
            Log.d(TAG, "Run: AcceptThread running...");

            BluetoothSocket socket = null;

            try {
                Log.d(TAG, "Run: RFCOMM server socket start...");
                socket = mServerSocket.accept();
                Log.d(TAG, "Run: RFCOMM server socket accepted connection.");
            } catch (IOException e) {
                Log.d(TAG, "Run: IOException" + e.getMessage());
                e.printStackTrace();
            }

            if(socket != null){
                connected(socket, mDevice);
            }

            Log.i(TAG, "End AcceptThread.");
        }

        public void cancel(){
            Log.d(TAG, "Cancel: Cancelling AcceptThread");
            try{
                mServerSocket.close();
            } catch(IOException e){
                Log.e(TAG, "Cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }

    /**
     * Runs while making outgoing connection w/ device
     */
    private class ConnectThread extends Thread{
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "ConnectThread: Started.");
            mDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket temp = null;
            Log.i(TAG, "RUN mConnectThread");

            // Get a BluetoothSocket for connection w/ bluetooth device
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using" +
                        "UUID: " + MY_UUID_INSECURE);
                temp = mDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.d(TAG, "ConnectThread: Couldn't create InsecureRfcommSocket" + e.getMessage());
                e.printStackTrace();
            }

            mSocket = temp;

            // Cancel discovery, since it is memory intensive.
            mBluetoothAdapter.cancelDiscovery();

            // This is a blocking call and will only return on a successful connection
            // or an exception
            try {
                mSocket.connect();
                Log.d(TAG, "RUN: ConnectThread connected.");
            } catch (IOException e) {
                try{
                    mSocket.close();
                    Log.d(TAG, "RUN: ConnectThread closed socket.");
                }catch(IOException e1){
                    Log.e(TAG, "ConnectThread: RUN: Unable to close connection in socket " + e1.getMessage());
                    e1.printStackTrace();
                }
                Log.d(TAG, "RUN: Couldn't connect to UUID: " + MY_UUID_INSECURE);
            }
            connected(mSocket, mDevice);
        }

        public void cancel(){
            try{
                Log.d(TAG, "CANCEL: Closing client socket...");
                mSocket.close();
            }catch(IOException e){
                Log.e(TAG, "CANCEL: close() of mSocket in ConnectThread Failed " + e.getMessage());
            }
        }
    }

    /**
     * Starts the chat service.
     * Starts the AcceptThread to begin a session in listening mode
     * Called by the Activity onResume()
     */
    public synchronized void start(){
        Log.d(TAG, "START");

        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started");

        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    /**
     * Responsible for maintaining bluetooth connection
     * sends and receives data through IO Streams.
     */
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "ConnectedThread: Starting...");

            mSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            // Dismisses progress dialog when connection established.
            mProgressDialog.dismiss();

            try {
                tempIn = mSocket.getInputStream();
                tempOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInputStream = tempIn;
            mOutputStream = tempOut;
        }

        public void run(){
            byte[] buffer = new byte[1024]; // Buffer for the streams
            int bytes;

            while(true){
                try {
                    bytes = mInputStream.read(buffer);
                    String incomingMsg = new String(buffer, 0, bytes);
                    Log.d(TAG, "Inputer Stream: " + incomingMsg);
                } catch (IOException e) {
                    Log.wtf(TAG, "ConnectedThread: Error reading inputStream " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputStream: " + text);
            try {
                mOutputStream.write(bytes);
            } catch (IOException e) {
                Log.wtf(TAG, "ConnectedThread: Error writing outputStream " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void cancel(){
            try{
                mSocket.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice mBluetoothDevice){
        Log.d(TAG, "connected: Starting...");

        // Start thread to manage connection and transmissions
        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    // Write to ConnectedThread in asynchronous manner
    public void write(byte[] out){
        Log.d(TAG, "write: Write called..");
        mConnectedThread.write(out);
    }
}
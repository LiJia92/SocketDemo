package study.lastwarmth.me.socketdemo;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 服务器端Socket帮助类
 * Created by Jaceli on 2016-11-04.
 */

public class ServerSocketHelper {

    private final static int LISTENING_PORT = 9999;
    private final static int SERVER_PORT = 8888;

    private static ServerSocketHelper mInstance;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private ServerSocket mServerSocket;
    private static List<Socket> socketList = new ArrayList<>();
    private boolean stop = false;
    private ReceiveMessageListener listener;

    public void setListener(ReceiveMessageListener listener) {
        this.listener = listener;
    }

    private ServerSocketHelper() {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("ServerSocketThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {

                }
            };
        }
    }

    public static ServerSocketHelper getInstance() {
        synchronized (ServerSocketHelper.class) {
            if (mInstance == null) {
                mInstance = new ServerSocketHelper();
            }
        }
        return mInstance;
    }

    public void startListening() {
        stop = false;
//        setupServer();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buf = new byte[1024];
                    DatagramSocket ds = new DatagramSocket(LISTENING_PORT);
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    String ip = getIP(MyApplication.getApplication());
                    Log.e("TAG", "startListening:" + ip);
                    ds.receive(dp);
                    ds.close();
                    StringBuffer sb = new StringBuffer();
                    int i;
                    for (i = 0; i < 1024; i++) {
                        if (buf[i] == 0) {
                            break;
                        }
                        sb.append((char) buf[i]);
                    }
                    Log.e("TAG", "startListening result:" + sb.toString());
                    sendIpBroadcast(sb.toString());
                    setupServer();
//                    if (listener != null) {
//                        listener.onReceiveMessage(sb.toString());
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendIpBroadcast(final String clientIp) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String host = clientIp;
                final String message = getIP(MyApplication.getApplication());
                try {
                    InetAddress adds = InetAddress.getByName(host);
                    DatagramSocket ds = new DatagramSocket();
                    DatagramPacket dp = new DatagramPacket(message.getBytes(),
                            message.length(), adds, LISTENING_PORT);
                    ds.send(dp);
                    Log.e("TAG", "sendIpBroadcast");
                    ds.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupServer() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mServerSocket = new ServerSocket(SERVER_PORT);
                    Log.e("TAG", "setupServer");
                    while (!stop) {
                        Socket socket = mServerSocket.accept();
                        socketList.add(socket);
                        new Thread(new MyServerRunnable(socket)).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stopServer() {
        stop = true;
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mHandlerThread.quit();
    }

    private static class MyServerRunnable implements Runnable {
        Socket socket = null;
        BufferedReader bufferedReader = null;

        MyServerRunnable(Socket socket) {
            this.socket = socket;
            Log.e("TAG", "client connected");
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            String content = null;
            try {
                while (!mInstance.stop && ((content = bufferedReader.readLine()) != null)) {
                    Log.e("TAG", "content:" + content);
                    for (Socket socket : socketList) {
                        try {
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            if (content.equals("getTime")) {
                                writer.write("time:" + String.valueOf(SystemClock.elapsedRealtime()));
                                writer.newLine();
                                writer.flush();
                            } else {
                                JSONObject root = new JSONObject(content);
                                long time = root.getLong("time");
                                byte[] data = root.getString("data").getBytes();
                                if (mInstance.listener != null) {
                                    mInstance.listener.onReceiveMessage(time, data);
                                }
                            }
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String readFromClient() {
            try {
                return bufferedReader.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static String getIP(Context application) {
        WifiManager wifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } else {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String ip = intToIp(ipAddress);
            return ip;
        }
        return null;
    }

    private static String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }
}

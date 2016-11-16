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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Jaceli on 2016-11-04.
 */

public class ClientSocketHelper {

    private final static int LISTENING_PORT = 9999;
    private final static int SERVER_PORT = 8888;
    private final static String BROADCAST_IP = "255.255.255.255";

    private static ClientSocketHelper mInstance;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private GetTimeListener listener;

    private DataOutputStream dataOutputStream;

    public void setListener(GetTimeListener listener) {
        this.listener = listener;
    }

    private ClientSocketHelper() {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("ClientSocketThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {

                }
            };
        }
    }

    public static ClientSocketHelper getInstance() {
        synchronized (ClientSocketHelper.class) {
            if (mInstance == null) {
                mInstance = new ClientSocketHelper();
            }
        }
        return mInstance;
    }

    public void sendIpBroadcast() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final String message = getIP(MyApplication.getApplication());
                try {
                    InetAddress adds = InetAddress.getByName(BROADCAST_IP);
                    DatagramSocket ds = new DatagramSocket();
                    DatagramPacket dp = new DatagramPacket(message.getBytes(),
                            message.length(), adds, LISTENING_PORT);
                    ds.send(dp);
                    Log.e("TAG", "sendIpBroadcast");
                    ds.close();
                    startListening();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startListening() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buf = new byte[1024];
                    DatagramSocket ds = new DatagramSocket(LISTENING_PORT);
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
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
                    connect(sb.toString());
                    Log.e("TAG", "startListening");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void connect(final String serverIp) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e("TAG", serverIp);
                    socket = new Socket(serverIp, SERVER_PORT);
                    Log.e("TAG", "client connected");
//                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//                    writer.write("Hello world");
//                    writer.newLine();
//                    writer.flush();
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String content;
                            try {
                                while ((content = reader.readLine()) != null) {
                                    Log.e("TAG", "receive TIME");
                                    if (content.startsWith("time:")) {
                                        long receiveTime = SystemClock.elapsedRealtime();
                                        String[] str = content.split(":");
                                        long serverTime = Long.valueOf(str[1]);
                                        if (listener != null) {
                                            listener.onGetTime(receiveTime, serverTime);
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void postMsg(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
//                    writer.write(msg);
//                    writer.newLine();
//                    writer.flush();

                    dataOutputStream.write(msg.getBytes("UTF-8"));
                    Log.e("TAG", "write done.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void postMsg(final long time, final byte[] data) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject root = new JSONObject();
                    root.put("time", time);
                    root.put("data", new String(data));
                    writer.write(root.toString());
                    writer.newLine();
                    writer.flush();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void postMsg(final byte[] data) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    dataOutputStream.write(data);
                    dataOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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

    interface GetTimeListener {
        void onGetTime(long receiveTime, long serverTime);
    }
}

package study.lastwarmth.me.socketdemo;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class ServerActivity extends AppCompatActivity implements View.OnClickListener, ReceiveMessageListener, SurfaceHolder.Callback {

    private Button listen;
    private Button stop;
    private TextView log;
    private SurfaceView surfaceView;
    private MediaCodec codec;
    private MediaCodec decoder;
    private final static int TIME_INTERNAL = 30;
    private List<byte[]> h264data = new LinkedList<>();
    String path = Environment.getExternalStorageDirectory() + "/test_1280x720.h264";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        listen = (Button) findViewById(R.id.start_listen);
        stop = (Button) findViewById(R.id.stop_server);
        log = (TextView) findViewById(R.id.server_text);
        surfaceView = (SurfaceView) findViewById(R.id.surface);

        listen.setOnClickListener(this);
        stop.setOnClickListener(this);

        log.setMovementMethod(ScrollingMovementMethod.getInstance());

        ServerSocketHelper.getInstance().setListener(this);

        surfaceView.getHolder().addCallback(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_listen:
                ServerSocketHelper.getInstance().startListening();
                break;
            case R.id.stop_server:
                ServerSocketHelper.getInstance().stopServer();
        }
    }

    @Override
    public void onReceiveMessage(final long time, byte[] data) {
        h264data.add(data);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                long receiveTime = SystemClock.elapsedRealtime();
//                long delay = 0;
//                try {
//                    delay = receiveTime - time;
//                } catch (NumberFormatException e) {
//
//                }
//                log.append("时间：" + time + "--->" + receiveTime + ":" + delay + "\n");
//            }
//        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initMediaDecode();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void initMediaDecode() {
        Log.e("TAG", "initMediaDecode");
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1280, 720);
        try {
            decoder = MediaCodec.createDecoderByType("video/avc");
            decoder.configure(format, surfaceView.getHolder().getSurface(), null, 0);
            decoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new DecodeThread().start();
    }


    private class DecodeThread extends Thread {

        MediaCodec.BufferInfo mBufferInfo;
        int mCount = 0;

        public DecodeThread() {
            Log.e("TAG", "DecodeThread");
            mBufferInfo = new MediaCodec.BufferInfo();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (h264data.size() > 0) {
                        byte[] data = h264data.get(0);
                        h264data.remove(0);
//                        Log.e("Media", "save to file data size:" + data.length);
//                        Util.save(data, 0, data.length, path, true);
                        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
                        int inputBufferIndex = decoder.dequeueInputBuffer(100);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(data);
                            decoder.queueInputBuffer(inputBufferIndex, 0, data.length, mCount * TIME_INTERNAL, 0);
                        }

                        // Get output buffer index
                        int outputBufferIndex = decoder.dequeueOutputBuffer(mBufferInfo, 100);
                        while (outputBufferIndex >= 0) {
                            Log.e("Media", "onFrame index:" + outputBufferIndex);
                            decoder.releaseOutputBuffer(outputBufferIndex, true);
                            outputBufferIndex = decoder.dequeueOutputBuffer(mBufferInfo, 0);
                        }
                    } else {
                        sleep(TIME_INTERNAL);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

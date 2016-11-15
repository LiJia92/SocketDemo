package study.lastwarmth.me.socketdemo;

/**
 * Created by Jaceli on 2016-11-04.
 */

public interface ReceiveMessageListener {

    void onReceiveMessage(long msg, byte[] data);

}

package be.hogent.fnt.elm.auto.tcpclient;

/**
 * Created by Jork on 20/10/2015.
 */
public interface TcpClientCallback {
    void onConnected();
    void onClosed();
    void onMessageRecieved(String message);
    void onError(Exception ex);
}

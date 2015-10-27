package be.hogent.fnt.elm.auto.tcpclient;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Jork on 20/10/2015.
 */
public class TcpClient {
    Socket socket;
    TcpClientCallback callback;

    TcpClient(TcpClientCallback callback) {
        this.callback = callback;
    }

    public void asyncConnect(final String host, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 5000);
                    callback.onConnected();
                }
                catch (IOException e){
                    callback.onError(e);
                    callback.onClosed();
                }
            }
        }).start();

    }

    /*public boolean isConnected()
    {
        if (socket != null) return socket.isConnected();
        else return false;
    }*/

    public void asyncClose() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null) if (socket.isConnected()) try{
                    socket.shutdownInput();
                    socket.close();
                }
                catch (IOException e){
                    callback.onError(e);
                }
                callback.onClosed();
            }
        }).start();
    }

    public void asynRecieve() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = "";
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    int bytesRead = -1;

                    bytesRead = socket.getInputStream().read(buffer, 0, bufferSize);
                    if (bytesRead > 0){
                        result = new String(buffer, 0, bytesRead);
                        callback.onMessageRecieved(result);
                    }
                    else callback.onClosed();
                } catch (Exception e){
                    callback.onError(e);
                }
            }
        }).start();
    }

    public void sendMessage(String message) {
        try{
            socket.getOutputStream().write(message.getBytes());
            socket.getOutputStream().flush();
        }
        catch (IOException e){
            callback.onError(e);
        }
    }
}

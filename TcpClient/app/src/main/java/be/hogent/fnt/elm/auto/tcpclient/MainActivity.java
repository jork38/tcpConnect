package be.hogent.fnt.elm.auto.tcpclient;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    EditText txtHost;
    EditText txtSend;

    enum ConnectButton {CONNECT, DISCONNECT}

    Button btnConnect;
    Button btnSend;
    Button btnInfo;
    ScrollView svLog;
    TextView txtLog;

    TcpClient tcpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtHost = (EditText) findViewById(R.id.txtHost);
        txtSend = (EditText) findViewById(R.id.txtSend);

        svLog = (ScrollView) findViewById(R.id.svLog);
        txtLog = (TextView) findViewById(R.id.txtLog);

        btnInfo = (Button) findViewById(R.id.btnInfo);
        btnInfo.setOnClickListener(new InfoButtonClickListener());

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setTag(ConnectButton.CONNECT);
        btnConnect.setOnClickListener(new ConnectButtonClickListener());

        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new SendButtonClickListener());
    }


    class InfoButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            try {
                List<NetworkInterface> interfaces;
                interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface networkInterface : interfaces) {
                    List<InetAddress> addresses;
                    addresses = Collections.list(networkInterface.getInetAddresses());
                    if (addresses.size() > 0) {
                        String name = networkInterface.getDisplayName();
                        logToScreen("Network interface found: %s", name);

                        if (networkInterface.getHardwareAddress() != null) {
                            StringBuilder sb = new StringBuilder(18);
                            for (byte b : networkInterface.getHardwareAddress()) {
                                if (sb.length() > 0) sb.append(':');
                                sb.append(String.format("%02x", b));
                            }
                            logToScreen("\tMAC Address: %s", sb.toString());
                        }

                        for (InetAddress address : addresses) {
                            logToScreen("\tAddress: %s", address.getHostAddress());
                        }
                        logToScreen("\n");
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    class ConnectButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch ((ConnectButton) btnConnect.getTag()) {
                case CONNECT:
                    try {
                        String host = txtHost.getText().toString().split(":")[0];
                        String strPort = txtHost.getText().toString().split(":")[1];
                        Integer port = Integer.parseInt(strPort);

                        logToScreen("start Connecting...");
                        btnConnect.setEnabled(false);
                        txtHost.setEnabled(false);

                        tcpClient = new TcpClient(new MainActivityCallback());
                        tcpClient.asyncConnect(host, port);
                    } catch (Exception ex) {
                        logToScreen("Error connecting: %s", ex.toString());
                        btnConnect.setEnabled(true);
                        txtHost.setEnabled(true);
                    }
                    break;
                case DISCONNECT:
                    logToScreen("start disconnecting...");
                    tcpClient.asyncClose();
                    break;
            }
        }
    }

    class SendButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            String message = txtSend.getText().toString();
            if (!message.endsWith("\n")) message += "\n";

            tcpClient.sendMessage(message);
            txtSend.setText("");
        }
    }

    class MainActivityCallback implements TcpClientCallback {

        @Override
        public void onConnected() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    btnConnect.setText("Disconnect");
                    btnConnect.setTag(ConnectButton.DISCONNECT);
                    btnConnect.setEnabled(true);

                    logToScreen("Connected");
                    tcpClient.asynRecieve();

                    btnSend.setEnabled(true);
                    txtSend.setEnabled(true);
                }
            });
        }

        @Override
        public void onClosed() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    btnConnect.setText("Connected");
                    btnConnect.setTag(ConnectButton.CONNECT);
                    btnConnect.setEnabled(true);

                    txtHost.setEnabled(true);

                    btnSend.setEnabled(false);
                    txtSend.setEnabled(false);

                    logToScreen("disconnected");
                }
            });

        }

        @Override
        public void onMessageRecieved(final String message) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    logToScreen(message);
                }
            });

            tcpClient.asynRecieve();

        }

        @Override
        public void onError(final Exception ex) {
            new Handler(Looper.getMainLooper()).post(new Runnable()
            {
                @Override
                public void run() {
                    logToScreen("Error: %s", ex.toString());
                    tcpClient.asyncClose();
                }
            });
        }
    }

    void logToScreen(String format, Object... args) {
        String message = String.format(format, args);
        if (!message.endsWith("\n")) message += "\n";

        txtLog.append(message);
        svLog.post(new Runnable() {
            @Override
            public void run() {
                svLog.fullScroll(View.FOCUS_DOWN);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        try {
            tcpClient.asyncClose();
        } catch (Exception ex){

        }
        super.onDestroy();
    }
}

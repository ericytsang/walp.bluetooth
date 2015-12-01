package gmail.surpluset.walpbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    private static final UUID PROTOCOL_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private TextView loaclMacAddressTextView;
    private EditText remoteMacAddressEditText;
    private EditText messageEditText;
    private BluetoothSocket socket;
    private final Object notifiedWhenSocketIsAssigned = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get view references
        loaclMacAddressTextView = (TextView) findViewById(R.id.mainactivity_macaddressvalue_textview);
        remoteMacAddressEditText = (EditText) findViewById(R.id.mainactivity_macaddressvalue_edittext);
        messageEditText = (EditText) findViewById(R.id.mainactivity_message_edittext);

        // start threads
        new ReceiveThread().start();
        new AcceptThread().start();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // configure views
        loaclMacAddressTextView.setText(BluetoothAdapter.getDefaultAdapter().getAddress());
    }

    public void connect(View view)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                synchronized(notifiedWhenSocketIsAssigned)
                {
                    try
                    {
                        String serverAddress = remoteMacAddressEditText.getText().toString();
                        BluetoothDevice server = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(serverAddress);
                        socket = server.createInsecureRfcommSocketToServiceRecord(PROTOCOL_UUID);
                        socket.connect();
                        notifiedWhenSocketIsAssigned.notify();
                        MainActivity.this.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(MainActivity.this,"connected",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    catch(final Exception e)
                    {
                        MainActivity.this.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(MainActivity.this,"failed to connected: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void send(View view) throws IOException
    {
        if(socket != null)
        {
            String message = messageEditText.getText().toString();
            new DataOutputStream(socket.getOutputStream()).writeUTF(message);
        }
        else
        {
            Toast.makeText(MainActivity.this,"not connected; can't send",Toast.LENGTH_SHORT).show();
        }
    }

    private class AcceptThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                BluetoothServerSocket btServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord("meh",PROTOCOL_UUID);
                while(socket == null)
                {
                    BluetoothSocket socket = btServerSocket.accept();
                    synchronized(notifiedWhenSocketIsAssigned)
                    {
                        if(MainActivity.this.socket == null)
                        {
                            MainActivity.this.socket = socket;
                            notifiedWhenSocketIsAssigned.notify();
                        }
                    }
                }
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private class ReceiveThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                synchronized(notifiedWhenSocketIsAssigned)
                {
                    while(socket == null)
                    {
                        notifiedWhenSocketIsAssigned.wait();
                    }
                }
            }
            catch(InterruptedException e)
            {
                throw new RuntimeException(e);
            }

            while(socket != null)
            {
                try
                {
                    final String message = new DataInputStream(socket.getInputStream()).readUTF();
                    MainActivity.this.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch(IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

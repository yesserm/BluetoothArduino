package com.example.bluetootharduino;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private ListView lista;
    private BluetoothAdapter bluetoothAdapter;
    private List<String> nombresEmparejados;
    private final List<String> nombresEncontrados = new ArrayList<>();
    private TextView tvResultado;
    private TextView tvDispositivos;
    AlertDialog dialog;
    Boolean encontrado = true;

    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private static final String NAME_SECURE = "BluetoothArduinoSecure";
    private static final String NAME_INSECURE = "BluetoothArduinoInsecure";
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private int mState;
    public static final int STATE_NONE = 0;
    private int mNewState;
    private ConnectedThread mConnectedThread;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private static final String TAG = "BTAdminConnection";

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public MainActivity(BluetoothAdapter mAdapter, Handler mHandler) {
        mState = STATE_NONE;
        mNewState = mState;
        this.mAdapter = mAdapter;
        this.mHandler = mHandler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        tvResultado = findViewById(R.id.tvResultado);
        if (bluetoothAdapter != null)
        {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
            Log.e("DESCUBRIENDO ", " " + bluetoothAdapter.startDiscovery());
            tvDispositivos = findViewById(R.id.tvDispositivos);
            String dato = getString(R.string.dispositivosTexto)+" Encontrados";
            tvDispositivos.setText(dato);

        }


        // broadcast
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        Button botonBluetooth = findViewById(R.id.btProbarBluetooth);
        botonBluetooth.setOnClickListener(this::pruebaBluetooth);

        Button btEncontrar = findViewById(R.id.btEncontrar);
        btEncontrar.setOnClickListener(this::escanearBluetooth);


        // Lista para dispositivos emparejados
        lista = findViewById(R.id.listaElementos);
        nombresEmparejados = new ArrayList<>();
    }

    public Boolean validarbluetooth()
    {
        if (bluetoothAdapter == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.dialog_message)
                    .setTitle(R.string.dialog_title);

            dialog = builder.create();
            dialog.show();
            encontrado = false;
            return encontrado;
        }
        Log.e("ERROR", "res " + true);
        encontrado = true;
        return encontrado;
    }

    protected void pruebaBluetooth(View view)
    {
        if (!validarbluetooth()) {
            return;
        }
        String resultado = getString(R.string.resultado);
        nombresEncontrados.clear();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
                Toast.makeText(this, R.string.mensaje_bluetooth, Toast.LENGTH_SHORT).show();
                resultado += "\nBluetooth no soportado";
                tvResultado.setText(resultado);
        } else {
            Toast.makeText(this, "Bluetooth soportado", Toast.LENGTH_SHORT).show();
            resultado += "\nBluetooth soportado\n";
            tvResultado.setText(resultado);

            if (!bluetoothAdapter.isDiscovering())
            {
                int requestCode = 1;
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(discoverableIntent, requestCode);

                tvDispositivos = findViewById(R.id.tvDispositivos);
                String dato = getString(R.string.dispositivosTexto)+" Encontrados";
                tvDispositivos.setText(dato);
            }
            Log.e("DESCUBRIENDO ", " " + bluetoothAdapter.startDiscovery());
        }
    }

    protected void escanearBluetooth(View view)
    {
        if (!validarbluetooth()) { return; }
        // encontrar dispositivos ya emparejados
        Set<BluetoothDevice> dispositivosEmparejados = bluetoothAdapter.getBondedDevices();
        if (dispositivosEmparejados.size() > 0) {
            for (BluetoothDevice dispositivo : dispositivosEmparejados) {
                String nombreDispositivo = dispositivo.getName();
                String direccionHardware = dispositivo.getAddress();
                nombresEmparejados.add(nombreDispositivo + " => " + direccionHardware);
            }
            tvDispositivos = findViewById(R.id.tvDispositivos);
            String dato = getString(R.string.dispositivosTexto) + " Emparejados";
            tvDispositivos.setText(dato);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombresEmparejados);
            lista.setAdapter(adapter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String nombreDispositivo = dispositivo.getName();
                String direccionHardware = dispositivo.getAddress();
                nombresEncontrados.add(nombreDispositivo + "\n"+direccionHardware);
                Log.e("ENCONTRADO", nombreDispositivo + " => " + direccionHardware);
                lista.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, nombresEncontrados));
            }
        }
    };

    /**
     * Inicie ConnectedThread para comenzar a administrar una conexión Bluetooth
     *
     * @param socket El BluetoothSocket en el que se realizó la conexión
     * @param device El dispositivo Bluetooth que se ha conectado
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancelar el hilo que completó la conexión
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancelar cualquier hilo que esté ejecutando actualmente una conexión
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancelar el hilo de aceptación porque solo queremos conectarnos a un dispositivo
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Inicie el hilo para administrar la conexión y realizar transmisiones
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Envíe el nombre del dispositivo conectado a la Actividad de la interfaz de usuario
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Actualizar el título de la interfaz de usuario
        updateUserInterfaceTitle();
    }

    /**
     * Este hilo se ejecuta mientras escucha las conexiones entrantes. Se comporta como un cliente
     * del lado del servidor. Funciona hasta que se acepta una conexión (o hasta que se cancela).
     */
    private class AcceptThread extends Thread {
        // El socket del servidor local
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Seguro" : "Inseguro";

            // Crear un nuevo socket de servidor de escucha
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Escuche el servidor del socket si no estamos conectados
            while (mState != STATE_CONNECTED) {
                try {
                    // Esta es una llamada de bloqueo y solo regresará en una conexión exitosa
                    // o una excepción
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // Si se aceptó una conexión
                if (socket != null) {
                    synchronized (MainActivity.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situación normal. Inicie el hilo conectado.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // No está listo o ya está conectado. Terminar nuevo socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "No se pudo cerrar el socket no deseado", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Tipo de Socket" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Tipo de Socket" + mSocketType + "close() of server failed", e);
            }
        }
    }

    /**
     * Inicie el servicio de chat. Específicamente, inicie AcceptThread para comenzar
     * una sesión en modo de escucha (servidor). Llamado por la actividad onResume()
     */

    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancelar cualquier hilo que intente establecer una conexión
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancelar cualquier hilo que esté ejecutando actualmente una conexión
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Inicie el hilo para escuchar en un BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
        // Actualizar el título de la interfaz de usuario
        updateUserInterfaceTitle();
    }

    /**
     * Devuelve el estado de conexión actual.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Actualice el título de la interfaz de usuario de acuerdo con el estado actual de la conexión de chat
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Dar el nuevo estado al controlador para que la actividad de la interfaz de usuario pueda actualizarse
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Escribir en ConnectedThread de manera no sincronizada
     *
     * @param out Los bytes para escribir
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Crear objeto temporal
        ConnectedThread r;
        // Sincronizar una copia de ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Realice la escritura sin sincronizar
        r.write(out);
    }

    /**
     * Indique que el intento de conexión falló y notifique la actividad de la interfaz de usuario.
     */
    private void connectionFailed() {
        // Enviar un mensaje de error a la actividad.
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "No se puede conectar el dispositivo");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Actualizar el título de la interfaz de usuario
        updateUserInterfaceTitle();

        // Vuelva a iniciar el servicio para reiniciar el modo de escucha
        MainActivity.this.start();
    }

    /**
     * Indique que se perdió la conexión y notifique la actividad de la interfaz de usuario.
     */

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        MainActivity.this.start();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (MainActivity.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * Este hilo se ejecuta durante una conexión con un dispositivo remoto.
     * Maneja todas las transmisiones entrantes y salientes.
     */

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread (BluetoothSocket socket, String socketType) {
            Log.d(TAG,"Creacion la conexion al hilo: " + socketType);

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Obtenga las transmisiones de entrada y salida de BluetoothSocket
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG,"No se creo el socket", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG,"INICIADO mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Siga escuchando InputStream mientras está conectado
            while (mState == STATE_CONNECTED) {
                try {
                    // Leer del InputStream
                    bytes = mmInStream.read(buffer);

                    // Envíe los bytes obtenidos a la actividad de la interfaz de usuario
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Desconectado", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Escriba en el OutStream conectado.
         *
         * @param buffer Los bytes para escribir
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Comparte el mensaje enviado con la actividad de la interfaz de usuario
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Excepción durante la escritura", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
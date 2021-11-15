package com.example.bluetootharduino;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

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
}

package com.example.bluetootharduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private ListView lista;
    private BluetoothAdapter bluetoothAdapter;
    private List<String> nombresEmparejados;
    private TextView tvResultado;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        tvResultado = findViewById(R.id.tvResultado);

        Button botonBluetooth = findViewById(R.id.btProbarBluetooth);
        botonBluetooth.setOnClickListener(this::pruebaBluetooth);

        Button btEncontrar = findViewById(R.id.btEncontrar);
        btEncontrar.setOnClickListener(view -> escanearBluetooth(view));

        lista = findViewById(R.id.listaElementos);
        nombresEmparejados = new ArrayList<>();
    }

    protected void pruebaBluetooth(View view)
    {
        String resultado = getString(R.string.resultado);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.mensaje_bluetooth, Toast.LENGTH_SHORT).show();
            System.out.println("Bluetooth no soportado");
            resultado += "\nBluetooth no soportado";
            tvResultado.setText(resultado);
            finish();
        } else {
            Toast.makeText(this, "Bluetooth soportado", Toast.LENGTH_SHORT).show();
            System.out.println("Bluetooth soportado");
            resultado += "\nBluetooth soportado\n";
            tvResultado.setText(resultado);
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    protected void escanearBluetooth(View view)
    {
        // encontrar dispositivos ya emparejados
        Set<BluetoothDevice> dispositivosEmparejados = bluetoothAdapter.getBondedDevices();
        if (dispositivosEmparejados.size() >  0)
        {
            for( BluetoothDevice dispositivo: dispositivosEmparejados)
            {
                String nombreDispositivo = dispositivo.getName();
                String direccionHardware = dispositivo.getAddress();
                nombresEmparejados.add(nombreDispositivo + " => " + direccionHardware);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombresEmparejados);
            lista.setAdapter(adapter);
        }
    }
}
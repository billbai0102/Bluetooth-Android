package bill.bai.bluetoothtexts;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOnOff = (Button) findViewById(R.id.btnOnOff);

        //Get default bt adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableDisableBluetooth();
            }
        });
    }

    //On click listener for btnOnOff
    public void enableDisableBluetooth(){
        if(mBluetoothAdapter == null){
            Log.d(TAG, "enableDisableBluetooth: Device doesn't have BT Capabilities.");
        }
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBluetoothIntent);

            IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, bluetoothIntent);
        }
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();

            IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, bluetoothIntent);
        }
    }
}

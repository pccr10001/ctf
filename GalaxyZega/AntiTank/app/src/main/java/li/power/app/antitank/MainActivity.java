package li.power.app.antitank;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.SyncStateContract;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private TextView txtLog;
    private LogReceiver mLogReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogReceiver = new LogReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_ADD_LOG);
        registerReceiver(mLogReceiver, intentFilter);

        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.bth_stop);
        txtLog = findViewById(R.id.txt_log);
        txtLog.setMovementMethod((new ScrollingMovementMethod()));

        btnStart.setOnClickListener((l) -> {
            {
                mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                        .getAdapter();

                if (mBluetoothAdapter != null) {

                    if (mBluetoothAdapter.isEnabled()) {

                        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                            Intent service = new Intent(getApplicationContext(), TankService.class);
                            startService(service);
                            showToast("Service started.");

                        } else {
                            showToast("No BLE adv supported.");
                            finish();
                        }
                    } else {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, 1);
                        showToast("Request Permission.");

                    }
                } else {

                    showToast("No BLE supported.");
                }
            }
        });
        btnStop.setOnClickListener((l)->{
            Intent intent=new Intent();
            intent.setAction(Constants.ACTION_STOP_ADV);
            sendBroadcast(intent);
            showToast("Stopping adv ...");
        });
    }

    public void showToast(String msg) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mLogReceiver);
        super.onDestroy();
    }

    public class LogReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(Objects.equals(intent.getAction(), Constants.ACTION_ADD_LOG)){
                txtLog.append(intent.getStringExtra(Constants.TAG_ADD_LOG)+"\n");
            }

        }
    }


}

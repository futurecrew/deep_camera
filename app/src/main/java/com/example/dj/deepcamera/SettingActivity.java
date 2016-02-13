package com.example.dj.deepcamera;

import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SettingActivity extends AppCompatActivity {

    private Button saveButton, cancelButton;
    private FeedReaderDbHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_setting);
        setContentView(R.layout.content_setting);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String serverIp = (String)intent.getExtras().get("server_ip");
        String serverPort = (String)intent.getExtras().get("server_port");
        String image_size = (String)intent.getExtras().get("image_size");
        String mode = (String)intent.getExtras().get("mode");
        String carType = (String)intent.getExtras().get("car_type");

        Log.d("Main", "serverIp : " + serverIp);
        Log.d("Main", "serverPort : " + serverPort);
        Log.d("Main", "image_size : " + image_size);
        Log.d("Main", "mode : " + mode);
        Log.d("Main", "carType : " + carType);

        ((EditText)findViewById(R.id.server_ip)).setText(serverIp);
        ((EditText)findViewById(R.id.server_port)).setText(serverPort);
        ((EditText)findViewById(R.id.image_size)).setText(image_size);

        if (mode.equals("1"))
            ((RadioButton)findViewById(R.id.mode1)).setChecked(true);
        else if (mode.equals("2"))
            ((RadioButton)findViewById(R.id.mode2)).setChecked(true);
        else if (mode.equals("3"))
            ((RadioButton)findViewById(R.id.mode3)).setChecked(true);

        if (carType.equals("1"))
            ((RadioButton)findViewById(R.id.car1)).setChecked(true);
        else if (carType.equals("2"))
            ((RadioButton)findViewById(R.id.car2)).setChecked(true);
        else if (carType.equals("3"))
            ((RadioButton)findViewById(R.id.car3)).setChecked(true);

        saveButton = (Button) findViewById(R.id.button_save);
        saveButton.setOnClickListener(saveButtonListener);

        cancelButton = (Button) findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(cancelButtonListener);

    }


    View.OnClickListener saveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = getIntent();
            intent.putExtra("server_ip", ((EditText) findViewById(R.id.server_ip)).getText().toString());
            intent.putExtra("server_port", ((EditText) findViewById(R.id.server_port)).getText().toString());
            intent.putExtra("image_size", ((EditText) findViewById(R.id.image_size)).getText().toString());

            String mode = "1";
            if (((RadioButton)findViewById(R.id.mode1)).isChecked())
                mode = "1";
            else if (((RadioButton)findViewById(R.id.mode2)).isChecked())
                mode = "2";
            else if (((RadioButton)findViewById(R.id.mode3)).isChecked())
                mode = "3";

            String carType = "1";
            if (((RadioButton)findViewById(R.id.car1)).isChecked())
                carType = "1";
            else if (((RadioButton)findViewById(R.id.car2)).isChecked())
                carType = "2";
            else if (((RadioButton)findViewById(R.id.car3)).isChecked())
                carType = "3";

            intent.putExtra("mode", mode);
            intent.putExtra("car_type", carType);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    View.OnClickListener cancelButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = getIntent();
            setResult(RESULT_CANCELED,intent);
            finish();
        }
    };
}

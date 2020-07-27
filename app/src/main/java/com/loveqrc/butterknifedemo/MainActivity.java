package com.loveqrc.butterknifedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.loveqrc.annotaionlib.BindView;
import com.loveqrc.butterknife_api.ButterKnife;

public class MainActivity extends AppCompatActivity {


    @BindView(R.id.btn)
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        btn.setText("Rc在努力");
    }
}


package study.lastwarmth.me.socketdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button server;
    private Button client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        server = (Button) findViewById(R.id.server);
        client = (Button) findViewById(R.id.client);

        server.setOnClickListener(this);
        client.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.server:
                intent = new Intent(this, ServerActivity.class);
                startActivity(intent);
                break;
            case R.id.client:
                intent = new Intent(this, ClientActivity.class);
                startActivity(intent);
                break;
        }
    }
}

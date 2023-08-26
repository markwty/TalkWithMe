package com.example.talkwithme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Connector.Connector;

public class VidyoCallActivity extends AppCompatActivity implements Connector.IConnect {
    private int room_id;
    private String opp_name = "", token = "";
    private Connector vc = null;
    private FrameLayout VideoFrameLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vidyo_call_activity);

        Intent intent = getIntent();
        room_id = intent.getIntExtra("id", 0);
        opp_name = intent.getStringExtra("opp_name");
        token = intent.getStringExtra("token");
        if(room_id == 0 || token == null){
            finish();
        }

        ConnectorPkg.setApplicationUIContext(this);
        ConnectorPkg.initialize();
        VideoFrameLayout = findViewById(R.id.VideoFrameLayout);

        final TextView DisplayNameText = findViewById(R.id.DisplayNameText);
        DisplayNameText.setText(opp_name);
        final ImageView AnswerButton = findViewById(R.id.AnswerButton);
        AnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(vc == null){
                    vc = new Connector(VideoFrameLayout, Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, 2, "warning info@VidyoClient info@VidyoConnector", "", 0);
                    vc.showViewAt(VideoFrameLayout, 0, 0, VideoFrameLayout.getWidth(), VideoFrameLayout.getHeight());
                    vc.connect("prod.vidyo.io", token, opp_name, Integer.toString(room_id), VidyoCallActivity.this);
                }
            }
        });
        final ImageView HangupButton = findViewById(R.id.HangupButton);
        HangupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(vc != null){
                    vc.disconnect();
                }
                finish();
            }
        });

        VideoFrameLayout.setOnClickListener(new View.OnClickListener() {
            //May choose to use animation
            @Override
            public void onClick(View v) {
                if(HangupButton.getVisibility() == View.VISIBLE){
                    HangupButton.setVisibility(View.INVISIBLE);
                    DisplayNameText.setVisibility(View.INVISIBLE);
                }else{
                    HangupButton.setVisibility(View.VISIBLE);
                    DisplayNameText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void onSuccess() {}

    public void onFailure(Connector.ConnectorFailReason reason) {}

    public void onDisconnected(Connector.ConnectorDisconnectReason reason) {}
}
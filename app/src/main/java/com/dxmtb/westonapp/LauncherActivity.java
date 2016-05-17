package com.dxmtb.westonapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LauncherActivity extends AppCompatActivity implements View.OnClickListener  {

    private LinearLayout layout;
    private TextView commandLabel, widthLabel, heightLabel, sizeLabel;
    private EditText commandText, widthText, heightText;
    private Button launchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        commandLabel = new TextView(this);
        commandLabel.setText("Start up tmpdir");
        layout.addView(commandLabel);

        commandText = new EditText(this);
        commandText.setText("/arch/tmp/weston-app-0");
        layout.addView(commandText);

        widthLabel = new TextView(this);
        widthLabel.setText("Width");
        layout.addView(widthLabel);

        widthText = new EditText(this);
        widthText.setText("800");
        layout.addView(widthText);

        heightLabel = new TextView(this);
        heightLabel.setText("Height");
        layout.addView(heightLabel);

        heightText = new EditText(this);
        heightText.setText("600");
        layout.addView(heightText);

        launchButton = new Button(this);
        launchButton.setText("Launch");
        launchButton.setOnClickListener(this);
        layout.addView(launchButton);

        sizeLabel = new TextView(this);
        sizeLabel.setText("Size:");
        layout.addView(sizeLabel);


        ViewTreeObserver viewTreeObserver = layout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                boolean once = false;
                @Override
                public void onGlobalLayout() {
                    if (once)
                        return;
                    once = true;
                    sizeLabel.setText("Width: " + layout.getWidth() + " Height: " + layout.getHeight());
                    widthText.setText("" + layout.getWidth());
                    heightText.setText("" + layout.getHeight());
                }
            });
        }


        setContentView(layout);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.TMP_DIR_MESSAGE, commandText.getText().toString());
        intent.putExtra(MainActivity.WIDTH_MESSAGE, widthText.getText().toString());
        intent.putExtra(MainActivity.HEIGHT_MESSAGE, heightText.getText().toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }
}

package de.hg_epp.whereisdon;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Outro
 *
 * (c) 2015 Jan Zartmann
 */
public class DonWin extends ActionBarActivity{

    private ImageView don;
    private TextView winMes;
    private TextView winMes2;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.don_win);
        don = (ImageView) findViewById(R.id.donWin);
        winMes = (TextView) findViewById(R.id.winMessage);
        winMes2 = (TextView) findViewById(R.id.outro);
        setPic();
        setText();
        startDonAnimation();
    }

    public void setPic() {
        don.setImageResource(R.drawable.don);

    }

    public void setText(){
        winMes.setText("Gl�ckwunsch, du hast das Spiel erfolgreich beendet!");
        winMes2.setText(setRaw());
    }

    //method reads the Raw
    private String setRaw() {
        InputStream inputStream = getResources().openRawResource(R.raw.outro);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int j;
        try {
            j = inputStream.read();
            while (j != -1) {
                byteArrayOutputStream.write(j);
                j = inputStream.read();
            }

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }


    private void startDonAnimation() {
        Animation upDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.up_down_don);
        upDown.setRepeatCount(Animation.INFINITE);
        upDown.setRepeatMode(Animation.REVERSE);
        don.setVisibility(View.VISIBLE);
        don.startAnimation(upDown);
    }
}
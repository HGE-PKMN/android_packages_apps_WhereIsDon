package de.hg_epp.whereisdon;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    public static String mChar;
    public static final String PREFS_NAME = "WIDPrefs";
    public static Activity mActivity;

    private MediaPlayer mMusicSwitch;

    /**
     * MainMenu for our Game. It manages the main stuff
     * (c) 2015 Jan Zartmann
     * (c) 2015 Christian Oder
     * <p/>
     * https://developer.android.com/training/system-ui/immersive.html
     * Implementation of the Google Non-Sticky Immersive Mode
     */

    // make our App Fullscreen, no Matter if Window is focused or not
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mMusicSwitch = MediaPlayer.create(this, R.raw.menu_switch_sound);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // make our App Fullscreen
        mActivity = this;
    }

    //method gets invoked when someone clicks a button with the onClick setting
    public void buttonOnClick(View z) {
        mMusicSwitch.start();
        switch (z.getId()) {
            case R.id.continue_button:
                //check if the App was running before if not call the Intro
                SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                if(settings.getBoolean("intro_run", true)){
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("intro_run", false);
                    editor.apply();
                    this.startActivity(new Intent(this, Introduction.class));
                }else {
                    // else just start the Game
                    loadDefaultMap();
                }
                break;
            case R.id.restart_button:
                reallyResetGame();
                break;
            case R.id.gender_changer_radio_boy:
                //set Trainer to male
                Toast.makeText(this, getString(R.string.boy_selected), Toast.LENGTH_SHORT).show();
                mChar = "gfx/trainer_male.png";
                storeButtonState();
                break;
            case R.id.gender_changer_radio_girl:
                Toast.makeText(this, getString(R.string.girl_selected), Toast.LENGTH_SHORT).show();
                //set Trainer to female
                mChar = "gfx/trainer_female.png";
                storeButtonState();
                break;

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //reload the starte from the Radio Buttons
        SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean radio_boy = settings.getBoolean("radio_boy", true);
        boolean radio_girl = settings.getBoolean("radio_girl", false);
        ((RadioButton) findViewById(R.id.gender_changer_radio_boy)).setChecked(radio_boy);
        ((RadioButton) findViewById(R.id.gender_changer_radio_girl)).setChecked(radio_girl);
        mChar = settings.getString("mChar_dir", "gfx/trainer_male.png");
    }

    // save the state of the radio buttons
    private void storeButtonState() {
        SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("radio_boy", ((RadioButton) findViewById(R.id.gender_changer_radio_boy)).isChecked());
        editor.putBoolean("radio_girl", ((RadioButton) findViewById(R.id.gender_changer_radio_girl)).isChecked());
        editor.putString("mChar_dir", mChar);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //reset all the values to default
    private void resetGame(){
        SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("intro_run", true);
        editor.putString("mChar_dir", "gfx/trainer_male.png");
        editor.putBoolean("t1", false);
        editor.putBoolean("t2", false);
        editor.putBoolean("t3", false);
        editor.putBoolean("t4", false);
        editor.putBoolean("t5", false);
        editor.putBoolean("t6", false);
        editor.putInt("won_games", 1);
        editor.putInt("maxMapID", 0);
        editor.putBoolean("moser_approved", false);
        editor.apply();
        Toast.makeText(this, getString(R.string.game_has_been_reset), Toast.LENGTH_LONG).show();
    }

    //create Dialog to ask if you really want to reset the game
    public void reallyResetGame() {
        DialogFragment DialogFragment = new resetGameDialog();
        DialogFragment.show(getFragmentManager(), "reset");
    }

    // Dialog asking if you want to reset the game
    public static class resetGameDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.resetGame)
                    .setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ((MainActivity) getActivity()).resetGame();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    // load the default map (the ground floor)
    public void loadDefaultMap(){
        Intent startAct = new Intent(this, TMXTiledMapDigital.class);
        ResourceManager.setMapID(0);
        this.startActivity(startAct);
    }
}

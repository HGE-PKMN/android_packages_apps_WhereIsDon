package de.hg_epp.whereisdon;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class FightEngine extends ActionBarActivity implements Animation.AnimationListener {
    /**
     * Engine Idea:
     * (c) 2015 Christian Schechter
     * <p/>
     * with slight modifications by:
     * (c) 2015 Christian Oder
     * <p/>
     * Code:
     * (c) 2015 Christian Oder
     * (c) 2015 Jan Zartmann
     * <p/>
     * https://developer.android.com/training/system-ui/immersive.html
     * Implementation of the Google Non-Sticky Immersive Mode
     */

    public static final String PREFS_NAME = "WIDPrefs";

    private String[] attacks;
    private String[] sayings;
    private String[] wbtsayings;
    private ArrayList<Integer> mDrawableArray = new ArrayList<>();
    private Button attack_button;
    private TextView sayingsTV;
    private boolean mTrainerFight;
    private boolean mButtonLocked;
    private int remainingFights;
    private int winsPlayer;
    private int winsTeacher;
    private int teacherWBT;
    private boolean inHitAnimation;
    private ImageView wbt_p;
    private ImageView wbt_t;
    private double player_lvl;
    private double teacher_lvl;
    private int teacher_won_fights;
    private int mTeacherID;
    private int mMapID;
    private String teacher_name;
    private String teacher_token;
    private int wbt_type_p;
    private int wbt_type_t;
    private double hp_p;
    private double hp_t;
    private View fake_view;
    private boolean wasPaused = false;

    MediaPlayer mMusic;

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

    // Initialize the ImageViews, TextViews and Buttons, create the fake_view and
    // unlock the attack button
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fight_layout);
        wbt_p = (ImageView) findViewById(R.id.webertron_p);
        wbt_t = (ImageView) findViewById(R.id.webertron_t);

        // load animation upDown
        startWBTAnimation();

        attack_button = (Button) findViewById(R.id.attack_button);
        sayingsTV = (TextView) findViewById(R.id.textfield);
        // create some fake view to pass an argument to the escape method
        fake_view = findViewById(R.id.action_bar);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("WIDCheatCodeInput".equals(type)) {
                cheatSetN(intent); // Handle text being sent
            } else if ("WIDCheatCodeUpdate".equals(type)) {
                cheatAnswerN(); // Handle text being sent
            } else if ("CreateFight".equals(type)) {
                unlockButton();
                prepareFight(intent);
            }
        } else {
            Log.e("WID_FE", "No Intent detected!");
        }
    }

    // override the action when pressing the back button to block escaping from a trainer fight.
    @Override
    public void onBackPressed() {
        escape(fake_view);
    }

    @Override
    protected void onPause() {
        super.onPause();
        wasPaused = true;
        //pause that music, else it keeps on playing while minimized
        this.mMusic.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wasPaused) {
            this.mMusic.start();
            this.mMusic.setLooping(true);
        }
    }

    public void chooseWBT() {
        DialogFragment DialogFragment = new selectWBTDialog();
        DialogFragment.show(getFragmentManager(), "WBT");
    }

    public void chooseATKType() {
        DialogFragment DialogFragment = new selectATKDialog();
        DialogFragment.show(getFragmentManager(), "ATK");
    }

    // initially prepare for a fight, set the wins to 0 and the remaining fights to 3
    // (can be changed later). Also set trainer fight true (this allows us to easily implement wild
    // Webertron fights later on, then call prepareNextFight.
    public void prepareFight(Intent intent) {
        getArrays();
        winsPlayer = 0;
        winsTeacher = 0;
        // set the remaining fights to 3 + 1 (4), cause we initially reduce it by 1,
        // so we only have 3 rounds
        remainingFights = 3 + 1;
        wbt_type_p = 0;
        wbt_type_t = 0;
        hp_p = 0;
        hp_t = 0;
        player_lvl = 0;
        teacher_lvl = 0;
        setTrainerFight(intent);
        setCurrentMapID(intent);
        setTeacherID(intent);
        setTeacherWonFights(intent);
        setTeacherName(intent);
        setTeacherToken(intent);
        prepareNextFight();
        //initializes music
        mMusic = MediaPlayer.create(this, R.raw.fight_music);
        //method for starting the music
        //music with loop
        mMusic.start();
        mMusic.setLooping(true);
    }

    // open Dialog to select Players Webertron, after that initialize the other parts
    // this happens in prepareNextFight2ndPart
    public void prepareNextFight() {
        chooseWBT();
    }

    // prepare for the next fight, reload the Webertron Images, set new Texts, and update the
    // HP and LVL Text View
    public void prepareNextFight2ndPart() {
        unlockButton();
        setWBTPic();
        setText();
        teacherWBT = 0;

        // Initialize all Values for the fight method
        player_lvl = getPlayerLevel();
        teacher_lvl = getTeacherLevel();
        wbt_type_p = getPlayerWBT();
        wbt_type_t = getTeacherWBT();
        hp_p = getHP(getT(wbt_type_p), player_lvl);
        hp_t = getHP(getT(wbt_type_t), teacher_lvl);
        lowerRemainingFights();
        setHP(hp_p, hp_t);
        setLVL(player_lvl, teacher_lvl);
        setWins(winsPlayer, winsTeacher);
    }

    // increases the players won games
    private void increaseN() {
        // max level is 20, so max amount of count games is 20² = 400
        if (getN() < 400) {
            setN(getN() + 1);
        }
    }

    //get N from the Shared Settings
    private int getN() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getInt("won_games", 0);
    }

    //store a new N to the Shared Settings
    private void setN(int n) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("won_games", n);
        editor.apply();
    }

    public void cheatAnswerN() {
        Intent sendIntent = new Intent();
        sendIntent.setComponent(new ComponentName("de.myself5.whereisdoncheats", "de.myself5.whereisdoncheats.MainActivity"));
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, Integer.toString(getN()));
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "WIDCheatCodeAnswer");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
        finish();
    }

    // this is our cheat method, to set our own level from the WID Cheat App
    public void cheatSetN(Intent intent) {
        int n = Integer.parseInt(intent.getStringExtra(Intent.EXTRA_TEXT));
        if (intent.getStringExtra(Intent.EXTRA_UID).equals("true"))
        // cheat implmentation to access all floors
        {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("maxMapID", 3);
            editor.apply();
        }
        setN(n);
        finish();
    }

    // returns the Players current Level in dependence of the won games
    private double getPlayerLevel() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int nFights = settings.getInt("won_games", 1);
        return Math.sqrt(nFights * 1D);
    }

    private void setTeacherWonFights(Intent intent) {
        String levelS = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!levelS.equals("")) {
            teacher_won_fights = Integer.parseInt(levelS);
        }
    }

    private void setTrainerFight(Intent intent){
        String trainer_fight = intent.getStringExtra(Intent.EXTRA_BCC);
        mTrainerFight = trainer_fight.equals("true");
    }

    private void setCurrentMapID(Intent intent) {
        String mapID = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        if (!mapID.equals("")) {
            mMapID = Integer.parseInt(mapID);
        }
    }

    private void setTeacherID(Intent intent) {
        String TeacherID = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME);
        if (!TeacherID.equals("")) {
            mTeacherID = Integer.parseInt(TeacherID);
        }
    }

    // returns the Teachers Level
    private double getTeacherLevel() {
        return Math.sqrt(teacher_won_fights * 1D);
    }

    // calculates the Webertrons HP in dependence of the type factor (u/atk_pwr), the players level
    // and a random factor between 0.85 and 1.15
    private double getAtkPwr(double atk_pwr, double level, double atk_type) {
        Random r = new Random();
        int Low = 85;
        // this calculates a number inside [85; 116) -> [85; 115]
        int High = 115 + 1;
        double R = (r.nextInt(High - Low) + Low) / 100D;
        double atkp;
        Log.e("WID", "R: " + R);
        atkp = (250 * Math.sqrt(level) * R * atk_pwr * atk_type);
        Log.e("WID", "atkp: " + atkp);
        return atkp;
    }

    // calculates the Webertrons HP in dependence of the type factor (t/type) and the players level
    private double getHP(double type, double level) {
        return (1500D * type * Math.sqrt(level));
    }

    // loads the arrays from the strings.xml
    // loads the array from drawable webertrons
    private void getArrays() {
        Resources res = getResources();
        attacks = res.getStringArray(R.array.attacks);
        sayings = res.getStringArray(R.array.sayings);
        wbtsayings = res.getStringArray(R.array.wbtsayings);
        mDrawableArray.add(R.drawable.wbt_1);
        mDrawableArray.add(R.drawable.wbt_2);
        mDrawableArray.add(R.drawable.wbt_3);
        mDrawableArray.add(R.drawable.wbt_4);
        mDrawableArray.add(R.drawable.wbt_5);
        mDrawableArray.add(R.drawable.wbt_6);
        mDrawableArray.add(R.drawable.wbt_7);
        mDrawableArray.add(R.drawable.wbt_8);
        mDrawableArray.add(R.drawable.wbt_9);
        mDrawableArray.add(R.drawable.wbt_10);
        mDrawableArray.add(R.drawable.wbt_11);
        mDrawableArray.add(R.drawable.wbt_12);
        mDrawableArray.add(R.drawable.wbt_13);
        mDrawableArray.add(R.drawable.wbt_14);
        mDrawableArray.add(R.drawable.wbt_15);
        mDrawableArray.add(R.drawable.wbt_16);
        mDrawableArray.add(R.drawable.wbt_17);
        mDrawableArray.add(R.drawable.wbt_18);
        mDrawableArray.add(R.drawable.wbt_19);
        mDrawableArray.add(R.drawable.wbt_20);
        mDrawableArray.add(R.drawable.wbt_21);
        mDrawableArray.add(R.drawable.wbt_22);
        mDrawableArray.add(R.drawable.wbt_23);
        mDrawableArray.add(R.drawable.wbt_24);
        mDrawableArray.add(R.drawable.wbt_25);
    }

    // randomly sets the Sayings and Attack Button Text
    private void setText() {
        Random r = new Random();
        int Low = 0;
        int High1 = attacks.length;
        int High2 = 0;
        if(mTrainerFight) {
            High2 = sayings.length;
        }else{
            High2 = wbtsayings.length;
        }
        int R1 = r.nextInt(High1 - Low) + Low;
        int R2 = r.nextInt(High2 - Low) + Low;
        attack_button.setText(attacks[R1]);

        if(mTrainerFight) {
            sayingsTV.setText(getTeacherName() + " " + sayings[R2]);
        }else{
            sayingsTV.setText(getTeacherName() + " " + wbtsayings[R2]);
        }
    }

    // sets the Sayings TextView text to R.string.fighting
    private void setFightingText() {
        sayingsTV.setText(getString(R.string.fighting));
    }

    // randomly loads a Webertron image to the ImageViews
    private void setWBTPic() {
        Random r = new Random();
        int Low = 0;
        int High = mDrawableArray.size();
        int R1 = r.nextInt(High - Low) + Low;
        int R2 = r.nextInt(High - Low) + Low;
        /*ImageView wbt1 = (ImageView) findViewById(R.id.webertron_1);*/
        wbt_p.setImageResource(mDrawableArray.get(R1));
        /*ImageView wbt2 = (ImageView) findViewById(R.id.webertron_2);*/
        //this ImageView needs to be mirrored
        wbt_t.setImageBitmap(flipImage(BitmapFactory.decodeResource(getResources(), mDrawableArray.get(R2)), 2));
    }

    // This allows us to mirror the Image so the Webertrons face each other
    // Source: http://shaikhhamadali.blogspot.de/2013/08/image-flipping-mirroring-in-imageview.html
    public Bitmap flipImage(Bitmap src, int type) {
        // create new matrix for transformation
        int FLIP_VERTICAL = 1;
        int FLIP_HORIZONTAL = 2;
        Matrix matrix = new Matrix();
        // if vertical
        if (type == FLIP_VERTICAL) {
            // y = y * -1
            matrix.preScale(1.0f, -1.0f);
        }
        // if horizontal
        else if (type == FLIP_HORIZONTAL) {
            // x = x * -1
            matrix.preScale(-1.0f, 1.0f);
            // unknown type
        } else {
            return null;
        }

        // return transformed image
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    // randomly assigns a Webertron Type to a teacher
    private int setTeacherWBTType() {
        Random r = new Random();
        int Low = 1;
        int High = 4;
        return r.nextInt(High - Low) + Low;
    }

    // returns the Webertron Type the Teacher got assigned randomly
    private int getTeacherWBT() {
        // if it's the first call after prepareNextFight set a random Webertron, else just return
        // the current value
        if (teacherWBT == 0) {
            teacherWBT = setTeacherWBTType();
        }
        return teacherWBT;
    }

    // returns the Webertron Type the Player selected
    private int getPlayerWBT() {
        return wbt_type_p;
    }

    public void fight(View unused) {
        if (!mButtonLocked) {
            //let the Webertrons jiggle while attacking each other
            startWBTAnimationHit();
            chooseATKType();
        } else {
            Log.e("WID", "Attack button locked!");
        }
    }

    public double getATKNormal() {
        Random r = new Random();
        int Low = 95;
        int High = 106;
        return (r.nextInt(High - Low) + Low) / 100D;
    }

    public double getATKRisky() {
        Random r = new Random();
        int Low = 15;
        int High = 201;
        return (r.nextInt(High - Low) + Low) / 100D;
    }

    public void fight2ndPart(double atk_type) {
        // atk has the random factor inside, so recalculate it every hit
        Random r = new Random();
        int Low = 0;
        int High = 101;
        int R = r.nextInt(High - Low) + Low;
        double t_atk_type;
        if (R <= 70) {
            t_atk_type = getATKNormal();
        } else {
            t_atk_type = getATKRisky();
        }
        double atk_p = getAtkPwr(getU(wbt_type_p), player_lvl, atk_type);
        double atk_t = getAtkPwr(getU(wbt_type_t), teacher_lvl, t_atk_type);

        // actually fight
        // see if one has killed the other already
        // Teacher attacks first, when you PKM is dead you can't attack the teacher
        hp_p = hp_p - (int) atk_t;
        hp_t = hp_t - (int) atk_p;

        // wait 2 seconds and set Text, do this in an Async to not freeze the Animation
        // but set an boolean to block the button during the Waiting Time

        setFightingText();
        lockButton();
        new FightEngineAsyncWait().execute("");
    }

    // updates the TextView with the Player and the Teachers Level
    private void setLVL(double player_lvl, double teacher_lvl) {
        TextView tvPlayerLVL = (TextView) findViewById(R.id.lvl_player);
        TextView tvTeacherLVL = (TextView) findViewById(R.id.lvl_teacher);
        tvPlayerLVL.setText(getString(R.string.LVL) + " " + Math.round(player_lvl));
        tvTeacherLVL.setText(getString(R.string.LVL) + " " + Math.round(teacher_lvl));
    }

    // updates the TextView with the Webertrons current HP
    private void setHP(double hp_p, double hp_t) {
        TextView tvPlayerHP = (TextView) findViewById(R.id.hp_player);
        TextView tvTeacherHP = (TextView) findViewById(R.id.hp_teacher);
        tvPlayerHP.setText(getString(R.string.HP) + " " + Math.round(hp_p));
        tvTeacherHP.setText(getString(R.string.HP) + " " + Math.round(hp_t));
    }

    // updates the TextView with the Players and Teachers current Wins
    private void setWins(int win_p, int win_t) {
        TextView tvWinsPlayer = (TextView) findViewById(R.id.winsPlayer);
        TextView tvWinsTeacher = (TextView) findViewById(R.id.winsTeacher);
        tvWinsPlayer.setText(getString(R.string.wins) + " " + getString(R.string.you) + " " + win_p);
        tvWinsTeacher.setText(getString(R.string.wins) + " " + getTeacherToken() + " " + win_t);
    }

    private void setTeacherName(Intent intent) {
        String nameS = intent.getStringExtra(Intent.EXTRA_TITLE);
        if (!nameS.equals("")) {
            teacher_name = nameS;
        }
    }

    private void setTeacherToken(Intent intent) {
        String tokenS = intent.getStringExtra(Intent.EXTRA_UID);
        if (!tokenS.equals("")) {
            teacher_token = tokenS;
        }
    }

    private String getTeacherName() {
        return teacher_name + ":";
    }

    private String getTeacherToken() {

        return teacher_token + ":";
    }

    // updates the TextView with the Remaining Fights
    private void lowerRemainingFights() {
        remainingFights--;
        TextView tvRemainingFights = (TextView) findViewById(R.id.remainingFights);
        tvRemainingFights.setText(getString(R.string.remainingFights) + " " + remainingFights);
    }


    // on Click Method for the Escape button.
    // Doesn't work if Player is fighting against a Teacher for the first time.
    // Is also called by the other classes to end the game (using the fake_view as a parameter)
    public void escape(View unused) {
        if (!mTrainerFight) {
            Intent startAct = new Intent(this, TMXTiledMapDigital.class);
            ResourceManager.setMapID(mMapID);
            finish();
            this.startActivity(startAct);
        } else {
            Toast.makeText(this, getString(R.string.cant_escape_in_trainerfight), Toast.LENGTH_LONG).show();
        }
    }

    // returns the U value of each Pokemon Type (it affects the max ATTK Power of each Webertron)
    public double getU(int type) {
        double u = 0;
        switch (type) {
            case 1:
                //Type Literatur
                u = 1.2D;
                break;
            case 2:
                //Type Mathe
                u = 0.8D;
                break;
            case 3:
                //Type Natur
                u = 1.0D;
                break;
        }
        return u;
    }

    // returns the T value of each Pokemon Type (it affects the max HP of each Webertron)
    public double getT(int type) {
        double t = 0;
        switch (type) {
            case 1:
                //Type Literatur
                t = 0.8D;
                break;
            case 2:
                //Type Mathe
                t = 1.2D;
                break;
            case 3:
                //Type Natur
                t = 1.0D;
                break;
        }
        return t;
    }

    // Show some Toast message when the Player wins,
    // and close the FightEngine when the Player won 2 (out of 3) matches
    private void winPlayer() {
        winsPlayer++;
        increaseN();
        if(mTrainerFight) {
            if (winsPlayer == 2) {
                Toast.makeText(this, getString(R.string.duel_won), Toast.LENGTH_LONG).show();
                mTrainerFight = false;
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                if (mMapID == settings.getInt("maxMapID", 0)) {
                    switch (mTeacherID) {
                        case 1:
                            editor.putBoolean("t1", true);
                            editor.apply();
                            break;
                        case 2:
                            editor.putBoolean("t2", true);
                            editor.apply();
                            break;
                        case 3:
                            editor.putBoolean("t3", true);
                            editor.apply();
                            break;
                        case 4:
                            editor.putBoolean("t4", true);
                            editor.apply();
                            break;
                        case 5:
                            editor.putBoolean("t5", true);
                            editor.apply();
                            break;
                        case 6:
                            editor.putBoolean("t6", true);
                            editor.apply();
                            break;
                        case 7:
                            editor.putBoolean("moser_approved", true);
                            editor.apply();
                            break;
                        default:
                            // this is just an empty case for some exeptions like wild wbts
                            break;
                    }
                }
                escape(fake_view);
            } else {
                Toast.makeText(this, getString(R.string.fight_won), Toast.LENGTH_LONG).show();
                prepareNextFight();
            }
        }else{
            Toast.makeText(this, getString(R.string.fight_won), Toast.LENGTH_LONG).show();
            escape(fake_view);
        }
    }

    // Show some Toast message when the Teacher wins,
    // and close the FightEngine when the Teacher won 2 (out of 3) matches
    private void winEnemy() {
        winsTeacher++;
        if(mTrainerFight) {
            if (winsTeacher == 2) {
                Toast.makeText(this, getString(R.string.duel_lost), Toast.LENGTH_LONG).show();
                mTrainerFight = false;
                escape(fake_view);
            } else {
                Toast.makeText(this, getString(R.string.fight_lost), Toast.LENGTH_LONG).show();
                prepareNextFight();
            }
        }else{
            // Show some text when losing against the WildWBT.
            // We only fight once against the wild wbt, not 3 times like against a Teacher
                Toast.makeText(this, getString(R.string.fight_lost), Toast.LENGTH_LONG).show();
                escape(fake_view);
        }
    }

    // empty method needed for the Animation Listener
    @Override
    public void onAnimationStart(Animation animation) {

    }

    // restart the animation when it ends (Androids Loop function has some problems)
    @Override
    public void onAnimationEnd(Animation animation) {
        if(!inHitAnimation) {
            startWBTAnimation();
        }else{
            startWBTAnimationHit();
        }
    }

    // empty method needed for the Animation Listener
    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    // make the Webertrons bounce up and down
    private void startWBTAnimation() {
        Animation upDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.up_down);
        upDown.setRepeatCount(Animation.INFINITE);
        upDown.setRepeatMode(Animation.REVERSE);
        upDown.setAnimationListener(this);
        wbt_p.setVisibility(View.VISIBLE);
        wbt_t.setVisibility(View.VISIBLE);
        wbt_p.startAnimation(upDown);
        wbt_t.startAnimation(upDown);
    }

    //make the Webertrons jiggle
    private void startWBTAnimationHit() {
        inHitAnimation = true;
        wbt_t.clearAnimation();
        wbt_p.clearAnimation();
        Animation hit = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.hit);
        hit.setAnimationListener(this);
        wbt_p.setVisibility(View.VISIBLE);
        wbt_p.startAnimation(hit);
        wbt_t.setVisibility(View.VISIBLE);
        wbt_t.startAnimation(hit);
    }


    // lock the Attack button
    public void lockButton() {
        mButtonLocked = true;
    }

    // unlock the Attack button
    public void unlockButton() {
        mButtonLocked = false;
    }

    public void makeHitSound() {
        //sleep and make hiz sounds
        MediaPlayer mHit;
        Random random = new Random();
        if (random.nextBoolean())
            mHit = MediaPlayer.create(this, R.raw.hit1);
        else
            mHit = MediaPlayer.create(this, R.raw.hit1);
        mHit.start();
    }

    /**
     * Just a little tool to sleep for 2 Seconds without blocking the View Thread for FightEngine
     * Created by Christian Oder on 26/06/2015.
     */
    public class FightEngineAsyncWait extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                TimeUnit.SECONDS.sleep(1);
                makeHitSound();
                TimeUnit.SECONDS.sleep(1);
                makeHitSound();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.e("WID", "doinBackground");
            //just to have a return, actually it's unused in the onPostExecute
            return "good Morning";
        }

        // unlock the button and update the HP TextView and set the saying and button text
        @Override
        protected void onPostExecute(String unused) {
            // randomly choose the order of the attacks
            // Either Player Webertron attacks first or Teacher Webertron does so
            Random random = new Random();
            inHitAnimation = false;
            startWBTAnimation();
            if (random.nextBoolean()) {
                if (hp_p < 1) {
                    winEnemy();
                } else {
                    if (hp_t < 1) {
                        winPlayer();
                    } else {
                        Log.e("WID", "onPostExecute");
                        unlockButton();
                        setHP(hp_p, hp_t);
                        setText();
                    }
                }
            } else {
                if (hp_t < 1) {
                    winPlayer();
                } else {
                    if (hp_p < 1) {
                        winEnemy();
                    } else {
                        Log.e("WID", "onPostExecute");
                        unlockButton();
                        setHP(hp_p, hp_t);
                        setText();
                    }
                }
            }
        }
    }

    public static class selectWBTDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.choose_wbt)
                    .setItems(R.array.webertons, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // first position is 0, so increase the value by 1 to match our
                            // Webertron types
                            // call the classes like this to prevent the need to be able to call
                            // non-static content from the static DialogFragment
                            // http://stackoverflow.com/questions/15414908/should-an-internal-dialogfragment-class-be-static-or-not
                            ((FightEngine) getActivity()).wbt_type_p = which + 1;
                            ((FightEngine) getActivity()).prepareNextFight2ndPart();
                        }
                    });
            setCancelable(false);
            return builder.create();
        }
    }

    public static class selectATKDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.choose_atk_type)
                    .setItems(R.array.atk_types, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // first position is 0, so increase the value by 1 to match our
                            // Webertron types
                            // call the classes like this to prevent the need to be able to call
                            // non-static content from the static DialogFragment
                            // http://stackoverflow.com/questions/15414908/should-an-internal-dialogfragment-class-be-static-or-not
                            if (which == 0) {
                                ((FightEngine) getActivity()).fight2ndPart(((FightEngine) getActivity()).getATKNormal());
                            } else if (which == 1) {
                                ((FightEngine) getActivity()).fight2ndPart(((FightEngine) getActivity()).getATKRisky());
                            } else {
                                Toast.makeText(getActivity(), getString(R.string.invalid_atk_type), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            setCancelable(false);
            return builder.create();
        }
    }
}

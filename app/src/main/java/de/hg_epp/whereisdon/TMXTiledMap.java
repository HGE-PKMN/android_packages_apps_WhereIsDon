package de.hg_epp.whereisdon;

import java.io.IOException;

import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.camera.hud.controls.DigitalOnScreenControl;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.tmx.TMXLayer;
import org.andengine.extension.tmx.TMXLoader;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.extension.tmx.TMXProperties;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.extension.tmx.TMXTileProperty;
import org.andengine.extension.tmx.util.exception.TMXLoadException;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.bitmap.AssetBitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.Constants;
import org.andengine.util.debug.Debug;

import android.opengl.GLES20;
import android.view.View;

/**
 * Based Off TMXTiledMapExample.java by
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga
 *
 * modified by Christian Oder for steering of the Sprite and FullScreen Mode
 * using Google Non Sticky Immersive Mode
 * @author Christian Oder
 */
public class TMXTiledMap extends SimpleBaseGameActivity  {

    // Non Sticky Immersive Mode
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    // ===========================================================
    // Constants
    // ===========================================================

    private static final int CAMERA_WIDTH = 240;
    private static final int CAMERA_HEIGHT = 135;

    // ===========================================================
    // Fields
    // ===========================================================

    private BoundCamera mCamera;

    private ITexture mPlayerTexture;
    private TiledTextureRegion mPlayerTextureRegion;

    private org.andengine.extension.tmx.TMXTiledMap mTMXTiledMap;

    private BitmapTextureAtlas mOnScreenControlTexture;
    private ITextureRegion mOnScreenControlBaseTextureRegion;
    private ITextureRegion mOnScreenControlKnobTextureRegion;

    static AnimatedSprite mPlayer;

    private DigitalOnScreenControl mDigitalOnScreenControl;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public EngineOptions onCreateEngineOptions() {
        this.mCamera = new BoundCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        this.mCamera.setBoundsEnabled(false);

        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_SENSOR, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
    }

    @Override
    public void onCreateResources() throws IOException {
        this.mPlayerTexture = new AssetBitmapTexture(this.getTextureManager(), this.getAssets(), "gfx/player.png", TextureOptions.DEFAULT);
        this.mPlayerTextureRegion = TextureRegionFactory.extractTiledFromTexture(this.mPlayerTexture, 3, 4);
        this.mPlayerTexture.load();

        this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
        this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "gfx/onscreen_control_base.png", 0, 0);
        this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "gfx/onscreen_control_knob.png", 128, 0);
        this.mOnScreenControlTexture.load();
    }

    @Override
    public Scene onCreateScene() {
        this.mEngine.registerUpdateHandler(new FPSLogger());

        final Scene scene = new Scene();

        try {
            final TMXLoader tmxLoader = new TMXLoader(this.getAssets(), this.mEngine.getTextureManager(), TextureOptions.BILINEAR_PREMULTIPLYALPHA, this.getVertexBufferObjectManager(), new ITMXTilePropertiesListener() {
                @Override
                public void onTMXTileWithPropertiesCreated(final org.andengine.extension.tmx.TMXTiledMap pTMXTiledMap, final TMXLayer pTMXLayer, final TMXTile pTMXTile, final TMXProperties<TMXTileProperty> pTMXTileProperties) {
                }
            });
            this.mTMXTiledMap = tmxLoader.loadFromAsset("tmx/basic_asp.tmx");
            this.mTMXTiledMap.setOffsetCenter(0, 0);
        } catch (final TMXLoadException e) {
            Debug.e(e);
        }

        scene.attachChild(this.mTMXTiledMap);

		/* Make the camera not exceed the bounds of the TMXEntity. */
        this.mCamera.setBoundsEnabled(false);
        this.mCamera.setBounds(0, 0, this.mTMXTiledMap.getWidth(), this.mTMXTiledMap.getHeight());
        this.mCamera.setBoundsEnabled(true);

        final float centerX = CAMERA_WIDTH / 2;
        final float centerY = CAMERA_HEIGHT / 2;

		/* Create the sprite and add it to the scene. */
        mPlayer = new AnimatedSprite(centerX, centerY, this.mPlayerTextureRegion, this.getVertexBufferObjectManager());
        mPlayer.setOffsetCenterY(0);
        this.mCamera.setChaseEntity(mPlayer);
        final PhysicsHandler physicsHandler = new PhysicsHandler(mPlayer);
        mPlayer.registerUpdateHandler(physicsHandler);

        		/* Velocity control (left). */
        // Amount of Pixels away from 0/0 (top left corner)
        final float edge_space = 40;
        final AnalogOnScreenControl velocityOnScreenControl = new AnalogOnScreenControl(edge_space, edge_space, this.mCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, this.getVertexBufferObjectManager(), new AnalogOnScreenControl.IAnalogOnScreenControlListener() {
            @Override
            public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {

                physicsHandler.setVelocity(pValueX * 100, pValueY * 100);
            }

            @Override
            public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
				/* Nothing. */
            }
        });
        velocityOnScreenControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        velocityOnScreenControl.getControlBase().setAlpha(0.5f);
        velocityOnScreenControl.getControlBase().setScale(0.5f);

        scene.setChildScene(velocityOnScreenControl);

		/* Now we are going to create a rectangle that will  always highlight the tile below the feet of the pEntity. */
        final Rectangle currentTileRectangle = new Rectangle(0, 0, this.mTMXTiledMap.getTileWidth(), this.mTMXTiledMap.getTileHeight(), this.getVertexBufferObjectManager());
		/* Set the OffsetCenter to 0/0, so that it aligns with the TMXTiles. */
        //currentTileRectangle.setOffsetCenter(0, 0);
        //currentTileRectangle.setColor(1, 0, 0, 0.25f);
        //scene.attachChild(currentTileRectangle);

		/* The layer for the player to walk on. */
        final TMXLayer tmxLayer = this.mTMXTiledMap.getTMXLayers().get(0);

        scene.registerUpdateHandler(new IUpdateHandler() {
            @Override
            public void reset() {
            }

            @Override
            public void onUpdate(final float pSecondsElapsed) {
				/* Get the scene-coordinates of the players feet. */
                final float[] playerFootCordinates = mPlayer.convertLocalCoordinatesToSceneCoordinates(16, 1);

				/* Get the tile the feet of the player are currently waking on. */
                final TMXTile tmxTile = tmxLayer.getTMXTileAt(playerFootCordinates[Constants.VERTEX_INDEX_X], playerFootCordinates[Constants.VERTEX_INDEX_Y]);
                if (tmxTile != null) {
                    // tmxTile.setTextureRegion(null); <-- Eraser-style removing of tiles =D
                    currentTileRectangle.setPosition(tmxLayer.getTileX(tmxTile.getTileColumn()), tmxLayer.getTileY(tmxTile.getTileRow()));
                }
            }
        });
        scene.attachChild(mPlayer);

        return scene;
    }

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
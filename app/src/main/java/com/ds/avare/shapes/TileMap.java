/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.shapes;

import android.content.Context;
import android.os.AsyncTask;

import com.ds.avare.place.Boundaries;
import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;


/**
 * 
 * @author zkhan, SteveAtChartbundle
 * A cache of tiles
 */
public class TileMap extends MapBase {


    private static final int SIZE = BitmapHolder.HEIGHT * BitmapHolder.WIDTH * 2; // RGB565 = 2
    private int mNumShowing;

    /**
     * @param context
     */
    public TileMap(Context context) {
        super(context, SIZE, (new Preferences(context)).getTilesNumber(context));
        mNumShowing = 0;
        mTileTask = null;
    }

    public void reload(String[] tileNames, GenericCallback c) {
        mNumShowing = super.reloadMap(tileNames, c);
    }

    private AsyncTask mTileTask;


    /**
     * Lets call chart showing partial when tiles showing are below a threshold
     *
     * @return
     */
    @Override
    public boolean isChartPartial() {
        return mNumShowing <= 0;
    }


    /**
     * Function that loads new tiles in background
     *
     */
    public void loadTiles(final double lon, final double lat, final Pan panIn, final float macro, final Scale scale, final double bearing, final GenericCallback callbackDone) {

        if(mTileTask != null && mTileTask.getStatus() == AsyncTask.Status.RUNNING) {
            mTileTask.cancel(true);
        }

        mTileTask = new AsyncTask<Void, Object, TileUpdate>() {
            double offsets[] = new double[2];
            double p[] = new double[2];
            int     movex;
            int     movey;
            float factor;
            String   tileNames[];
            Tile centerTile;
            Tile gpsTile;
            String chart = "";

            /**
             *
             */
            @Override
            protected void onPreExecute () {

                /*
                 * Now draw in background, but first find tiles in foreground
                 * Find tile at my GPS location
                 */
                gpsTile = new Tile(mContext, mPref, lon, lat, (double) scale.downSample());

                offsets[0] = gpsTile.getOffsetX(lon);
                offsets[1] = gpsTile.getOffsetY(lat);
                p[0] = gpsTile.getPx();
                p[1] = gpsTile.getPy();

                factor = macro / (float) scale.getMacroFactor();

                /*
                 * Make a copy of Pan to find next tile set in case this gets stopped, we do not
                 * destroy our Pan information.
                 */
                Pan pan = new Pan(panIn);
                double n_x = pan.getMoveX();
                double n_y = pan.getMoveY();

                if (mPref.isTrackUp()) {
                    double p[] = new double[2];
                    p = Helper.rotateCoord(0.0, 0.0, bearing, n_x, n_y);
                    pan.setMove((float) (p[0] * factor), (float) (p[1] * factor));
                } else {
                    pan.setMove((float) (n_x * factor), (float) (n_y * factor));
                }
                movex = pan.getTileMoveXWithoutTear();
                movey = pan.getTileMoveYWithoutTear();

                // Find tile of where I am on screen
                centerTile = new Tile(mContext, mPref, gpsTile, movex, movey);

                /*
                 * Neighboring tiles with center and pan
                 */
                int i = 0;
                tileNames = new String[getTilesNum()];
                int ty = (int) (getYTilesNum() / 2);
                int tx = (int) (getXTilesNum() / 2);
                for (int tiley = ty; tiley >= -ty; tiley--) {
                    for (int tilex = -tx; tilex <= tx; tilex++) {
                        tileNames[i++] = centerTile.getTileNeighbor(tilex, tiley);
                    }
                }
            }

            @Override
            protected TileUpdate doInBackground(Void... vals) {
                Thread.currentThread().setName("Tile");
                /*
                 * Load tiles, draw in UI thread
                 */
                reload(tileNames,
                        // As tiles are loaded, callback to notify us
                        new GenericCallback() {
                            @Override
                            public Object callback(Object o1, Object o2) {
                                publishProgress(o1, o2);
                                return null;
                            }
                        }
                );
                if(isChartPartial()) {
                    // If tiles not found, find name of chart we are on to show to user
                    chart = Boundaries.getInstance().findChartOn(centerTile.getChartIndex(), centerTile.getLongitude(), centerTile.getLatitude());
                }
                TileUpdate t = new TileUpdate();
                t.movex = movex;
                t.movey = movey;
                t.centerTile = centerTile;
                t.gpsTile = gpsTile;
                t.offsets = offsets;
                t.factor = factor;
                t.chart = chart;

                return t;
            }

            @Override
            protected void onProgressUpdate(Object... objs) {
                // Put in bitmap cache a new loaded tile
                TileMap t = (TileMap)objs[0];
                BitmapHolder b = (BitmapHolder)objs[1];
                t.addInCache(b);
                // Do we really want to update the location view and show user the tiles being loaded?
            }

            @Override
            protected void onPostExecute(TileUpdate t) {
                /*
                 * UI thread
                 */
                if(t != null) {
                    callbackDone.callback(TileMap.this, t);
                }
            }

        }.execute(null, null, null);
    }

    /**
     * Use this with handler to update tiles in UI thread
     * @author zkhan
     *
     */
    public class TileUpdate {
        public String chart;
        public double offsets[];
        public int movex;
        public int movey;
        public float factor;
        public Tile centerTile;
        public Tile gpsTile;
    }

}
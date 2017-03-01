/*
Copyright (c) 2017, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.shapes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;

/**
 * Created by zkhan on 2/10/17.
 *
 * Create a sub tile out of main tile and surrounding tiles.
 * This puts location of airplane on center of a 384x384 tile without need to make a 3 * 3 matrix of full tiles
 * This heavily relies on the fact that mail tile is 512x512, and sub tiles are 128x128 with output image of 384x384
 */

public class SubTile extends Tile {

    public static final int DIM = 384;


    private double mLatSub = -1;
    private double mLonSub = -1;

    private String mNameSub = "";

    private int mLocation = -1;


    // to divide 512 x 512 tiles to a 3x3 array of 128x128 tiles
    // 8x8 neighbor table where main tile top left (row = 0, col = 0) starts at [2][2] in it, and has 9 tiles for which neighbors apply
    // each entry is {neighbor relative row index, neighbor relative col index, tile in that neighbor

    private static final int tilesNeighbors[][][] = {
            {{-1, -1, 10}, {-1, -1, 11}, {-1,  0,  8}, {-1,  0,  9}, {-1,  0, 10}, {-1,  0, 11}, {-1,  1,  8}, {-1,  1,  9}},
            {{-1, -1, 14}, {-1, -1, 15}, {-1,  0, 12}, {-1,  0, 13}, {-1,  0, 14}, {-1,  0, 15}, {-1,  1, 12}, {-1,  1, 13}},
            {{ 0, -1,  2}, { 0, -1,  3}, { 0,  0,  0}, { 0,  0,  1}, { 0,  0,  2}, { 0,  0,  3}, { 0,  1,  0}, { 0,  1,  1}},
            {{ 0, -1,  6}, { 0, -1,  7}, { 0,  0,  4}, { 0,  0,  5}, { 0,  0,  6}, { 0,  0,  7}, { 0,  1,  4}, { 0,  1,  5}},
            {{ 0, -1, 10}, { 0, -1, 11}, { 0,  0,  8}, { 0,  0,  9}, { 0,  0, 10}, { 0,  0, 11}, { 0,  1,  8}, { 0,  1,  9}},
            {{ 0, -1, 14}, { 0, -1, 15}, { 0,  0, 12}, { 0,  0, 13}, { 0,  0, 14}, { 0,  0, 15}, { 0,  1, 12}, { 0,  1, 13}},
            {{ 1, -1,  2}, { 1, -1,  3}, { 1,  0,  0}, { 1,  0,  1}, { 1,  0,  2}, { 1,  0,  3}, { 1,  1,  0}, { 1,  1,  1}},
            {{ 1, -1,  6}, { 1, -1,  7}, { 1,  0,  4}, { 1,  0,  5}, { 1,  0,  6}, { 1,  0,  7}, { 1,  1,  4}, { 1,  1,  5}},
    } ;

    //4x4 of 128 of 512
    private static final int dims[][] = {
            {  0,   0}, {  0, 128}, {  0, 256}, {  0, 384},
            {128,   0}, {128, 128}, {128, 256}, {128, 384},
            {256,   0}, {256, 128}, {256, 256}, {256, 384},
            {384,   0}, {384, 128}, {384, 256}, {384, 384},
    } ;



    public SubTile(Context ctx, Preferences pref, double lon, double lat, double zoom, String index) {
        super(ctx, pref, lon, lat, zoom, index);
        calculate(lon, lat);
    }

    // tells which sub tile (128) I am on based on x, y pixel location
    public static int whereAmI(int x, int y) {
        int x0 = ((x / 128) * 128);
        int y0 = ((y / 128) * 128);
        for(int i = 0; i < 16; i++) {
            if(y0 == dims[i][0] && x0 == dims[i][1]) {
                return i;
            }
        }
        return -1;
    }

    public static Rect whatAreMyDims(int location) {
        Rect ret = new Rect();
        ret.top = dims[location][0];
        ret.left = dims[location][1];
        ret.bottom = dims[location][0] + 128;
        ret.right = dims[location][1] + 128;
        return ret;
    }

    public static Rect whatAreMyDims(int row, int col) {
        Rect ret = new Rect();
        ret.top = row * 128;
        ret.left = col * 128;
        ret.bottom = ret.top + 128;
        ret.right = ret.left + 128;;
        return ret;
    }

    private static final int locCol(int location) {
        return location % 4;
    }

    private static final int locRow(int location) {
        return location / 4;
    }

    private static int[][][] whatAreMyNeighbors(int row, int col) {
        // its 3x3 tiles with particulars of that tile in third col
        // row, col, location (use whatAreMyDims on location for actual dimensions of tile)
        int neighbors[][][] = new int[3][3][3];
        neighbors[0][0][0] = tilesNeighbors[2 + row - 1][2 + col - 1][0];
        neighbors[0][0][1] = tilesNeighbors[2 + row - 1][2 + col - 1][1];
        neighbors[0][0][2] = tilesNeighbors[2 + row - 1][2 + col - 1][2];

        neighbors[0][1][0] = tilesNeighbors[2 + row - 1][2 + col - 0][0];
        neighbors[0][1][1] = tilesNeighbors[2 + row - 1][2 + col - 0][1];
        neighbors[0][1][2] = tilesNeighbors[2 + row - 1][2 + col - 0][2];

        neighbors[0][2][0] = tilesNeighbors[2 + row - 1][2 + col + 1][0];
        neighbors[0][2][1] = tilesNeighbors[2 + row - 1][2 + col + 1][1];
        neighbors[0][2][2] = tilesNeighbors[2 + row - 1][2 + col + 1][2];

        neighbors[1][0][0] = tilesNeighbors[2 + row + 0][2 + col - 1][0];
        neighbors[1][0][1] = tilesNeighbors[2 + row + 0][2 + col - 1][1];
        neighbors[1][0][2] = tilesNeighbors[2 + row + 0][2 + col - 1][2];

        neighbors[1][1][0] = tilesNeighbors[2 + row + 0][2 + col + 0][0];
        neighbors[1][1][1] = tilesNeighbors[2 + row + 0][2 + col + 0][1];
        neighbors[1][1][2] = tilesNeighbors[2 + row + 0][2 + col + 0][2];

        neighbors[1][2][0] = tilesNeighbors[2 + row + 0][2 + col + 1][0];
        neighbors[1][2][1] = tilesNeighbors[2 + row + 0][2 + col + 1][1];
        neighbors[1][2][2] = tilesNeighbors[2 + row + 0][2 + col + 1][2];

        neighbors[2][0][0] = tilesNeighbors[2 + row + 1][2 + col - 1][0];
        neighbors[2][0][1] = tilesNeighbors[2 + row + 1][2 + col - 1][1];
        neighbors[2][0][2] = tilesNeighbors[2 + row + 1][2 + col - 1][2];

        neighbors[2][1][0] = tilesNeighbors[2 + row + 1][2 + col + 0][0];
        neighbors[2][1][1] = tilesNeighbors[2 + row + 1][2 + col + 0][1];
        neighbors[2][1][2] = tilesNeighbors[2 + row + 1][2 + col + 0][2];

        neighbors[2][2][0] = tilesNeighbors[2 + row + 1][2 + col + 1][0];
        neighbors[2][2][1] = tilesNeighbors[2 + row + 1][2 + col + 1][1];
        neighbors[2][2][2] = tilesNeighbors[2 + row + 1][2 + col + 1][2];

        return neighbors;
    }

    private void calculate(double lon, double lat) {
        mLocation = whereAmI((int) super.getOffsetX(lon) + BitmapHolder.WIDTH / 2, (int) super.getOffsetY(lat) + BitmapHolder.HEIGHT / 2);

//      | | | | |
//      | | | | |
//      | | | | |
//      | | | | |
        Rect s = whatAreMyDims(mLocation);


        // find center lat/lon of subtile
        double x = s.left - BitmapHolder.WIDTH / 2 + 128 / 2;
        double y = s.top - BitmapHolder.HEIGHT / 2 + 128 / 2;
        mLatSub = super.getLatitude() + getPy() * y;
        mLonSub = super.getLongitude() + getPx() * x;

        mNameSub = super.getName() + "_" + mLocation;
    }


        /**
         * Load image
         * @param bh
         * @param mapsFolder
         * @return
         */
    public boolean load(BitmapHolder bh, String mapsFolder) {

        boolean ret = false;

        if(mLocation < 0) {
            return ret;
        }

        int[][][] tiles = whatAreMyNeighbors(locRow(mLocation), locCol(mLocation));

        for(int row = 0; row < 3; row++) {
            for(int col = 0; col < 3; col++) {
                // load tiles and draw in main bitmap
                Rect src = whatAreMyDims(tiles[row][col][2]);
                Rect dst = whatAreMyDims(row, col);
                BitmapHolder b = new BitmapHolder(mapsFolder + "/" + super.getTileNeighbor(tiles[row][col][1], -tiles[row][col][0]), Bitmap.Config.ARGB_8888, src);

                if(b.getBitmap() != null) {
                    bh.drawInBitmap(b, null, dst);
                    ret = true;
                }
                b.recycle();
            }
        }

        return ret;
    }


    @Override
    public double getLatitude() {
        return mLatSub;
    }

    @Override
    public double getLongitude() {
        return mLonSub;
    }

    @Override
    public double getOffsetX(double lon) {

        double px = getPx();

        if(px != 0) {
            return(lon - mLonSub) / px;
        }
        else {
            return(0);
        }
    }

    @Override
    public double getOffsetY(double lat) {

        double py = getPy();

        if(py != 0) {
            return (lat - mLatSub) / py;
        }
        else {
            return(0);
        }
    }


    @Override
    public String getName() {
        return mNameSub;
    }
}

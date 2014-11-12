package com.ds.avare.cap;

import android.os.Message;

import com.ds.avare.StorageService;
import com.ds.avare.position.Pan;

public class GridDrawTask implements Runnable {

	public boolean running = true;
	private boolean runAgain = false;
	
	/**
     * Storage service that contains all the state
     */
    public StorageService              mService;

	@Override
	public void run() {
		Thread.currentThread().setName("CAPGrid");
		
		while(running) {
			if(!runAgain) {
                try {
                    Thread.sleep(1000 * 3600);
                }
                catch(Exception e) {
                    
                }
            }
            runAgain = false;
            
            if(null == mService) {
                continue;
            }
            
            /*
             * Now draw in background
             */
            int level = mScale.downSample();
            gpsTile = mImageDataSource.findClosest(lon, lat, offsets, p, level);
            
            if(gpsTile == null) {
                continue;
            }
            
            float factor = (float)mMacro / (float)mScale.getMacroFactor();

            /*
             * Make a copy of Pan to find next tile set in case this gets stopped, we do not 
             * destroy our Pan information.
             */
            Pan pan = new Pan(mPan);
            pan.setMove((float)(mPan.getMoveX() * factor), (float)(mPan.getMoveY() * factor));
            movex = pan.getTileMoveXWithoutTear();
            movey = pan.getTileMoveYWithoutTear();
            
            String newt = gpsTile.getNeighbor(movey, movex);
            centerTile = mImageDataSource.findTile(newt);
            if(null == centerTile) {
                continue;
            }

            /*
             * Neighboring tiles with center and pan
             */
            int i = 0;
            tileNames = new String[mService.getTiles().getTilesNum()];
            for(int tiley = -(int)(mService.getTiles().getYTilesNum() / 2) ; 
                    tiley <= (mService.getTiles().getYTilesNum() / 2); tiley++) {
                for(int tilex = -(int)(mService.getTiles().getXTilesNum() / 2); 
                        tilex <= (mService.getTiles().getXTilesNum() / 2) ; tilex++) {
                    tileNames[i++] = centerTile.getNeighbor(tiley, tilex);
                }
            }

            /*
             * Load tiles, draw in UI thread
             */
            try {
                mService.getTiles().reload(tileNames);
            }
            catch(Exception e) {
                /*
                 * We are interrupted for new movement. Try again to load new tiles.
                 */
                runAgain = true;
                continue;
            }
            
            /*
             * UI thread
             */
            TileUpdate t = new TileUpdate();
            t.movex = movex;
            t.movey = movey;
            t.centerTile = centerTile;
            t.offsets = offsets;
            t.p = p;
            t.factor = factor;
            
            Message m = mHandler.obtainMessage();
            m.obj = t;
            mHandler.sendMessage(m);
		}
	}

}

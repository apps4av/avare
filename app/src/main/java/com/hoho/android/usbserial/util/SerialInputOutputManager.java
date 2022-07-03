/* Copyright 2011 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

package com.hoho.android.usbserial.util;

import android.util.Log;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SerialInputOutputManager implements Runnable {
   private static final String TAG = SerialInputOutputManager.class.getSimpleName();
   private static final boolean DEBUG = true;
   private static final int READ_WAIT_MILLIS = 200;
   private static final int BUFSIZ = 4096;
   private final UsbSerialDriver mDriver;
   private final ByteBuffer mReadBuffer;
   private final ByteBuffer mWriteBuffer;
   private SerialInputOutputManager.State mState;
   private SerialInputOutputManager.Listener mListener;

   public SerialInputOutputManager(UsbSerialDriver driver) {
      this(driver, (SerialInputOutputManager.Listener)null);
   }

   public SerialInputOutputManager(UsbSerialDriver driver, SerialInputOutputManager.Listener listener) {
      this.mReadBuffer = ByteBuffer.allocate(4096);
      this.mWriteBuffer = ByteBuffer.allocate(4096);
      this.mState = SerialInputOutputManager.State.STOPPED;
      this.mDriver = driver;
      this.mListener = listener;
   }

   public synchronized void setListener(SerialInputOutputManager.Listener listener) {
      this.mListener = listener;
   }

   public synchronized SerialInputOutputManager.Listener getListener() {
      return this.mListener;
   }

   public void writeAsync(byte[] data) {
      synchronized(this.mWriteBuffer) {
         this.mWriteBuffer.put(data);
      }
   }

   public synchronized void stop() {
      if (this.getState() == SerialInputOutputManager.State.RUNNING) {
         Log.i(TAG, "Stop requested");
         this.mState = SerialInputOutputManager.State.STOPPING;
      }

   }

   private synchronized SerialInputOutputManager.State getState() {
      return this.mState;
   }

   public void run() {
      synchronized(this) {
         if (this.getState() != SerialInputOutputManager.State.STOPPED) {
            throw new IllegalStateException("Already running.");
         }

         this.mState = SerialInputOutputManager.State.RUNNING;
      }

      Log.i(TAG, "Running ..");

      try {
         while(this.getState() == SerialInputOutputManager.State.RUNNING) {
            this.step();
         }

         Log.i(TAG, "Stopping mState=" + this.getState());
      } catch (Exception var12) {
         Log.w(TAG, "Run ending due to exception: " + var12.getMessage(), var12);
         SerialInputOutputManager.Listener listener = this.getListener();
         if (listener != null) {
            listener.onRunError(var12);
         }
      } finally {
         synchronized(this) {
            this.mState = SerialInputOutputManager.State.STOPPED;
            Log.i(TAG, "Stopped.");
         }
      }

   }

   private void step() throws IOException {
      int len = this.mDriver.read(this.mReadBuffer.array(), 200);
      if (len > 0) {
         Log.d(TAG, "Read data len=" + len);
         SerialInputOutputManager.Listener listener = this.getListener();
         if (listener != null) {
            byte[] data = new byte[len];
            this.mReadBuffer.get(data, 0, len);
            listener.onNewData(data);
         }

         this.mReadBuffer.clear();
      }

      byte[] outBuff = (byte[])null;
      synchronized(this.mWriteBuffer) {
         if (this.mWriteBuffer.position() > 0) {
            len = this.mWriteBuffer.position();
            outBuff = new byte[len];
            this.mWriteBuffer.rewind();
            this.mWriteBuffer.get(outBuff, 0, len);
            this.mWriteBuffer.clear();
         }
      }

      if (outBuff != null) {
         Log.d(TAG, "Writing data len=" + len);
         this.mDriver.write(outBuff, 200);
      }

   }

   public interface Listener {
      void onNewData(byte[] var1);

      void onRunError(Exception var1);
   }

   private static enum State {
      STOPPED,
      RUNNING,
      STOPPING;
   }
}

/* Copyright 2013 Google Inc.
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

package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * USB CDC/ACM serial driver implementation.
 *
 * @author mike wakerly (opensource@hoho.com)
 * @see <a
 *      href="http://www.usb.org/developers/devclass_docs/usbcdc11.pdf">Universal
 *      Serial Bus Class Definitions for Communication Devices, v1.1</a>
 */
public class CdcAcmSerialDriver extends CommonUsbSerialDriver {
   private final String TAG = CdcAcmSerialDriver.class.getSimpleName();
   private UsbInterface mControlInterface;
   private UsbInterface mDataInterface;
   private UsbEndpoint mControlEndpoint;
   private UsbEndpoint mReadEndpoint;
   private UsbEndpoint mWriteEndpoint;
   private boolean mRts = false;
   private boolean mDtr = false;
   private static final int USB_RECIP_INTERFACE = 1;
   private static final int USB_RT_ACM = 33;
   private static final int SET_LINE_CODING = 32;
   private static final int GET_LINE_CODING = 33;
   private static final int SET_CONTROL_LINE_STATE = 34;
   private static final int SEND_BREAK = 35;

   public CdcAcmSerialDriver(UsbDevice device, UsbDeviceConnection connection) {
      super(device, connection);
   }

   public void open() throws IOException {
      Log.d(this.TAG, "claiming interfaces, count=" + this.mDevice.getInterfaceCount());
      Log.d(this.TAG, "Claiming control interface.");
      this.mControlInterface = this.mDevice.getInterface(0);
      Log.d(this.TAG, "Control iface=" + this.mControlInterface);
      if (!this.mConnection.claimInterface(this.mControlInterface, true)) {
         throw new IOException("Could not claim control interface.");
      } else {
         this.mControlEndpoint = this.mControlInterface.getEndpoint(0);
         Log.d(this.TAG, "Control endpoint direction: " + this.mControlEndpoint.getDirection());
         Log.d(this.TAG, "Claiming data interface.");
         this.mDataInterface = this.mDevice.getInterface(1);
         Log.d(this.TAG, "data iface=" + this.mDataInterface);
         if (!this.mConnection.claimInterface(this.mDataInterface, true)) {
            throw new IOException("Could not claim data interface.");
         } else {
            this.mReadEndpoint = this.mDataInterface.getEndpoint(1);
            Log.d(this.TAG, "Read endpoint direction: " + this.mReadEndpoint.getDirection());
            this.mWriteEndpoint = this.mDataInterface.getEndpoint(0);
            Log.d(this.TAG, "Write endpoint direction: " + this.mWriteEndpoint.getDirection());
         }
      }
   }

   private int sendAcmControlMessage(int request, int value, byte[] buf) {
      return this.mConnection.controlTransfer(33, request, value, 0, buf, buf != null ? buf.length : 0, 5000);
   }

   public void close() throws IOException {
      this.mConnection.close();
   }

   public int read(byte[] dest, int timeoutMillis) throws IOException {
      synchronized(this.mReadBufferLock) {
         int readAmt = Math.min(dest.length, this.mReadBuffer.length);
         int numBytesRead = this.mConnection.bulkTransfer(this.mReadEndpoint, this.mReadBuffer, readAmt, timeoutMillis);
         if (numBytesRead < 0) {
            return 0;
         } else {
            System.arraycopy(this.mReadBuffer, 0, dest, 0, numBytesRead);
            return numBytesRead;
         }
      }
   }

   public int write(byte[] src, int timeoutMillis) throws IOException {
      int offset;
      int amtWritten;
      for(offset = 0; offset < src.length; offset += amtWritten) {
         int writeLength;
         synchronized(this.mWriteBufferLock) {
            writeLength = Math.min(src.length - offset, this.mWriteBuffer.length);
            byte[] writeBuffer;
            if (offset == 0) {
               writeBuffer = src;
            } else {
               System.arraycopy(src, offset, this.mWriteBuffer, 0, writeLength);
               writeBuffer = this.mWriteBuffer;
            }

            amtWritten = this.mConnection.bulkTransfer(this.mWriteEndpoint, writeBuffer, writeLength, timeoutMillis);
         }

         if (amtWritten <= 0) {
            throw new IOException("Error writing " + writeLength + " bytes at offset " + offset + " length=" + src.length);
         }

         Log.d(this.TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
      }

      return offset;
   }

   public void setParameters(int baudRate, int dataBits, int stopBits, int parity) {
      byte stopBitsByte;
      switch(stopBits) {
      case 1:
         stopBitsByte = 0;
         break;
      case 2:
         stopBitsByte = 2;
         break;
      case 3:
         stopBitsByte = 1;
         break;
      default:
         throw new IllegalArgumentException("Bad value for stopBits: " + stopBits);
      }

      byte parityBitesByte;
      switch(parity) {
      case 0:
         parityBitesByte = 0;
         break;
      case 1:
         parityBitesByte = 1;
         break;
      case 2:
         parityBitesByte = 2;
         break;
      case 3:
         parityBitesByte = 3;
         break;
      case 4:
         parityBitesByte = 4;
         break;
      default:
         throw new IllegalArgumentException("Bad value for parity: " + parity);
      }

      byte[] msg = new byte[]{(byte)(baudRate & 255), (byte)(baudRate >> 8 & 255), (byte)(baudRate >> 16 & 255), (byte)(baudRate >> 24 & 255), stopBitsByte, parityBitesByte, (byte)dataBits};
      this.sendAcmControlMessage(32, 0, msg);
   }

   public boolean getCD() throws IOException {
      return false;
   }

   public boolean getCTS() throws IOException {
      return false;
   }

   public boolean getDSR() throws IOException {
      return false;
   }

   public boolean getDTR() throws IOException {
      return this.mDtr;
   }

   public void setDTR(boolean value) throws IOException {
      this.mDtr = value;
      this.setDtrRts();
   }

   public boolean getRI() throws IOException {
      return false;
   }

   public boolean getRTS() throws IOException {
      return this.mRts;
   }

   public void setRTS(boolean value) throws IOException {
      this.mRts = value;
      this.setDtrRts();
   }

   private void setDtrRts() {
      int value = (this.mRts ? 2 : 0) | (this.mDtr ? 1 : 0);
      this.sendAcmControlMessage(34, value, (byte[])null);
   }

   public static Map<Integer, int[]> getSupportedDevices() {
      Map<Integer, int[]> supportedDevices = new LinkedHashMap();
      supportedDevices.put(9025, new int[]{1, 67, 16, 66, 59, 68, 63, 68, 32822});
      supportedDevices.put(5824, new int[]{1155});
      supportedDevices.put(1003, new int[]{8260});
      supportedDevices.put(7855, new int[]{4});
      return supportedDevices;
   }
}

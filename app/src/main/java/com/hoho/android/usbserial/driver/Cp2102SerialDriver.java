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

public class Cp2102SerialDriver extends CommonUsbSerialDriver {
   private static final String TAG = Cp2102SerialDriver.class.getSimpleName();
   private static final int DEFAULT_BAUD_RATE = 9600;
   private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;
   private static final int REQTYPE_HOST_TO_DEVICE = 65;
   private static final int SILABSER_IFC_ENABLE_REQUEST_CODE = 0;
   private static final int SILABSER_SET_BAUDDIV_REQUEST_CODE = 1;
   private static final int SILABSER_SET_LINE_CTL_REQUEST_CODE = 3;
   private static final int SILABSER_SET_MHS_REQUEST_CODE = 7;
   private static final int SILABSER_SET_BAUDRATE = 30;
   private static final int SILABSER_FLUSH_REQUEST_CODE = 18;
   private static final int FLUSH_READ_CODE = 10;
   private static final int FLUSH_WRITE_CODE = 5;
   private static final int UART_ENABLE = 1;
   private static final int UART_DISABLE = 0;
   private static final int BAUD_RATE_GEN_FREQ = 3686400;
   private static final int MCR_DTR = 1;
   private static final int MCR_RTS = 2;
   private static final int MCR_ALL = 3;
   private static final int CONTROL_WRITE_DTR = 256;
   private static final int CONTROL_WRITE_RTS = 512;
   private UsbEndpoint mReadEndpoint;
   private UsbEndpoint mWriteEndpoint;

   public Cp2102SerialDriver(UsbDevice device, UsbDeviceConnection connection) {
      super(device, connection);
   }

   private int setConfigSingle(int request, int value) {
      return this.mConnection.controlTransfer(65, request, value, 0, (byte[])null, 0, 5000);
   }

   public void open() throws IOException {
      boolean opened = false;

      try {
         for(int i = 0; i < this.mDevice.getInterfaceCount(); ++i) {
            UsbInterface usbIface = this.mDevice.getInterface(i);
            if (this.mConnection.claimInterface(usbIface, true)) {
               Log.d(TAG, "claimInterface " + i + " SUCCESS");
            } else {
               Log.d(TAG, "claimInterface " + i + " FAIL");
            }
         }

         UsbInterface dataIface = this.mDevice.getInterface(this.mDevice.getInterfaceCount() - 1);

         for(int i = 0; i < dataIface.getEndpointCount(); ++i) {
            UsbEndpoint ep = dataIface.getEndpoint(i);
            if (ep.getType() == 2) {
               if (ep.getDirection() == 128) {
                  this.mReadEndpoint = ep;
               } else {
                  this.mWriteEndpoint = ep;
               }
            }
         }

         this.setConfigSingle(0, 1);
         this.setConfigSingle(7, 771);
         this.setConfigSingle(1, 384);
         opened = true;
      } finally {
         if (!opened) {
            this.close();
         }

      }
   }

   public void close() throws IOException {
      this.setConfigSingle(0, 0);
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

         Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
      }

      return offset;
   }

   private void setBaudRate(int baudRate) throws IOException {
      byte[] data = new byte[]{(byte)(baudRate & 255), (byte)(baudRate >> 8 & 255), (byte)(baudRate >> 16 & 255), (byte)(baudRate >> 24 & 255)};
      int ret = this.mConnection.controlTransfer(65, 30, 0, 0, data, 4, 5000);
      if (ret < 0) {
         throw new IOException("Error setting baud rate.");
      }
   }

   public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {
      this.setBaudRate(baudRate);
      int configDataBits = 0;
      switch(dataBits) {
      case 5:
         configDataBits = configDataBits | 1280;
         break;
      case 6:
         configDataBits = configDataBits | 1536;
         break;
      case 7:
         configDataBits = configDataBits | 1792;
         break;
      case 8:
         configDataBits = configDataBits | 2048;
         break;
      default:
         configDataBits = configDataBits | 2048;
      }

      this.setConfigSingle(3, configDataBits);
      int configParityBits = 0;
      switch(parity) {
      case 1:
         configParityBits |= 16;
         break;
      case 2:
         configParityBits |= 32;
      }

      this.setConfigSingle(3, configParityBits);
      int configStopBits = 0;
      switch(stopBits) {
      case 1:
         configStopBits |= 0;
         break;
      case 2:
         configStopBits |= 2;
      }

      this.setConfigSingle(3, configStopBits);
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
      return true;
   }

   public void setDTR(boolean value) throws IOException {
   }

   public boolean getRI() throws IOException {
      return false;
   }

   public boolean getRTS() throws IOException {
      return true;
   }

   public boolean purgeHwBuffers(boolean purgeReadBuffers, boolean purgeWriteBuffers) throws IOException {
      int value = (purgeReadBuffers ? 10 : 0) | (purgeWriteBuffers ? 5 : 0);
      if (value != 0) {
         this.setConfigSingle(18, value);
      }

      return true;
   }

   public void setRTS(boolean value) throws IOException {
   }

   public static Map<Integer, int[]> getSupportedDevices() {
      Map<Integer, int[]> supportedDevices = new LinkedHashMap();
      supportedDevices.put(4292, new int[]{60000});
      return supportedDevices;
   }
}

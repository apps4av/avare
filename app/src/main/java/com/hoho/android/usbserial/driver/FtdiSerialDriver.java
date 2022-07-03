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

package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class FtdiSerialDriver extends CommonUsbSerialDriver {
   public static final int USB_TYPE_STANDARD = 0;
   public static final int USB_TYPE_CLASS = 0;
   public static final int USB_TYPE_VENDOR = 0;
   public static final int USB_TYPE_RESERVED = 0;
   public static final int USB_RECIP_DEVICE = 0;
   public static final int USB_RECIP_INTERFACE = 1;
   public static final int USB_RECIP_ENDPOINT = 2;
   public static final int USB_RECIP_OTHER = 3;
   public static final int USB_ENDPOINT_IN = 128;
   public static final int USB_ENDPOINT_OUT = 0;
   public static final int USB_WRITE_TIMEOUT_MILLIS = 5000;
   public static final int USB_READ_TIMEOUT_MILLIS = 5000;
   private static final int SIO_RESET_REQUEST = 0;
   private static final int SIO_MODEM_CTRL_REQUEST = 1;
   private static final int SIO_SET_FLOW_CTRL_REQUEST = 2;
   private static final int SIO_SET_BAUD_RATE_REQUEST = 3;
   private static final int SIO_SET_DATA_REQUEST = 4;
   private static final int SIO_RESET_SIO = 0;
   private static final int SIO_RESET_PURGE_RX = 1;
   private static final int SIO_RESET_PURGE_TX = 2;
   public static final int FTDI_DEVICE_OUT_REQTYPE = 64;
   public static final int FTDI_DEVICE_IN_REQTYPE = 192;
   private static final int MODEM_STATUS_HEADER_LENGTH = 2;
   private final String TAG = FtdiSerialDriver.class.getSimpleName();
   private FtdiSerialDriver.DeviceType mType = null;
   private int mInterface = 0;
   private int mMaxPacketSize = 64;
   private static final boolean ENABLE_ASYNC_READS = false;

   private final int filterStatusBytes(byte[] src, byte[] dest, int totalBytesRead, int maxPacketSize) {
      int packetsCount = totalBytesRead / maxPacketSize + 1;

      for(int packetIdx = 0; packetIdx < packetsCount; ++packetIdx) {
         int count = packetIdx == packetsCount - 1 ? totalBytesRead % maxPacketSize - 2 : maxPacketSize - 2;
         if (count > 0) {
            System.arraycopy(src, packetIdx * maxPacketSize + 2, dest, packetIdx * (maxPacketSize - 2), count);
         }
      }

      return totalBytesRead - packetsCount * 2;
   }

   public FtdiSerialDriver(UsbDevice usbDevice, UsbDeviceConnection usbConnection) {
      super(usbDevice, usbConnection);
   }

   public void reset() throws IOException {
      int result = this.mConnection.controlTransfer(64, 0, 0, 0, (byte[])null, 0, 5000);
      if (result != 0) {
         throw new IOException("Reset failed: result=" + result);
      } else {
         this.mType = FtdiSerialDriver.DeviceType.TYPE_R;
      }
   }

   public void open() throws IOException {
      boolean opened = false;

      try {
         for(int i = 0; i < this.mDevice.getInterfaceCount(); ++i) {
            if (!this.mConnection.claimInterface(this.mDevice.getInterface(i), true)) {
               throw new IOException("Error claiming interface " + i);
            }

            Log.d(this.TAG, "claimInterface " + i + " SUCCESS");
         }

         this.reset();
         opened = true;
      } finally {
         if (!opened) {
            this.close();
         }

      }
   }

   public void close() {
      this.mConnection.close();
   }

   public int read(byte[] dest, int timeoutMillis) throws IOException {
      UsbEndpoint endpoint = this.mDevice.getInterface(0).getEndpoint(0);
      synchronized(this.mReadBufferLock) {
         int readAmt = Math.min(dest.length, this.mReadBuffer.length);
         int totalBytesRead = this.mConnection.bulkTransfer(endpoint, this.mReadBuffer, readAmt, timeoutMillis);
         if (totalBytesRead < 2) {
            throw new IOException("Expected at least 2 bytes");
         } else {
            return this.filterStatusBytes(this.mReadBuffer, dest, totalBytesRead, endpoint.getMaxPacketSize());
         }
      }
   }

   public int write(byte[] src, int timeoutMillis) throws IOException {
      UsbEndpoint endpoint = this.mDevice.getInterface(0).getEndpoint(1);

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

            amtWritten = this.mConnection.bulkTransfer(endpoint, writeBuffer, writeLength, timeoutMillis);
         }

         if (amtWritten <= 0) {
            throw new IOException("Error writing " + writeLength + " bytes at offset " + offset + " length=" + src.length);
         }

         Log.d(this.TAG, "Wrote amtWritten=" + amtWritten + " attempted=" + writeLength);
      }

      return offset;
   }

   private int setBaudRate(int baudRate) throws IOException {
      long[] vals = this.convertBaudrate(baudRate);
      long actualBaudrate = vals[0];
      long index = vals[1];
      long value = vals[2];
      int result = this.mConnection.controlTransfer(64, 3, (int)value, (int)index, (byte[])null, 0, 5000);
      if (result != 0) {
         throw new IOException("Setting baudrate failed: result=" + result);
      } else {
         return (int)actualBaudrate;
      }
   }

   public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {
      this.setBaudRate(baudRate);
      int config;
      switch(parity) {
      case 0:
         config = dataBits | 0;
         break;
      case 1:
         config = dataBits | 256;
         break;
      case 2:
         config = dataBits | 512;
         break;
      case 3:
         config = dataBits | 768;
         break;
      case 4:
         config = dataBits | 1024;
         break;
      default:
         throw new IllegalArgumentException("Unknown parity value: " + parity);
      }

      switch(stopBits) {
      case 1:
         config |= 0;
         break;
      case 2:
         config |= 4096;
         break;
      case 3:
         config |= 2048;
         break;
      default:
         throw new IllegalArgumentException("Unknown stopBits value: " + stopBits);
      }

      int result = this.mConnection.controlTransfer(64, 4, config, 0, (byte[])null, 0, 5000);
      if (result != 0) {
         throw new IOException("Setting parameters failed: result=" + result);
      }
   }

   private long[] convertBaudrate(int baudrate) {
      int divisor = 24000000 / baudrate;
      int bestDivisor = 0;
      int bestBaud = 0;
      int bestBaudDiff = 0;
      int[] fracCode = new int[]{0, 3, 2, 4, 1, 5, 6, 7};

      for(int i = 0; i < 2; ++i) {
         int tryDivisor = divisor + i;
         if (tryDivisor <= 8) {
            tryDivisor = 8;
         } else if (this.mType != FtdiSerialDriver.DeviceType.TYPE_AM && tryDivisor < 12) {
            tryDivisor = 12;
         } else if (divisor < 16) {
            tryDivisor = 16;
         } else if (this.mType != FtdiSerialDriver.DeviceType.TYPE_AM && tryDivisor > 131071) {
            tryDivisor = 131071;
         }

         int baudEstimate = (24000000 + tryDivisor / 2) / tryDivisor;
         int baudDiff;
         if (baudEstimate < baudrate) {
            baudDiff = baudrate - baudEstimate;
         } else {
            baudDiff = baudEstimate - baudrate;
         }

         if (i == 0 || baudDiff < bestBaudDiff) {
            bestDivisor = tryDivisor;
            bestBaud = baudEstimate;
            bestBaudDiff = baudDiff;
            if (baudDiff == 0) {
               break;
            }
         }
      }

      long encodedDivisor = (long)(bestDivisor >> 3 | fracCode[bestDivisor & 7] << 14);
      if (encodedDivisor == 1L) {
         encodedDivisor = 0L;
      } else if (encodedDivisor == 16385L) {
         encodedDivisor = 1L;
      }

      long value = encodedDivisor & 65535L;
      long index;
      if (this.mType != FtdiSerialDriver.DeviceType.TYPE_2232C && this.mType != FtdiSerialDriver.DeviceType.TYPE_2232H && this.mType != FtdiSerialDriver.DeviceType.TYPE_4232H) {
         index = encodedDivisor >> 16 & 65535L;
      } else {
         index = encodedDivisor >> 8 & 65535L;
         index &= 65280L;
         index |= 0L;
      }

      return new long[]{(long)bestBaud, index, value};
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
      return false;
   }

   public void setDTR(boolean value) throws IOException {
   }

   public boolean getRI() throws IOException {
      return false;
   }

   public boolean getRTS() throws IOException {
      return false;
   }

   public void setRTS(boolean value) throws IOException {
   }

   public boolean purgeHwBuffers(boolean purgeReadBuffers, boolean purgeWriteBuffers) throws IOException {
      int result;
      if (purgeReadBuffers) {
         result = this.mConnection.controlTransfer(64, 0, 1, 0, (byte[])null, 0, 5000);
         if (result != 0) {
            throw new IOException("Flushing RX failed: result=" + result);
         }
      }

      if (purgeWriteBuffers) {
         result = this.mConnection.controlTransfer(64, 0, 2, 0, (byte[])null, 0, 5000);
         if (result != 0) {
            throw new IOException("Flushing RX failed: result=" + result);
         }
      }

      return true;
   }

   public static Map<Integer, int[]> getSupportedDevices() {
      Map<Integer, int[]> supportedDevices = new LinkedHashMap();
      supportedDevices.put(1027, new int[]{24577, 24597});
      return supportedDevices;
   }

   private static enum DeviceType {
      TYPE_BM,
      TYPE_AM,
      TYPE_2232C,
      TYPE_R,
      TYPE_2232H,
      TYPE_4232H;
   }
}

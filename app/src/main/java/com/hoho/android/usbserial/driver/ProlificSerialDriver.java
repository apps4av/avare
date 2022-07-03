/* This library is free software; you can redistribute it and/or
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

/*
 * Ported to usb-serial-for-android
 * by Felix Haedicke <felixhaedicke@web.de>
 *
 * Based on the pyprolific driver written
 * by Emmanuel Blot <emmanuel.blot@free.fr>
 * See https://github.com/eblot/pyftdi
 */

package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProlificSerialDriver extends CommonUsbSerialDriver {
   private static final int USB_READ_TIMEOUT_MILLIS = 1000;
   private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;
   private static final int USB_RECIP_INTERFACE = 1;
   private static final int PROLIFIC_VENDOR_READ_REQUEST = 1;
   private static final int PROLIFIC_VENDOR_WRITE_REQUEST = 1;
   private static final int PROLIFIC_VENDOR_OUT_REQTYPE = 64;
   private static final int PROLIFIC_VENDOR_IN_REQTYPE = 192;
   private static final int PROLIFIC_CTRL_OUT_REQTYPE = 33;
   private static final int WRITE_ENDPOINT = 2;
   private static final int READ_ENDPOINT = 131;
   private static final int INTERRUPT_ENDPOINT = 129;
   private static final int FLUSH_RX_REQUEST = 8;
   private static final int FLUSH_TX_REQUEST = 9;
   private static final int SET_LINE_REQUEST = 32;
   private static final int SET_CONTROL_REQUEST = 34;
   private static final int CONTROL_DTR = 1;
   private static final int CONTROL_RTS = 2;
   private static final int STATUS_FLAG_CD = 1;
   private static final int STATUS_FLAG_DSR = 2;
   private static final int STATUS_FLAG_RI = 8;
   private static final int STATUS_FLAG_CTS = 128;
   private static final int STATUS_BUFFER_SIZE = 10;
   private static final int STATUS_BYTE_IDX = 8;
   private static final int DEVICE_TYPE_HX = 0;
   private static final int DEVICE_TYPE_0 = 1;
   private static final int DEVICE_TYPE_1 = 2;
   private int mDeviceType = 0;
   private UsbEndpoint mReadEndpoint;
   private UsbEndpoint mWriteEndpoint;
   private UsbEndpoint mInterruptEndpoint;
   private int mControlLinesValue = 0;
   private int mBaudRate = -1;
   private int mDataBits = -1;
   private int mStopBits = -1;
   private int mParity = -1;
   private int mStatus = 0;
   private volatile Thread mReadStatusThread = null;
   private final Object mReadStatusThreadLock = new Object();
   boolean mStopReadStatusThread = false;
   private IOException mReadStatusException = null;
   private final String TAG = ProlificSerialDriver.class.getSimpleName();

   private final byte[] inControlTransfer(int requestType, int request, int value, int index, int length) throws IOException {
      byte[] buffer = new byte[length];
      int result = this.mConnection.controlTransfer(requestType, request, value, index, buffer, length, 1000);
      if (result != length) {
         throw new IOException(String.format("ControlTransfer with value 0x%x failed: %d", value, result));
      } else {
         return buffer;
      }
   }

   private final void outControlTransfer(int requestType, int request, int value, int index, byte[] data) throws IOException {
      int length = data == null ? 0 : data.length;
      int result = this.mConnection.controlTransfer(requestType, request, value, index, data, length, 5000);
      if (result != length) {
         throw new IOException(String.format("ControlTransfer with value 0x%x failed: %d", value, result));
      }
   }

   private final byte[] vendorIn(int value, int index, int length) throws IOException {
      return this.inControlTransfer(192, 1, value, index, length);
   }

   private final void vendorOut(int value, int index, byte[] data) throws IOException {
      this.outControlTransfer(64, 1, value, index, data);
   }

   private final void ctrlOut(int request, int value, int index, byte[] data) throws IOException {
      this.outControlTransfer(33, request, value, index, data);
   }

   private void doBlackMagic() throws IOException {
      this.vendorIn(33924, 0, 1);
      this.vendorOut(1028, 0, (byte[])null);
      this.vendorIn(33924, 0, 1);
      this.vendorIn(33667, 0, 1);
      this.vendorIn(33924, 0, 1);
      this.vendorOut(1028, 1, (byte[])null);
      this.vendorIn(33924, 0, 1);
      this.vendorIn(33667, 0, 1);
      this.vendorOut(0, 1, (byte[])null);
      this.vendorOut(1, 0, (byte[])null);
      this.vendorOut(2, this.mDeviceType == 0 ? 68 : 36, (byte[])null);
   }

   private void resetDevice() throws IOException {
      this.purgeHwBuffers(true, true);
   }

   private void setControlLines(int newControlLinesValue) throws IOException {
      this.ctrlOut(34, newControlLinesValue, 0, (byte[])null);
      this.mControlLinesValue = newControlLinesValue;
   }

   private final void readStatusThreadFunction() {
      while(true) {
         try {
            if (!this.mStopReadStatusThread) {
               byte[] buffer = new byte[10];
               int readBytesCount = this.mConnection.bulkTransfer(this.mInterruptEndpoint, buffer, 10, 500);
               if (readBytesCount <= 0) {
                  continue;
               }

               if (readBytesCount == 10) {
                  this.mStatus = buffer[8] & 255;
                  continue;
               }

               throw new IOException(String.format("Invalid CTS / DSR / CD / RI status buffer received, expected %d bytes, but received %d", 10, readBytesCount));
            }
         } catch (IOException var3) {
            this.mReadStatusException = var3;
         }

         return;
      }
   }

   private final int getStatus() throws IOException {
      if (this.mReadStatusThread == null && this.mReadStatusException == null) {
         synchronized(this.mReadStatusThreadLock) {
            if (this.mReadStatusThread == null) {
               byte[] buffer = new byte[10];
               int readBytes = this.mConnection.bulkTransfer(this.mInterruptEndpoint, buffer, 10, 100);
               if (readBytes != 10) {
                  Log.w(this.TAG, "Could not read initial CTS / DSR / CD / RI status");
               } else {
                  this.mStatus = buffer[8] & 255;
               }

               this.mReadStatusThread = new Thread(new Runnable() {
                  public void run() {
                     ProlificSerialDriver.this.readStatusThreadFunction();
                  }
               });
               this.mReadStatusThread.setDaemon(true);
               this.mReadStatusThread.start();
            }
         }
      }

      IOException readStatusException = this.mReadStatusException;
      if (this.mReadStatusException != null) {
         this.mReadStatusException = null;
         throw readStatusException;
      } else {
         return this.mStatus;
      }
   }

   private final boolean testStatusFlag(int flag) throws IOException {
      return (this.getStatus() & flag) == flag;
   }

   public ProlificSerialDriver(UsbDevice device, UsbDeviceConnection connection) {
      super(device, connection);
   }

   public void open() throws IOException {
      UsbInterface usbInterface = this.mDevice.getInterface(0);
      if (!this.mConnection.claimInterface(usbInterface, true)) {
         throw new IOException("Error claiming Prolific interface 0");
      } else {
         boolean openSuccessful = false;

         try {
            for(int i = 0; i < usbInterface.getEndpointCount(); ++i) {
               UsbEndpoint currentEndpoint = usbInterface.getEndpoint(i);
               switch(currentEndpoint.getAddress()) {
               case 2:
                  this.mWriteEndpoint = currentEndpoint;
                  break;
               case 129:
                  this.mInterruptEndpoint = currentEndpoint;
                  break;
               case 131:
                  this.mReadEndpoint = currentEndpoint;
               }
            }

            if (this.mDevice.getDeviceClass() == 2) {
               this.mDeviceType = 1;
            } else {
               try {
                  Method getRawDescriptorsMethod = this.mConnection.getClass().getMethod("getRawDescriptors");
                  byte[] rawDescriptors = (byte[])getRawDescriptorsMethod.invoke(this.mConnection);
                  byte maxPacketSize0 = rawDescriptors[7];
                  if (maxPacketSize0 == 64) {
                     this.mDeviceType = 0;
                  } else if (this.mDevice.getDeviceClass() != 0 && this.mDevice.getDeviceClass() != 255) {
                     Log.w(this.TAG, "Could not detect PL2303 subtype, Assuming that it is a HX device");
                     this.mDeviceType = 0;
                  } else {
                     this.mDeviceType = 2;
                  }
               } catch (NoSuchMethodException var14) {
                  Log.w(this.TAG, "Method UsbDeviceConnection.getRawDescriptors, required for PL2303 subtype detection, not available! Assuming that it is a HX device");
                  this.mDeviceType = 0;
               } catch (Exception var15) {
                  Log.e(this.TAG, "An unexpected exception occured while trying to detect PL2303 subtype", var15);
               }
            }

            this.setControlLines(this.mControlLinesValue);
            this.resetDevice();
            this.doBlackMagic();
            openSuccessful = true;
         } finally {
            if (!openSuccessful) {
               try {
                  this.mConnection.releaseInterface(usbInterface);
               } catch (Exception var13) {
               }
            }

         }

      }
   }

   public void close() throws IOException {
      try {
         this.mStopReadStatusThread = true;
         synchronized(this.mReadStatusThreadLock) {
            if (this.mReadStatusThread != null) {
               try {
                  this.mReadStatusThread.join();
               } catch (Exception var7) {
                  Log.w(this.TAG, "An error occured while waiting for status read thread", var7);
               }
            }
         }

         this.resetDevice();
      } finally {
         this.mConnection.releaseInterface(this.mDevice.getInterface(0));
      }

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
      }

      return offset;
   }

   public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {
      if (this.mBaudRate != baudRate || this.mDataBits != dataBits || this.mStopBits != stopBits || this.mParity != parity) {
         byte[] lineRequestData = new byte[]{(byte)(baudRate & 255), (byte)(baudRate >> 8 & 255), (byte)(baudRate >> 16 & 255), (byte)(baudRate >> 24 & 255), 0, 0, 0};
         switch(stopBits) {
         case 1:
            lineRequestData[4] = 0;
            break;
         case 2:
            lineRequestData[4] = 2;
            break;
         case 3:
            lineRequestData[4] = 1;
            break;
         default:
            throw new IllegalArgumentException("Unknown stopBits value: " + stopBits);
         }

         switch(parity) {
         case 0:
            lineRequestData[5] = 0;
            break;
         case 1:
            lineRequestData[5] = 1;
            break;
         case 2:
            lineRequestData[5] = 2;
            break;
         case 3:
            lineRequestData[5] = 3;
            break;
         case 4:
            lineRequestData[5] = 4;
            break;
         default:
            throw new IllegalArgumentException("Unknown parity value: " + parity);
         }

         lineRequestData[6] = (byte)dataBits;
         this.ctrlOut(32, 0, 0, lineRequestData);
         this.resetDevice();
         this.mBaudRate = baudRate;
         this.mDataBits = dataBits;
         this.mStopBits = stopBits;
         this.mParity = parity;
      }
   }

   public boolean getCD() throws IOException {
      return this.testStatusFlag(1);
   }

   public boolean getCTS() throws IOException {
      return this.testStatusFlag(128);
   }

   public boolean getDSR() throws IOException {
      return this.testStatusFlag(2);
   }

   public boolean getDTR() throws IOException {
      return (this.mControlLinesValue & 1) == 1;
   }

   public void setDTR(boolean value) throws IOException {
      int newControlLinesValue;
      if (value) {
         newControlLinesValue = this.mControlLinesValue | 1;
      } else {
         newControlLinesValue = this.mControlLinesValue & -2;
      }

      this.setControlLines(newControlLinesValue);
   }

   public boolean getRI() throws IOException {
      return this.testStatusFlag(8);
   }

   public boolean getRTS() throws IOException {
      return (this.mControlLinesValue & 2) == 2;
   }

   public void setRTS(boolean value) throws IOException {
      int newControlLinesValue;
      if (value) {
         newControlLinesValue = this.mControlLinesValue | 2;
      } else {
         newControlLinesValue = this.mControlLinesValue & -3;
      }

      this.setControlLines(newControlLinesValue);
   }

   public boolean purgeHwBuffers(boolean purgeReadBuffers, boolean purgeWriteBuffers) throws IOException {
      if (purgeReadBuffers) {
         this.vendorOut(8, 0, (byte[])null);
      }

      if (purgeWriteBuffers) {
         this.vendorOut(9, 0, (byte[])null);
      }

      return true;
   }

   public static Map<Integer, int[]> getSupportedDevices() {
      Map<Integer, int[]> supportedDevices = new LinkedHashMap();
      supportedDevices.put(1659, new int[]{8963});
      return supportedDevices;
   }
}

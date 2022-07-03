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
import android.hardware.usb.UsbManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper class which finds compatible {@link UsbDevice}s and creates
 * {@link UsbSerialDriver} instances.
 *
 * <p/>
 * You don't need a Prober to use the rest of the library: it is perfectly
 * acceptable to instantiate driver instances manually. The Prober simply
 * provides convenience functions.
 *
 * <p/>
 * For most drivers, the corresponding {@link #probe(UsbManager, UsbDevice)}
 * method will either return an empty list (device unknown / unsupported) or a
 * singleton list. However, multi-port drivers may return multiple instances.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public enum UsbSerialProber {
   FTDI_SERIAL {
      public List<UsbSerialDriver> probe(UsbManager manager, UsbDevice usbDevice) {
         if (!UsbSerialProber.testIfSupported(usbDevice, FtdiSerialDriver.getSupportedDevices())) {
            return Collections.emptyList();
         } else {
            UsbDeviceConnection connection = manager.openDevice(usbDevice);
            if (connection == null) {
               return Collections.emptyList();
            } else {
               UsbSerialDriver driver = new FtdiSerialDriver(usbDevice, connection);
               return Collections.singletonList(driver);
            }
         }
      }
   },
   CDC_ACM_SERIAL {
      public List<UsbSerialDriver> probe(UsbManager manager, UsbDevice usbDevice) {
         if (!UsbSerialProber.testIfSupported(usbDevice, CdcAcmSerialDriver.getSupportedDevices())) {
            return Collections.emptyList();
         } else {
            UsbDeviceConnection connection = manager.openDevice(usbDevice);
            if (connection == null) {
               return Collections.emptyList();
            } else {
               UsbSerialDriver driver = new CdcAcmSerialDriver(usbDevice, connection);
               return Collections.singletonList(driver);
            }
         }
      }
   },
   SILAB_SERIAL {
      public List<UsbSerialDriver> probe(UsbManager manager, UsbDevice usbDevice) {
         if (!UsbSerialProber.testIfSupported(usbDevice, Cp2102SerialDriver.getSupportedDevices())) {
            return Collections.emptyList();
         } else {
            UsbDeviceConnection connection = manager.openDevice(usbDevice);
            if (connection == null) {
               return Collections.emptyList();
            } else {
               UsbSerialDriver driver = new Cp2102SerialDriver(usbDevice, connection);
               return Collections.singletonList(driver);
            }
         }
      }
   },
   PROLIFIC_SERIAL {
      public List<UsbSerialDriver> probe(UsbManager manager, UsbDevice usbDevice) {
         if (!UsbSerialProber.testIfSupported(usbDevice, ProlificSerialDriver.getSupportedDevices())) {
            return Collections.emptyList();
         } else {
            UsbDeviceConnection connection = manager.openDevice(usbDevice);
            if (connection == null) {
               return Collections.emptyList();
            } else {
               UsbSerialDriver driver = new ProlificSerialDriver(usbDevice, connection);
               return Collections.singletonList(driver);
            }
         }
      }
   };

   private UsbSerialProber() {
   }

   protected abstract List<UsbSerialDriver> probe(UsbManager var1, UsbDevice var2);

   public static UsbSerialDriver findFirstDevice(UsbManager usbManager) {
      Iterator var2 = usbManager.getDeviceList().values().iterator();

      while(var2.hasNext()) {
         UsbDevice usbDevice = (UsbDevice)var2.next();
         UsbSerialProber[] var6;
         int var5 = (var6 = values()).length;

         for(int var4 = 0; var4 < var5; ++var4) {
            UsbSerialProber prober = var6[var4];
            List<UsbSerialDriver> probedDevices = prober.probe(usbManager, usbDevice);
            if (!probedDevices.isEmpty()) {
               return (UsbSerialDriver)probedDevices.get(0);
            }
         }
      }

      return null;
   }

   public static List<UsbSerialDriver> findAllDevices(UsbManager usbManager) {
      List<UsbSerialDriver> result = new ArrayList();
      Iterator var3 = usbManager.getDeviceList().values().iterator();

      while(var3.hasNext()) {
         UsbDevice usbDevice = (UsbDevice)var3.next();
         result.addAll(probeSingleDevice(usbManager, usbDevice));
      }

      return result;
   }

   public static List<UsbSerialDriver> probeSingleDevice(UsbManager usbManager, UsbDevice usbDevice) {
      List<UsbSerialDriver> result = new ArrayList();
      UsbSerialProber[] var6;
      int var5 = (var6 = values()).length;

      for(int var4 = 0; var4 < var5; ++var4) {
         UsbSerialProber prober = var6[var4];
         List<UsbSerialDriver> probedDevices = prober.probe(usbManager, usbDevice);
         result.addAll(probedDevices);
      }

      return result;
   }

   /** @deprecated */
   @Deprecated
   public static UsbSerialDriver acquire(UsbManager usbManager) {
      return findFirstDevice(usbManager);
   }

   /** @deprecated */
   @Deprecated
   public static UsbSerialDriver acquire(UsbManager usbManager, UsbDevice usbDevice) {
      List<UsbSerialDriver> probedDevices = probeSingleDevice(usbManager, usbDevice);
      return !probedDevices.isEmpty() ? (UsbSerialDriver)probedDevices.get(0) : null;
   }

   private static boolean testIfSupported(UsbDevice usbDevice, Map<Integer, int[]> supportedDevices) {
      int[] supportedProducts = (int[])supportedDevices.get(usbDevice.getVendorId());
      if (supportedProducts == null) {
         return false;
      } else {
         int productId = usbDevice.getProductId();
         int[] var7 = supportedProducts;
         int var6 = supportedProducts.length;

         for(int var5 = 0; var5 < var6; ++var5) {
            int supportedProductId = var7[var5];
            if (productId == supportedProductId) {
               return true;
            }
         }

         return false;
      }
   }

   // $FF: synthetic method
   UsbSerialProber(UsbSerialProber var3) {
      this();
   }
}

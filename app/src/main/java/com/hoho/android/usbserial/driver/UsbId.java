/* Copyright 2012 Google Inc.
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

/**
 * Registry of USB vendor/product ID constants.
 *
 * Culled from various sources; see
 * <a href="http://www.linux-usb.org/usb.ids">usb.ids</a> for one listing.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public final class UsbId {
   public static final int VENDOR_FTDI = 1027;
   public static final int FTDI_FT232R = 24577;
   public static final int FTDI_FT231X = 24597;
   public static final int VENDOR_ATMEL = 1003;
   public static final int ATMEL_LUFA_CDC_DEMO_APP = 8260;
   public static final int VENDOR_ARDUINO = 9025;
   public static final int ARDUINO_UNO = 1;
   public static final int ARDUINO_MEGA_2560 = 16;
   public static final int ARDUINO_SERIAL_ADAPTER = 59;
   public static final int ARDUINO_MEGA_ADK = 63;
   public static final int ARDUINO_MEGA_2560_R3 = 66;
   public static final int ARDUINO_UNO_R3 = 67;
   public static final int ARDUINO_MEGA_ADK_R3 = 68;
   public static final int ARDUINO_SERIAL_ADAPTER_R3 = 68;
   public static final int ARDUINO_LEONARDO = 32822;
   public static final int VENDOR_VAN_OOIJEN_TECH = 5824;
   public static final int VAN_OOIJEN_TECH_TEENSYDUINO_SERIAL = 1155;
   public static final int VENDOR_LEAFLABS = 7855;
   public static final int LEAFLABS_MAPLE = 4;
   public static final int VENDOR_SILAB = 4292;
   public static final int SILAB_CP2102 = 60000;
   public static final int VENDOR_PROLIFIC = 1659;
   public static final int PROLIFIC_PL2303 = 8963;

   private UsbId() {
      throw new IllegalAccessError("Non-instantiable class.");
   }
}

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hoho.android.usbserial.util;

/**
 * Clone of Android's HexDump class, for use in debugging. Cosmetic changes
 * only.
 */
public class HexDump {
   private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

   public static String dumpHexString(byte[] array) {
      return dumpHexString(array, 0, array.length);
   }

   public static String dumpHexString(byte[] array, int offset, int length) {
      StringBuilder result = new StringBuilder();
      byte[] line = new byte[16];
      int lineIndex = 0;
      result.append("\n0x");
      result.append(toHexString(offset));

      int i;
      for(i = offset; i < offset + length; ++i) {
         if (lineIndex == 16) {
            result.append(" ");
            i = 0;

            while(true) {
               if (i >= 16) {
                  result.append("\n0x");
                  result.append(toHexString(i));
                  lineIndex = 0;
                  break;
               }

               if (line[i] > 32 && line[i] < 126) {
                  result.append(new String(line, i, 1));
               } else {
                  result.append(".");
               }

               ++i;
            }
         }

         byte b = array[i];
         result.append(" ");
         result.append(HEX_DIGITS[b >>> 4 & 15]);
         result.append(HEX_DIGITS[b & 15]);
         line[lineIndex++] = b;
      }

      if (lineIndex != 16) {
         i = (16 - lineIndex) * 3;
         ++i;

         for(i = 0; i < i; ++i) {
            result.append(" ");
         }

         for(i = 0; i < lineIndex; ++i) {
            if (line[i] > 32 && line[i] < 126) {
               result.append(new String(line, i, 1));
            } else {
               result.append(".");
            }
         }
      }

      return result.toString();
   }

   public static String toHexString(byte b) {
      return toHexString(toByteArray(b));
   }

   public static String toHexString(byte[] array) {
      return toHexString(array, 0, array.length);
   }

   public static String toHexString(byte[] array, int offset, int length) {
      char[] buf = new char[length * 2];
      int bufIndex = 0;

      for(int i = offset; i < offset + length; ++i) {
         byte b = array[i];
         buf[bufIndex++] = HEX_DIGITS[b >>> 4 & 15];
         buf[bufIndex++] = HEX_DIGITS[b & 15];
      }

      return new String(buf);
   }

   public static String toHexString(int i) {
      return toHexString(toByteArray(i));
   }

   public static String toHexString(short i) {
      return toHexString(toByteArray(i));
   }

   public static byte[] toByteArray(byte b) {
      byte[] array = new byte[]{b};
      return array;
   }

   public static byte[] toByteArray(int i) {
      byte[] array = new byte[]{(byte)(i >> 24 & 255), (byte)(i >> 16 & 255), (byte)(i >> 8 & 255), (byte)(i & 255)};
      return array;
   }

   public static byte[] toByteArray(short i) {
      byte[] array = new byte[]{(byte)(i >> 8 & 255), (byte)(i & 255)};
      return array;
   }

   private static int toByte(char c) {
      if (c >= '0' && c <= '9') {
         return c - 48;
      } else if (c >= 'A' && c <= 'F') {
         return c - 65 + 10;
      } else if (c >= 'a' && c <= 'f') {
         return c - 97 + 10;
      } else {
         throw new RuntimeException("Invalid hex char '" + c + "'");
      }
   }

   public static byte[] hexStringToByteArray(String hexString) {
      int length = hexString.length();
      byte[] buffer = new byte[length / 2];

      for(int i = 0; i < length; i += 2) {
         buffer[i / 2] = (byte)(toByte(hexString.charAt(i)) << 4 | toByte(hexString.charAt(i + 1)));
      }

      return buffer;
   }
}

package com.ds.avare.adsb;

import com.ds.avare.StorageService;
import com.ds.avare.adsb.gdl90.Crc;
import com.ds.avare.connections.BufferProcessor;
import com.ds.avare.gps.Gps;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Arrays;

public class AdsbMessageProcessingIntegrationTest {

    private static final byte[] testTrafficMessage =  new byte[] {
            (byte) 0x7E, (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x1D, (byte) 0x48, (byte) 0x97, (byte) 0xBB,
            (byte) 0xC8, (byte) 0x7A, (byte) 0x0A, (byte) 0x89, (byte) 0x88, (byte) 0x00, (byte) 0x0F, (byte) 0xFF, (byte) 0xCA, (byte) 0x01,
            (byte) 0x4E, (byte) 0x42, (byte) 0x4E, (byte) 0x44, (byte) 0x54, (byte) 0x33, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x8F,
            (byte) 0x92, (byte) 0x7E
    };
    private static final byte[] testOwnshipMessage = new byte[] {
            (byte) 0x7e, (byte) 0x0a, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1d, (byte) 0x49, (byte) 0x69, (byte) 0xbb,
            (byte) 0xc2, (byte) 0x90, (byte) 0x08, (byte) 0xc9, (byte) 0x88, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x01,
            (byte) 0x4e, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0xbe,
            (byte) 0xdf, (byte) 0x7e
    };

    @Mock
    TrafficCache trafficCache;
    @InjectMocks
    StorageService storageService;

    @Test
    public void storageServiceGetDataFromIO_ownshipMessage_trafficCacheUpdatedWithOwnshipInfo() throws JSONException {
        storageService = spy(StorageService.getInstance());
        trafficCache = mock(TrafficCache.class);
        when(storageService.getTrafficCache()).thenReturn(trafficCache);
        when(storageService.getGps()).thenReturn(mock(Gps.class));
        // new parameters (isairborne, vspeed) test #1
        final String jsonMsg = "{\"type\":\"ownship\",\"longitude\":-95.96248626708984,\"latitude\":41.184505462646484,\"isairborne\":true,\"speed\":0,\"vspeed\":192,\"bearing\":0,\"time\":1670140827383,\"altitude\":761,\"address\":0}";
        storageService.getDataFromIO(jsonMsg);
        verify(trafficCache).setOwnVertVelocity(192);
        verify(trafficCache).setOwnIsAirborne(true);
        // new parameters (isairborne, vspeed) test #2
        final String jsonMsg2 = "{\"type\":\"ownship\",\"longitude\":-95.96248626708984,\"latitude\":41.184505462646484,\"isairborne\":false,\"speed\":0,\"vspeed\":-256,\"bearing\":0,\"time\":1670140827383,\"altitude\":761,\"address\":0}";
        storageService.getDataFromIO(jsonMsg2);
        verify(trafficCache).setOwnVertVelocity(-256);
        verify(trafficCache).setOwnIsAirborne(false);
    }

    @Test
    public void storageServiceGetDataFromIO_trafficMessage_trafficObjectAddedToCache() {
        storageService = spy(StorageService.getInstance());
        trafficCache = mock(TrafficCache.class);
        when(storageService.getTrafficCache()).thenReturn(trafficCache);
        // new parameters (isairborne, vspeed) test #1
        final String jsonMsg = "{\"type\":\"traffic\",\"longitude\":-95.93000030517578,\"latitude\":41.179996490478516,\"isairborne\":true,\"speed\":0,\"vspeed\":-64,\"bearing\":284.0625,\"altitude\":3200,\"callsign\":\"NBNDT3  \",\"address\":3,\"time\":1670140829332}";
        storageService.getDataFromIO(jsonMsg);
        verify(trafficCache).putTraffic(eq("NBNDT3  "), eq(3), eq(true), eq(41.179996f),
                eq(-95.93f), eq(3200), eq(284.0625f), eq(0), eq(-64), anyLong());
        // new parameters (isairborne, vspeed) test #2
        final String jsonMsg2 = "{\"type\":\"traffic\",\"longitude\":-95.93000030517578,\"latitude\":41.179996490478516,\"isairborne\":false,\"speed\":0,\"vspeed\":-512,\"bearing\":284.0625,\"altitude\":3200,\"callsign\":\"NBNDT3  \",\"address\":3,\"time\":1670140829332}";
        storageService.getDataFromIO(jsonMsg2);
        verify(trafficCache).putTraffic(eq("NBNDT3  "), eq(3), eq(false), eq(41.179996f),
                eq(-95.93f), eq(3200), eq(284.0625f), eq(0), eq(-512), anyLong());
    }

    //TODO: This could super-set the message parsing tests above
    @Test
    public void bufferProcessor_trafficMessageDecode_properJsonMessageReturned() throws JSONException {
        final BufferProcessor bp = new BufferProcessor();
        try (MockedStatic<Logger> mockStaticLogger = mockStatic(Logger.class); MockedStatic<Crc> mockStaticCrc = mockStatic(Crc.class)) {
            mockStaticCrc.when(() -> Crc.checkCrc(any(), anyInt(), anyInt())).thenReturn(true);
            JSONObject jsonResult;
            // TODO: Validate lat, lon, etc.
            // new parameters (isairborne, vspeed) test #1
            bp.put(testTrafficMessage, testTrafficMessage.length);
            jsonResult = new JSONObject(bp.decode(mock(Preferences.class)).get(0));
            assertEquals("traffic", jsonResult.getString("type"));
            assertEquals(true, jsonResult.getBoolean("isairborne"));
            assertEquals(-64, jsonResult.getInt("vspeed"));
            // new parameters  (isairborne, vspeed) test #2
            bp.put(setAirborneFlagInCopy(testTrafficMessage, false), testTrafficMessage.length);
            jsonResult = new JSONObject(bp.decode(mock(Preferences.class)).get(0));
            assertEquals(false, jsonResult.getBoolean("isairborne"));
        }
    }

    @Test
    public void bufferProcessor_ownshipMessageDecode_properJsonMessageReturned() throws JSONException {
        final BufferProcessor bp = new BufferProcessor();
        try (MockedStatic<Logger> mockStaticLogger = mockStatic(Logger.class); MockedStatic<Crc> mockStaticCrc = mockStatic(Crc.class)) {
            mockStaticCrc.when(() -> Crc.checkCrc(any(), anyInt(), anyInt())).thenReturn(true);
            JSONObject jsonResult;
            // TODO: Validate lat, lon, etc.
            // new parameters (isairborne, vspeed) test #1
            bp.put(testOwnshipMessage, testTrafficMessage.length);
            jsonResult = new JSONObject(bp.decode(mock(Preferences.class)).get(0));
            assertEquals("ownship", jsonResult.getString("type"));
            assertEquals(true, jsonResult.getBoolean("isairborne"));
            assertEquals(192, jsonResult.getInt("vspeed"));
            // new parameters  (isairborne, vspeed) test #2
            bp.put(setAirborneFlagInCopy(testOwnshipMessage, false), testTrafficMessage.length);
            jsonResult = new JSONObject(bp.decode(mock(Preferences.class)).get(0));
            assertEquals(false, jsonResult.getBoolean("isairborne"));
        }
    }

    private static final byte[] setAirborneFlagInCopy(final byte[] msg, boolean isAirborne) {
        final byte[] newMsg = Arrays.copyOf(msg, msg.length);
        newMsg[11+2] = (byte) (isAirborne ? (newMsg[11+2] | (1 << 3)) : (newMsg[11+2] & ~(1 << 3) ));
        return newMsg;
    }

}

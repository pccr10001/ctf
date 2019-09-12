package li.power.app.antitank;

import android.os.ParcelUuid;

import java.util.UUID;

public class Constants {
    public static final int Manufacturer_ID = 0x14;
    public static final byte[] Manufacturer_Data = {0x64, 0x53, 0x6D, 0x61, 0x72, 0x74, 0x78, 0x2D, 0x41, 0x72, 0x6D, 0x6F, 0x72, 0x2D, 0x46, 0x39, 0x32, 0x31, 0x46, 0x34};
    public static final String ACTION_ADD_LOG = "li.power.app.antitank.action.ADD_LOG";
    public static final String ACTION_STOP_ADV = "li.power.app.antitank.action.STOP_ADV";

    public static final String TAG_ADD_LOG = "LOG";

    public static final ParcelUuid TANK_SERVICE_UUID = ParcelUuid.fromString("0000ff12-0000-1000-8000-00805f9b34fb");
    public static final UUID TANK_WRITE_UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    public static final UUID TANK_READ_UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");

    public static final String[][] TANK_COMMANDS = {
            {"CC0800EE", "CC080402190359EE"},             // Step 1
            {"CC07020101EE", "CC0706010200000000EE"},     //Step 2
            {"CC07020201EE", "CC0706020200000000EE"},     //Step 3
            {"CC07020301EE", "CC0706030200000000EE"},     //Step 4
            {"CC03020204EE", ""},     //Step 5-1
            {"CC0302011FEE", ""},     //Step 5-2

            {"CC0400EE", "CC04020028EE"},     //Battery
            {"CC01026464EE", ""},   //Brake
            {"CC01022828EE", ""},   //Backward
            {"CC0102A0A0EE", ""},   //Forward
            {"CC01029D2BEE", ""},   //Turn Right
            {"CC01022B9DEE", ""},   //Turn Left
    };


}

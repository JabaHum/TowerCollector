/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package info.zamojski.soft.towercollector.utils;

import info.zamojski.soft.towercollector.collector.validators.CellIdentityValidator;
import info.zamojski.soft.towercollector.collector.validators.CellLocationValidator;
import info.zamojski.soft.towercollector.model.Measurement;
import timber.log.Timber;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;


public class MobileUtils {


    public static int[] getMccMncPair(String operatorCode) {
        // mcc and mnc is concatenated in the networkOperatorString (the first 3 chars is the mcc and the last 2 is the mnc)
        if (!TextUtils.isEmpty(operatorCode) && operatorCode.length() > 3) {
            try {
                int mcc = Integer.parseInt(operatorCode.substring(0, 3));
                int mnc = Integer.parseInt(operatorCode.substring(3));
                return new int[]{mcc, mnc};
            } catch (NumberFormatException ex) {
                Timber.e(ex, "getMccMncPair(): Cannot parse network operator codes: %s", operatorCode);
            }
        }
        return null;
    }

    public static boolean isApi17VersionCompatible() {
        boolean result = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
        Timber.d("isApi17VersionCompatible(): Result = %s", result);
        return result;
    }

    public static boolean isApi26VersionCompatible() {
        boolean result = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        Timber.d("isApi26VersionCompatible(): Result = %s", result);
        return result;
    }

    public static boolean isApi17FullyCompatible(Context context) {
        return (isApi17VersionCompatible() && isApi17CellInfoAvailable(context));
    }

    public static boolean isCellInfoAvailable(Context context) {
        if (isApi17VersionCompatible() && isApi17CellInfoAvailable(context))
            return true;
        return isApi1CellInfoAvailable(context);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static boolean isApi17CellInfoAvailable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> cells;
        try {
            cells = telephonyManager.getAllCellInfo();
        } catch (SecurityException ex) {
            Timber.d(ex, "isApi17CellInfoAvailable(): Result = coarse location permission is denied");
            return false;
        }
        if (cells == null || cells.size() == 0) {
            Timber.d("isApi17CellInfoAvailable(): Result = no cell info");
            return false;
        }
        CellIdentityValidator validator = new CellIdentityValidator();
        for (CellInfo cell : cells) {
            if (validator.isValid(cell)) {
                Timber.d("isApi17CellInfoAvailable(): Result = true");
                return true;
            }
        }
        Timber.d("isApi17CellInfoAvailable(): Result = false");
        return false;
    }

    private static boolean isApi1CellInfoAvailable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        CellLocation cell;
        try {
            cell = telephonyManager.getCellLocation();
        } catch (SecurityException ex) {
            Timber.d(ex, "isApi1CellInfoAvailable(): Result = coarse location permission is denied");
            return false;
        }
        if (cell == null) {
            Timber.d("isApi1CellInfoAvailable(): Result = no cell location");
            return false;
        }
        int mcc = Measurement.UNKNOWN_CID;
        int mnc = Measurement.UNKNOWN_CID;
        if (cell instanceof GsmCellLocation) {
            String operatorCode = telephonyManager.getNetworkOperator();
            int[] mccMncPair = getMccMncPair(operatorCode);
            if (mccMncPair == null) {
                Timber.d("isApi1CellInfoAvailable(): Result = no operator code");
                return false;
            }
            mcc = mccMncPair[0];
            mnc = mccMncPair[1];
        }
        CellLocationValidator validator = new CellLocationValidator();
        boolean result = validator.isValid(cell, mcc, mnc);
        Timber.d("isApi1CellInfoAvailable(): Result = %s", result);
        return result;
    }
}

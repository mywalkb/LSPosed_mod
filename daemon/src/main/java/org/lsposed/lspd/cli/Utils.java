package org.lsposed.lspd.cli;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;

import org.lsposed.lspd.ICLIService;
import org.lsposed.lspd.models.Application;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Utils {

    public static final String CMDNAME = "cli";

    public enum ERRCODES {
        NOERROR,
        USAGE,
        EMPTY_SCOPE,
        ENABLE_DISABLE,
        SET_SCOPE,
        LS_SCOPE,
        NO_DAEMON,
        REMOTE_ERROR
    }

    private static HashMap<String,PackageInfo> packagesMap;

    private static void initPackagesMap(ICLIService managerService) throws RemoteException {
        var packages =
                managerService.getInstalledPackagesFromAllUsers(PackageManager.GET_META_DATA | PackageManager.MATCH_UNINSTALLED_PACKAGES, true).getList();
        packagesMap = new HashMap<>();
        for (var packageInfo: packages) {
            packagesMap.put(packageInfo.packageName, packageInfo);
        }
    }

    public static boolean validPackageNameAndUserId(ICLIService managerService, String packageName, int userId) throws RemoteException {
        if (packagesMap == null) {
            initPackagesMap(managerService);
        }

        return packagesMap.containsKey(packageName)
                && (Objects.requireNonNull(packagesMap.get(packageName)).applicationInfo.uid) / 100000 == userId;
    }

    public static boolean checkPackageInScope(String sPackageName, List<Application> lstScope) {
        for (var app : lstScope) {
            if (app.packageName.equals(sPackageName)) {
                return true;
            }
        }
        return false;
    }
}

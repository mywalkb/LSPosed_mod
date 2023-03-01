/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import android.util.Log;

import org.lsposed.daemon.BuildConfig;
import static org.lsposed.lspd.service.ServiceManager.TAG;

import io.github.xposed.xposedservice.IXposedService;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class LSPLegacyModuleService extends IXposedService.Stub {

    final private String name;

    public LSPLegacyModuleService(String name) {
        this.name = name;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public int getXposedApiVersion() {
        return BuildConfig.API_CODE;
    }

    @Override
    public int getXposedVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public String getXposedVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getApi() {
        return ConfigManager.getInstance().getApi();
    }

    @Override
    public boolean setModuleScope(List<io.github.xposed.xposedservice.models.Application> scope) throws RemoteException {
        var pid = getCallingPid();
        var uid = getCallingUid();
        ConfigManager config = ConfigManager.getInstance();
        String packageName = config.getModule(uid).packageName;
        List<String> listDeny = config.getDenyListPackages();

        if (packageName == null) throw new RemoteException("failed to retried package name");
        if (!Arrays.stream(config.enabledModules()).anyMatch(x -> x.equals(packageName))) throw new RemoteException("module not enabled");

        Log.d(TAG, "setModuleScope calling uid: " + uid + " calling pid: " + pid + " package " + packageName);

        List<org.lsposed.lspd.models.Application> newlist = new ArrayList();
        for (var app : scope) {
            if (listDeny.contains(app.packageName)) {
                throw new RemoteException(app.packageName + " is in magisk deny list");
            }
            var app2 = new org.lsposed.lspd.models.Application();
            app2.packageName = app.packageName;
            app2.userId = app.userId;
            newlist.add(app2);
        }
        return config.setModuleScope(packageName, newlist);
    }

    @Override
    public List<io.github.xposed.xposedservice.models.Application> getModuleScope() throws RemoteException {
        var pid = getCallingPid();
        var uid = getCallingUid();
        String packageName = ConfigManager.getInstance().getModule(uid).packageName;

        if (packageName == null) throw new RemoteException("failed to retried package name");
        if (!Arrays.stream(ConfigManager.getInstance().enabledModules()).anyMatch(x -> x.equals(packageName))) throw new RemoteException("module not enabled");

        Log.d(TAG, "getModuleScope calling uid: " + uid + " calling pid: " + pid + " package " + packageName);

        var list = ConfigManager.getInstance().getModuleScope(packageName);
        List<io.github.xposed.xposedservice.models.Application> newlist = new ArrayList();
        for (var app : list) {
            var app2 = new io.github.xposed.xposedservice.models.Application();
            app2.packageName = app.packageName;
            app2.userId = app.userId;
            newlist.add(app2);
        }
        if (list == null) return null;
        else return newlist;
    }

    @Override
    public List<String> getDenyListPackages() throws RemoteException {
        return ConfigManager.getInstance().getDenyListPackages();
    }
}

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

import static org.lsposed.lspd.service.PackageService.PER_USER_RANGE;

import android.content.AttributionSource;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import org.lsposed.daemon.BuildConfig;
import org.lsposed.lspd.models.Module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.service.IXposedScopeCallback;
import io.github.libxposed.service.IXposedService;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class LSPModuleService extends IXposedService.Stub {

    private final static String TAG = "LSPosedModuleService";

    private final static Set<Integer> uidSet = ConcurrentHashMap.newKeySet();
    private final static Map<Module, LSPModuleService> serviceMap = Collections.synchronizedMap(new WeakHashMap<>());

    public final static String FILES_DIR = "files";

    private final @NonNull
    Module loadedModule;

    static void uidClear() {
        uidSet.clear();
    }

    static void uidStarts(int uid) {
        if (!uidSet.contains(uid)) {
            uidSet.add(uid);
            var module = ConfigManager.getInstance().getModule(uid);
            if (module != null && module.file != null && !module.file.legacy) {
                var service = serviceMap.computeIfAbsent(module, LSPModuleService::new);
                service.sendBinder(uid);
            }
        }
    }

    static void uidGone(int uid) {
        uidSet.remove(uid);
    }

    private void sendBinder(int uid) {
        var name = loadedModule.packageName;
        try {
            int userId = uid / PackageService.PER_USER_RANGE;
            var authority = name + AUTHORITY_SUFFIX;
            var provider = ActivityManagerService.getContentProvider(authority, userId);
            if (provider == null) {
                Log.d(TAG, "no service provider for " + name);
                return;
            }
            var extra = new Bundle();
            extra.putBinder("binder", asBinder());
            Bundle reply = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                reply = provider.call(new AttributionSource.Builder(1000).setPackageName("android").build(), authority, SEND_BINDER, null, extra);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                reply = provider.call("android", null, authority, SEND_BINDER, null, extra);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                reply = provider.call("android", authority, SEND_BINDER, null, extra);
            }
            if (reply != null) {
                Log.d(TAG, "sent module binder to " + name);
            } else {
                Log.w(TAG, "failed to send module binder to " + name);
            }
        } catch (Throwable e) {
            Log.w(TAG, "failed to send module binder for uid " + uid, e);
        }
    }

    LSPModuleService(@NonNull Module module) {
        loadedModule = module;
    }

    private int ensureModule() throws RemoteException {
        var appId = Binder.getCallingUid() % PER_USER_RANGE;
        if (loadedModule.appId != appId) {
            throw new RemoteException("Module " + loadedModule.packageName + " is not for uid " + Binder.getCallingUid());
        }
        return Binder.getCallingUid() / PER_USER_RANGE;
    }

    @Override
    public int getAPIVersion() throws RemoteException {
        ensureModule();
        return API;
    }

    @Override
    public String getFrameworkName() throws RemoteException {
        ensureModule();
        return "LSPosed";
    }

    @Override
    public String getFrameworkVersion() throws RemoteException {
        ensureModule();
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public long getFrameworkVersionCode() throws RemoteException {
        ensureModule();
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public int getFrameworkPrivilege() throws RemoteException {
        ensureModule();
        return IXposedService.FRAMEWORK_PRIVILEGE_ROOT;
    }

    @Override
    public Bundle featuredMethod(String name, Bundle args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getScope() throws RemoteException {
        ensureModule();
        ArrayList<String> res = new ArrayList<>();
        var scope = ConfigManager.getInstance().getModuleScope(loadedModule.packageName);
        if (scope == null) return res;
        for (var s : scope) {
            res.add(s.packageName);
        }
        return res;
    }

    @Override
    public void requestScope(String packageName, IXposedScopeCallback callback) throws RemoteException {
        var userId = ensureModule();
        if (ConfigManager.getInstance().scopeRequestBlocked(loadedModule.packageName)) {
            callback.onScopeRequestDenied(packageName);
        } else {
            LSPNotificationManager.requestModuleScope(loadedModule.packageName, userId, packageName, callback);
            callback.onScopeRequestPrompted(packageName);
        }
    }

    @Override
    public String removeScope(String packageName) throws RemoteException {
        var userId = ensureModule();
        try {
            if (!ConfigManager.getInstance().removeModuleScope(loadedModule.packageName, packageName, userId)) {
                return "Invalid request";
            }
            return null;
        } catch (Throwable e) {
            return e.getMessage();
        }
    }

    @Override
    public Bundle requestRemotePreferences(String group) throws RemoteException {
        var userId = ensureModule();
        var bundle = new Bundle();
        bundle.putSerializable("map", ConfigManager.getInstance().getModulePrefs(loadedModule.packageName, userId, group));
        return bundle;
    }

    @Override
    public void updateRemotePreferences(String group, Bundle diff) throws RemoteException {
        var userId = ensureModule();
        Map<String, Object> values = new ArrayMap<>();
        if (diff.containsKey("delete")) {
            var deletes = diff.getStringArrayList("delete");
            for (var key : deletes) {
                values.put(key, null);
            }
        }
        if (diff.containsKey("put")) {
            try {
                var puts = (Map<?, ?>) diff.getSerializable("put");
                for (var entry : puts.entrySet()) {
                    values.put((String) entry.getKey(), entry.getValue());
                }
            } catch (Throwable e) {
                Log.e(TAG, "updateRemotePreferences: ", e);
            }
        }
        try {
            ConfigManager.getInstance().updateModulePrefs(loadedModule.packageName, userId, group, values);
            ((LSPInjectedModuleService) loadedModule.service).onUpdateRemotePreferences(group, diff);
        } catch (Throwable e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void deleteRemotePreferences(String group) throws RemoteException {
        var userId = ensureModule();
        ConfigManager.getInstance().deleteModulePrefs(loadedModule.packageName, userId, group);
    }

    @Override
    public ParcelFileDescriptor openRemoteFile(String path, int mode) throws RemoteException {
        var userId = ensureModule();
        ConfigFileManager.ensureModuleFilePath(path);
        try {
            var dir = ConfigFileManager.resolveModuleDir(loadedModule.packageName, FILES_DIR, userId, Binder.getCallingUid());
            return ParcelFileDescriptor.open(dir.resolve(path).toFile(), mode);
        } catch (Throwable e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public boolean deleteRemoteFile(String path) throws RemoteException {
        var userId = ensureModule();
        ConfigFileManager.ensureModuleFilePath(path);
        try {
            var dir = ConfigFileManager.resolveModuleDir(loadedModule.packageName, FILES_DIR, userId, Binder.getCallingUid());
            return dir.resolve(path).toFile().delete();
        } catch (Throwable e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public String[] listRemoteFiles() throws RemoteException {
        var userId = ensureModule();
        try {
            var dir = ConfigFileManager.resolveModuleDir(loadedModule.packageName, FILES_DIR, userId, Binder.getCallingUid());
            var files = dir.toFile().list();
            return files == null ? new String[0] : files;
        } catch (Throwable e) {
            throw new RemoteException(e.getMessage());
        }
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
        String packageName = config.getModule(uid);
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
        String packageName = ConfigManager.getInstance().getModule(uid);

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

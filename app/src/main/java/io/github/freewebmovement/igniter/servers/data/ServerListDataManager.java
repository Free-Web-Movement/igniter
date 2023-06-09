package io.github.freewebmovement.igniter.servers.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.persistence.ServerList;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;

public class ServerListDataManager implements ServerListDataSource {

    public ServerListDataManager(String configFilePath) {
    }

    @Override
    public List<TrojanConfig> loadServerConfigList() {
        return new ArrayList<>(ServerList.readDatabase(IgniterApplication.getApplication()));
    }

    @Override
    public void deleteServerConfig(TrojanConfig config) {
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            if (trojanConfigs.get(i).getRemoteAddr().equals(config.getRemoteAddr())) {
                trojanConfigs.remove(i);
                replaceServerConfigs(trojanConfigs);
                break;
            }
        }
    }

    @Override
    public void saveServerConfig(TrojanConfig config) {
        boolean configExisted = false;
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        Log.wtf("SERVER_LIST_DATA_MANAGER", "" + trojanConfigs.size());
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            String[] remoteAddress = new String[2];
            int[] remotePort = new int[2];

            remoteAddress[0] = trojanConfigs.get(i).getRemoteAddr();
            remoteAddress[1] = config.getRemoteAddr();
            remotePort[0] = trojanConfigs.get(i).getRemotePort();
            remotePort[1] = config.getRemotePort();

            if (remoteAddress[0].equals(remoteAddress[1]) && remotePort[0] == remotePort[1]) {
                trojanConfigs.set(i, config);
                configExisted = true;
                break;
            }
        }
        if (!configExisted) {
            trojanConfigs.add(config);
        }
        replaceServerConfigs(trojanConfigs);
    }

    @Override
    public void replaceServerConfigs(List<TrojanConfig> list) {
//        ServerList.write(list, mConfigFilePath);
//        Storage.print(mConfigFilePath, ServerList.CONFIG_LIST_TAG);
    }
}

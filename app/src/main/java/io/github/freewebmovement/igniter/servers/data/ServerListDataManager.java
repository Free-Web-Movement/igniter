package io.github.freewebmovement.igniter.servers.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.github.freewebmovement.igniter.persistence.ServerList;
import io.github.freewebmovement.igniter.persistence.Storage;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;

public class ServerListDataManager implements ServerListDataSource {
    private final String mConfigFilePath;

    public ServerListDataManager(String configFilePath) {
        mConfigFilePath = configFilePath;
    }

    @Override
    public List<TrojanConfig> loadServerConfigList() {
        return new ArrayList<>(ServerList.read(mConfigFilePath));
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
        boolean configRemoteAddrExists = false;
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        Log.wtf("SERVER_LIST_DATA_MANAGER", "" + trojanConfigs.size());
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            if (trojanConfigs.get(i).getRemoteAddr().equals(config.getRemoteAddr())) {
                trojanConfigs.set(i, config);
                configRemoteAddrExists = true;
                break;
            }
        }
        if (!configRemoteAddrExists) {
            trojanConfigs.add(config);
        }
        replaceServerConfigs(trojanConfigs);
    }

    @Override
    public void replaceServerConfigs(List<TrojanConfig> list) {
        ServerList.write(list, mConfigFilePath);
        Storage.print(mConfigFilePath, ServerList.CONFIG_LIST_TAG);
    }
}

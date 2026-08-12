package io.github.freewebmovement.igniter.activities.servers.data;

import java.util.ArrayList;
import java.util.List;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase;

public class ServerListDataManager implements ServerListDataSource {

    public ServerListDataManager() {
    }

    @Override
    public List<TrojanConfig> loadServerConfigList() {
        List<TrojanConfig> list = new ArrayList<>(AccessDatabase.readServers(IgniterApplication.getApplication()));
        TrojanConfig current = IgniterApplication.getApplication().trojanConfig;
        if (current != null && current.getRemoteAddr() != null && !current.getRemoteAddr().isEmpty()
                && !containsServer(list, current)) {
            list.add(0, current);
            AccessDatabase.insertServerIfMissing(IgniterApplication.getApplication(), current);
        }
        return list;
    }

    private boolean containsServer(List<TrojanConfig> list, TrojanConfig config) {
        for (TrojanConfig c : list) {
            if (c.getRemoteAddr() != null && c.getRemoteAddr().equals(config.getRemoteAddr())
                    && c.getRemotePort() == config.getRemotePort()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteServerConfig(TrojanConfig config) {
        AccessDatabase.deleteServer(IgniterApplication.getApplication(),
                config.getRemoteAddr(),
                config.getRemotePort());
    }

    @Override
    public void saveServerConfig(TrojanConfig config) {
        boolean configExisted = false;
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
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
        AccessDatabase.replaceServers(IgniterApplication.getApplication(), list);
    }
}

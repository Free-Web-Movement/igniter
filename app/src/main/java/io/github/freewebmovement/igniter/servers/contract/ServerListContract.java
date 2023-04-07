package io.github.freewebmovement.igniter.servers.contract;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import io.github.freewebmovement.igniter.persistence.TrojanConfig;
import io.github.freewebmovement.igniter.common.mvp.BasePresenter;
import io.github.freewebmovement.igniter.common.mvp.BaseView;

public interface ServerListContract {
    interface Presenter extends BasePresenter {
        void addServerConfig(String trojanUrl);
        void handleServerSelection(TrojanConfig config);
        void deleteServerConfig(TrojanConfig config, int pos);
        void gotoScanQRCode();
        void displayImportFileDescription();
        void hideImportFileDescription();
        void importConfigFromFile();
        void parseConfigsInFileStream(Context context, Uri fileUri);
    }

    interface View extends BaseView<Presenter> {
        void showAddTrojanConfigSuccess();
        void showQRCodeScanError(String scanContent);
        void selectServerConfig(TrojanConfig config);
        void showServerConfigList(List<TrojanConfig> configs);
        void removeServerConfig(TrojanConfig config, int pos);
        void gotoScanQRCode();
        void showImportFileDescription();
        void dismissImportFileDescription();
        void openFileChooser();
    }
}

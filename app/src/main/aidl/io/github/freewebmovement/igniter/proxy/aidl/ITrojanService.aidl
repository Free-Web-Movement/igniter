// ITrojanService.aidl
package io.github.freewebmovement.igniter.proxy.aidl;
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanServiceCallback;
// Declare any non-default types here with import statements

interface ITrojanService {
    int getState();
    void testConnection(String testUrl);
    void showDevelopInfoInLogcat();
    oneway void registerCallback(in ITrojanServiceCallback callback);
    oneway void unregisterCallback(in ITrojanServiceCallback callback);
}

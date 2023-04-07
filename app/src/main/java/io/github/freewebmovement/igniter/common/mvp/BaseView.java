package io.github.freewebmovement.igniter.common.mvp;

public interface BaseView<T extends BasePresenter> {
    void setPresenter(T presenter);
}

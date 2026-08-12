package io.github.freewebmovement.igniter.common.mvp

interface BaseView<T : BasePresenter> {
    fun setPresenter(presenter: T)
}

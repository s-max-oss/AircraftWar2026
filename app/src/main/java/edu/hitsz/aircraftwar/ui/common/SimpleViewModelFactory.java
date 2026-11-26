package edu.hitsz.aircraftwar.ui.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.function.Supplier;

/**
 * 用 lambda 创建 ViewModel 的小工厂：ViewModel 需要 Repository 这类构造参数，
 * 又不想每个页面都写一个 Factory 类。
 */
public final class SimpleViewModelFactory implements ViewModelProvider.Factory {

    private final Supplier<? extends ViewModel> creator;

    public SimpleViewModelFactory(Supplier<? extends ViewModel> creator) {
        this.creator = creator;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        ViewModel viewModel = creator.get();
        if (!modelClass.isInstance(viewModel)) {
            throw new IllegalArgumentException(
                    "Factory 创建了 " + viewModel.getClass().getName() + "，但请求的是 "
                            + modelClass.getName());
        }
        return (T) viewModel;
    }
}

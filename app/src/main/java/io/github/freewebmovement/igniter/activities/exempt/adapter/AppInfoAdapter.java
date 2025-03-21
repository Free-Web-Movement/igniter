package io.github.freewebmovement.igniter.activities.exempt.adapter;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.media.Image;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.persistence.data.AppInfo;

public class AppInfoAdapter extends RecyclerView.Adapter<AppInfoAdapter.ViewHolder> {
    private final List<AppInfo> mData = new ArrayList<>();
    private OnItemOperationListener mOnItemOperationListener;
    private final Rect mIconBound = new Rect();

    public AppInfoAdapter() {
        super();
        final int size = Resources.getSystem().getDimensionPixelSize(android.R.dimen.app_icon_size);
        mIconBound.right = size;
        mIconBound.bottom = size;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_app_info, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        if (i != RecyclerView.NO_POSITION) {
            viewHolder.bindData(mData.get(i));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshData(List<AppInfo> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setOnItemOperationListener(OnItemOperationListener onItemOperationListener) {
        mOnItemOperationListener = onItemOperationListener;
    }

    public interface OnItemOperationListener {
        void onToggle(boolean enabled, AppInfo appInfo, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {
        private final LinearLayout mAppNameLayout;
        private final ImageView mAppIV;
        private final TextView mNameTv;
        private final TextView mPackageNameTv;
        private final SwitchCompat mExemptSwitch;
        private AppInfo mCurrentInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mAppNameLayout = itemView.findViewById(R.id.appName);
            mNameTv = itemView.findViewById(R.id.appNameTv);
            mPackageNameTv = itemView.findViewById(R.id.appPackageNameTv);
            mAppIV = itemView.findViewById(R.id.appIV);
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mNameTv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mPackageNameTv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            mExemptSwitch = itemView.findViewById(R.id.appExemptSwitch);


        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mOnItemOperationListener != null) {
                mOnItemOperationListener.onToggle(isChecked, mCurrentInfo, getBindingAdapterPosition());
            }
        }

        void bindData(AppInfo appInfo) {
            mCurrentInfo = appInfo;
            mNameTv.setText(appInfo.getAppName());
            mPackageNameTv.setText(appInfo.getPackageName());
//            appInfo.getIcon().setBounds(mIconBound);
            mAppIV.setImageDrawable(appInfo.getIcon());


            mAppNameLayout.setOnLongClickListener(v -> {
                Toast.makeText(itemView.getContext(), appInfo.getAppName() + "\n" + appInfo.getPackageName(), Toast.LENGTH_LONG).show();
                return false;
            });

//            mAppNameLayout.set

//            mNameTv.setCompoundDrawables(appInfo.getIcon(), null, null, null);
//            mPackageNameTv.setCompoundDrawables(appInfo.getIcon(), null, null, null);
            mExemptSwitch.setOnCheckedChangeListener(null);
            mExemptSwitch.setChecked(appInfo.getEnabled());
            mExemptSwitch.setOnCheckedChangeListener(this);
        }
    }
}

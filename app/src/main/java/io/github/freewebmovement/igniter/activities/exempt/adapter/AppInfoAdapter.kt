package io.github.freewebmovement.igniter.activities.exempt.adapter

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.persistence.data.AppInfo

class AppInfoAdapter : RecyclerView.Adapter<AppInfoAdapter.ViewHolder>() {
    private val mData: MutableList<AppInfo> = ArrayList()
    private var mOnItemOperationListener: OnItemOperationListener? = null
    private val mIconBound = Rect()

    init {
        val size = Resources.getSystem().getDimensionPixelSize(android.R.dimen.app_icon_size)
        mIconBound.right = size
        mIconBound.bottom = size
    }

    @NonNull
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_app_info, viewGroup, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        if (i != RecyclerView.NO_POSITION) {
            viewHolder.bindData(mData[i])
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshData(data: List<AppInfo>) {
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setOnItemOperationListener(onItemOperationListener: OnItemOperationListener) {
        mOnItemOperationListener = onItemOperationListener
    }

    interface OnItemOperationListener {
        fun onToggle(enabled: Boolean, appInfo: AppInfo, position: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), CompoundButton.OnCheckedChangeListener {
        private val mAppNameLayout: LinearLayout = itemView.findViewById(R.id.appName)
        private val mAppIV: ImageView = itemView.findViewById(R.id.appIV)
        private val mNameTv: TextView = itemView.findViewById(R.id.appNameTv)
        private val mPackageNameTv: TextView = itemView.findViewById(R.id.appPackageNameTv)
        private val mExemptSwitch: SwitchCompat = itemView.findViewById(R.id.appExemptSwitch)
        private var mCurrentInfo: AppInfo? = null

        init {
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mNameTv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mPackageNameTv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
        }

        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            mOnItemOperationListener?.onToggle(isChecked, mCurrentInfo!!, bindingAdapterPosition)
        }

        fun bindData(appInfo: AppInfo) {
            mCurrentInfo = appInfo
            mNameTv.text = appInfo.appName
            mPackageNameTv.text = appInfo.packageName
//            appInfo.getIcon().setBounds(mIconBound)
            mAppIV.setImageDrawable(appInfo.icon)

            mAppNameLayout.setOnLongClickListener {
                Toast.makeText(itemView.context, appInfo.appName.toString() + "\n" + appInfo.packageName, Toast.LENGTH_LONG).show()
                false
            }

//            mAppNameLayout.set

//            mNameTv.setCompoundDrawables(appInfo.getIcon(), null, null, null);
//            mPackageNameTv.setCompoundDrawables(appInfo.getIcon(), null, null, null);
            mExemptSwitch.setOnCheckedChangeListener(null)
            mExemptSwitch.isChecked = appInfo.enabled
            mExemptSwitch.setOnCheckedChangeListener(this)
        }
    }
}

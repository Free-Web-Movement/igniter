package io.github.freewebmovement.igniter.activities.servers.fragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.persistence.TrojanConfig

class ServerListAdapter(context: Context, data: List<TrojanConfig>) : RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {
    private val mInflater: LayoutInflater
    private val mData: MutableList<TrojanConfig>
    private var mOnItemClickListener: OnItemClickListener? = null

    init {
        mData = ArrayList(data)
        mInflater = LayoutInflater.from(context)
    }

    @NonNull
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val vh = ViewHolder(mInflater.inflate(R.layout.item_server, viewGroup, false))
        vh.bindListener(mOnItemClickListener)
        return vh
    }

    fun replaceData(data: List<TrojanConfig>) {
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }

    fun removeItemOnPosition(pos: Int) {
        mData.removeAt(pos)
        notifyItemRemoved(pos)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bindData(mData[i])
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mOnItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemSelected(config: TrojanConfig, pos: Int)

        fun onItemDelete(config: TrojanConfig, pos: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var mConfig: TrojanConfig? = null
        private val mRemoteAddrTv: TextView = itemView.findViewById(R.id.serverAddrTv)
        private val mCurrentBadge: View = itemView.findViewById(R.id.currentServerBadge)
        private var itemClickListener: OnItemClickListener? = null

        init {
            itemView.setOnClickListener {
                itemClickListener?.onItemSelected(mConfig!!, bindingAdapterPosition)
            }
            itemView.findViewById<View>(R.id.deleteServerBtn).setOnClickListener {
                itemClickListener?.onItemDelete(mConfig!!, bindingAdapterPosition)
            }
        }

        fun bindData(config: TrojanConfig) {
            mConfig = config
            mRemoteAddrTv.text = config.getRemoteAddr()
            val current = IgniterApplication.getApplication().trojanConfig
            val isCurrent = config.getRemoteAddr() == current.getRemoteAddr() &&
                    config.getRemotePort() == current.getRemotePort()
            mCurrentBadge.visibility = if (isCurrent) View.VISIBLE else View.GONE
        }

        fun bindListener(listener: OnItemClickListener?) {
            itemClickListener = listener
        }
    }
}

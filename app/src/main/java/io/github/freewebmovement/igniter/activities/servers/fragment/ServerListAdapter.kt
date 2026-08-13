package io.github.freewebmovement.igniter.activities.servers.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.databinding.ItemServerBinding
import io.github.freewebmovement.igniter.persistence.database.Server

class ServerListAdapter(data: List<Server>) : RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(IgniterApplication.getApplication())
    private val mData: MutableList<Server> = ArrayList(data)
    private val mPingText: MutableMap<String, String> = HashMap()
    private val mPingColor: MutableMap<String, Int> = HashMap()
    private var mOnItemClickListener: OnItemClickListener? = null
    private var mRunning = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServerBinding.inflate(mInflater, parent, false)
        val vh = ViewHolder(binding)
        vh.bindListener(mOnItemClickListener)
        return vh
    }

    fun replaceData(data: List<Server>) {
        mData.clear()
        mData.addAll(data)
        mPingText.clear()
        mPingColor.clear()
        notifyDataSetChanged()
    }

    /** Updates the ping label of the row matching host:port. */
    fun updatePing(host: String, port: Int, text: String, color: Int) {
        val key = "$host:$port"
        mPingText[key] = text
        mPingColor[key] = color
        val pos = mData.indexOfFirst { it.hostname == host && it.port == port }
        if (pos >= 0) {
            notifyItemChanged(pos)
        }
    }

    /** Tracks whether the proxy is running so the row knows which server is active. */
    fun setRunning(running: Boolean) {
        if (mRunning != running) {
            mRunning = running
            notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(mData[position], mRunning)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mOnItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemSelected(server: Server, pos: Int)

        fun onItemPlay(server: Server, pos: Int)

        fun onItemStop(server: Server, pos: Int)

        fun onItemMore(server: Server, anchor: View, pos: Int)

        fun onItemDelete(server: Server, pos: Int)
    }

    inner class ViewHolder(private val binding: ItemServerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var mServer: Server? = null
        private var itemClickListener: OnItemClickListener? = null

        init {
            binding.root.setOnClickListener {
                itemClickListener?.onItemSelected(mServer!!, bindingAdapterPosition)
            }
            binding.playServerBtn.setOnClickListener {
                itemClickListener?.onItemPlay(mServer!!, bindingAdapterPosition)
            }
            binding.stopServerBtn.setOnClickListener {
                itemClickListener?.onItemStop(mServer!!, bindingAdapterPosition)
            }
            binding.moreServerBtn.setOnClickListener {
                itemClickListener?.onItemMore(
                    mServer!!, binding.moreServerBtn, bindingAdapterPosition)
            }
        }

        fun bindData(server: Server, running: Boolean) {
            mServer = server
            binding.serverAddrTv.text = server.hostname
            binding.serverUrlTv.text =
                "trojan://${server.password}@${server.hostname}:${server.port}"
            val key = "${server.hostname}:${server.port}"
            binding.serverPingTv.text = mPingText[key] ?: ""
            binding.serverPingTv.setTextColor(mPingColor[key] ?: 0xFF757575.toInt())
            val current = IgniterApplication.getApplication().trojanConfig
            val isCurrent = server.hostname == current.getRemoteAddr() &&
                    server.port == current.getRemotePort()
            binding.currentServerBadge.visibility = if (isCurrent) View.VISIBLE else View.GONE
            val isActive = running && isCurrent
            binding.playServerBtn.isEnabled = !isActive
            binding.stopServerBtn.isEnabled = isActive
            binding.playServerBtn.alpha = if (isActive) 0.4f else 1f
            binding.stopServerBtn.alpha = if (isActive) 1f else 0.4f
        }

        fun bindListener(listener: OnItemClickListener?) {
            itemClickListener = listener
        }
    }
}

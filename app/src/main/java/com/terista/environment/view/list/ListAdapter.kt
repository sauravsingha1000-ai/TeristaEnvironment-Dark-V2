package com.terista.environment.view.list

import android.view.View
import android.view.ViewGroup
import cbfg.rvadapter.RVHolder
import cbfg.rvadapter.RVHolderFactory
import com.terista.environment.R
import com.terista.environment.bean.InstalledAppBean
import com.terista.environment.databinding.ItemPackageBinding



class ListAdapter : RVHolderFactory() {

    override fun createViewHolder(parent: ViewGroup?, viewType: Int, item: Any): RVHolder<out Any> {
        return ListVH(inflate(R.layout.item_package,parent))
    }

    class ListVH(itemView:View) :RVHolder<InstalledAppBean>(itemView){

        val binding = ItemPackageBinding.bind(itemView)
        override fun setContent(item: InstalledAppBean, isSelected: Boolean, payload: Any?) {
            binding.icon.setImageDrawable(item.icon)
            binding.name.text = item.name
            binding.packageName.text = item.packageName
            binding.cornerLabel.visibility = if (item.isInstall) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
}
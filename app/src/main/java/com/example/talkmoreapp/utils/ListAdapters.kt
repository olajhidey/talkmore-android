package com.example.talkmoreapp.utils

import android.view.View
import com.example.talkmoreapp.R
import com.example.talkmoreapp.databinding.ListItemLayoutBinding
import com.example.talkmoreapp.model.UserResponse
import com.xwray.groupie.viewbinding.BindableItem

class ListAdapters(val content:UserResponse): BindableItem<ListItemLayoutBinding>() {

    override fun getLayout(): Int = R.layout.list_item_layout

    override fun bind(binding: ListItemLayoutBinding, position: Int) {
        binding.listItemUsername.text = content.username
        binding.listItemCallButton.setOnClickListener {
            // do something
        }
    }

    override fun initializeViewBinding(view: View): ListItemLayoutBinding {
        return ListItemLayoutBinding.bind(view)
    }
}
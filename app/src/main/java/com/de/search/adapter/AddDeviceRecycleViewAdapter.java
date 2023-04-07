package com.de.search.adapter;


import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.de.search.R;
import com.de.search.bean.DeviceBean;

import java.util.List;

//The adapter for the list, used to load the list

public class AddDeviceRecycleViewAdapter extends RecyclerView.Adapter<AddDeviceRecycleViewAdapter.MyHolder> {

    private final List<DeviceBean> mList;//Data source
    private final DeviceRecycleViewAdapterInterface myRecycleViewAdapterInterface;

    public AddDeviceRecycleViewAdapter(List<DeviceBean> list, DeviceRecycleViewAdapterInterface myRecycleViewAdapterInterface) {
        mList = list;
        this.myRecycleViewAdapterInterface = myRecycleViewAdapterInterface;
    }

    //Creates a ViewHolder and returns it, and all subsequent items in the layout are retrieved from the ViewHolder
    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Convert our custom item layout 'R.layout.item_one' to 'View'
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_device, parent, false);
        //Pass the 'view' to our custom ViewHolder
        MyHolder holder = new MyHolder(view);
        //Returns the MyHolder entity
        return holder;
    }

    //Bind data to the ViewHolder through the ViewHolder provided by the method
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(MyHolder holder, @SuppressLint("RecyclerView") int position) {
        DeviceBean deviceBean = mList.get(position);

        holder.textSort.setText(position + 1 + ".");
        holder.textName.setText("name：" + deviceBean.getName() + "");
        holder.textMac.setText("mac：" + deviceBean.getMac() + "");
        holder.textRssi.setText("rssi：" + deviceBean.getRssi() + "");


        holder.itemView.setOnLongClickListener(v -> {
            myRecycleViewAdapterInterface.onLongClick(position);
            return true;
        });

        holder.itemView.setOnClickListener(v -> myRecycleViewAdapterInterface.onClick(position));

    }

    //Gets the total number of data source bars
    @Override
    public int getItemCount() {
        return mList.size();
    }

    /**
     * A custom ViewHolder
     */
    static class MyHolder extends RecyclerView.ViewHolder {


        TextView textSort;
        TextView textName;
        TextView textMac;
        TextView textRssi;

        public MyHolder(View itemView) {
            super(itemView);
            textSort = itemView.findViewById(R.id.tv_sort);
            textName = itemView.findViewById(R.id.tv_name);
            textMac = itemView.findViewById(R.id.tv_mac);
            textRssi = itemView.findViewById(R.id.tv_rssi);

        }
    }

    public interface DeviceRecycleViewAdapterInterface{
        void onLongClick(int position);
        void onClick(int position);
    }
}

package com.de.search.adapter;


import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.de.search.R;
import com.de.search.bean.DeviceBean;

import java.util.ArrayList;
import java.util.List;

public class SelectDeviceRecycleViewAdapter extends RecyclerView.Adapter<SelectDeviceRecycleViewAdapter.MyHolder> {

    private final List<DeviceBean> mList;//Data source


    public SelectDeviceRecycleViewAdapter(List<DeviceBean> list) {
        mList = list;
    }

    //Creates a ViewHolder and returns it, and all subsequent items in the layout are retrieved from the ViewHolder
    @NonNull
    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Convert our custom item layout R.layout.item_one to View
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_device, parent, false);
        //Pass the view to our custom ViewHolder
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
        holder.textTime.setText("find time：" + deviceBean.getFindTime() + "");

        if (deviceBean.getMe() == 0){
            holder.textUser.setVisibility(View.VISIBLE);
            holder.textMessenger.setVisibility(View.VISIBLE);

            holder.textUser.setText("user：" + deviceBean.getUserName() + "");
            holder.textMessenger.setText("messenger：" + deviceBean.getMessengerName() + "");

        }else {
            holder.textUser.setVisibility(View.GONE);
            holder.textMessenger.setVisibility(View.GONE);
        }


        holder.checkBox.setChecked(deviceBean.isC());

        holder.checkBox.setOnCheckedChangeListener((compoundButton, b) -> mList.get(position).setC(b));

        holder.itemView.setOnClickListener(v -> {
            if (holder.checkBox.isChecked()){
                holder.checkBox.setChecked(false);
                mList.get(position).setC(false);
            }else {
                holder.checkBox.setChecked(true);
                mList.get(position).setC(true);
            }
        });

    }

    //Gets the total number of data source bars
    @Override
    public int getItemCount() {
        return mList.size();
    }

    public List<DeviceBean> getDeviceBeanList(){
        List<DeviceBean> deviceBeans = new ArrayList<>();

        for (DeviceBean deviceBean : mList){
            if (deviceBean.isC()){
                deviceBeans.add(deviceBean);
            }
        }
        return deviceBeans;
    }

    /**
     * A custom ViewHolder
     */
    class MyHolder extends RecyclerView.ViewHolder {


        TextView textSort;
        TextView textName;
        TextView textUser;
        TextView textMessenger;
        TextView textTime;
        CheckBox checkBox;

        public MyHolder(View itemView) {
            super(itemView);
            textSort = itemView.findViewById(R.id.tv_sort);
            textName = itemView.findViewById(R.id.tv_name);

            textUser = itemView.findViewById(R.id.tv_user);
            textMessenger = itemView.findViewById(R.id.tv_messenger);
            textTime = itemView.findViewById(R.id.tv_time);

            checkBox = itemView.findViewById(R.id.cb);


        }
    }
}

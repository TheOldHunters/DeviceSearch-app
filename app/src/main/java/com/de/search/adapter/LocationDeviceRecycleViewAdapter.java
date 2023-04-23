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
import com.de.search.bean.DeviceLocationBean;

import java.util.ArrayList;
import java.util.List;

public class LocationDeviceRecycleViewAdapter extends RecyclerView.Adapter<LocationDeviceRecycleViewAdapter.MyHolder> {

    private final List<DeviceLocationBean> mList;//Data source

    private LocationDeviceRecycleViewAdapterInterface locationDeviceRecycleViewAdapterInterface;

    public LocationDeviceRecycleViewAdapter(List<DeviceLocationBean> list) {
        mList = list;
    }

    public void setLocationDeviceRecycleViewAdapterInterface(LocationDeviceRecycleViewAdapterInterface locationDeviceRecycleViewAdapterInterface) {
        this.locationDeviceRecycleViewAdapterInterface = locationDeviceRecycleViewAdapterInterface;
    }

    //Creates a ViewHolder and returns it, and all subsequent items in the layout are retrieved from the ViewHolder
    @NonNull
    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Convert our custom item layout R.layout.item_one to View
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_device, parent, false);
        //Pass the view to our custom ViewHolder
        MyHolder holder = new MyHolder(view);
        //Returns the MyHolder entity
        return holder;
    }

    //Bind data to the ViewHolder through the ViewHolder provided by the method
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(MyHolder holder, @SuppressLint("RecyclerView") int position) {
        DeviceLocationBean deviceBean = mList.get(position);

        holder.textSort.setText(position + 1 + ".");
        holder.tvLatitude.setText(deviceBean.getLatitude());
        holder.tvLongitude.setText(deviceBean.getLongitude());
        holder.textSort.setText(position + 1 + ".");
        holder.textName.setText("name：" + deviceBean.getName() + "");
        holder.textRssi.setText("rssi：" + deviceBean.getRssi() + "");
        holder.textTime.setText("save time：" + deviceBean.getFindTime() + "");



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

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (locationDeviceRecycleViewAdapterInterface != null){
                    locationDeviceRecycleViewAdapterInterface.onLongClick(position);
                }
                return true;
            }
        });

    }

    //Gets the total number of data source bars
    @Override
    public int getItemCount() {
        return mList.size();
    }

    public List<DeviceLocationBean> getDeviceBeanList(){
        List<DeviceLocationBean> deviceBeans = new ArrayList<>();

        for (DeviceLocationBean deviceBean : mList){
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
        TextView tvLatitude;
        TextView tvLongitude;
        TextView textName;
        TextView textRssi;
        TextView textTime;
        CheckBox checkBox;

        public MyHolder(View itemView) {
            super(itemView);
            textSort = itemView.findViewById(R.id.tv_sort);
            tvLatitude = itemView.findViewById(R.id.tv_latitude);
            tvLongitude = itemView.findViewById(R.id.tv_longitude);
            textName = itemView.findViewById(R.id.tv_name);

            textRssi = itemView.findViewById(R.id.tv_rssi);
            textTime = itemView.findViewById(R.id.tv_time);

            checkBox = itemView.findViewById(R.id.cb);


        }
    }

    public interface LocationDeviceRecycleViewAdapterInterface{
        void onLongClick(int position);
    }



}

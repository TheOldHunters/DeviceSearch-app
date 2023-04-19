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

public class DeviceRecycleViewAdapter extends RecyclerView.Adapter<DeviceRecycleViewAdapter.MyHolder> {

    private final List<DeviceBean> mList;//Data source
    private final DeviceRecycleViewAdapterInterface myRecycleViewAdapterInterface;

    public DeviceRecycleViewAdapter(List<DeviceBean> list, DeviceRecycleViewAdapterInterface myRecycleViewAdapterInterface) {
        mList = list;
        this.myRecycleViewAdapterInterface = myRecycleViewAdapterInterface;
    }

    //Creates a ViewHolder and returns it, and all subsequent items in the layout are retrieved from the ViewHolder
    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Convert our custom item layout R.layout.item_one to View
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
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
        holder.textMac.setText("mac：" + deviceBean.getMac() + "");
        holder.textTime.setText("find time：" + deviceBean.getFindTime() + "");
        holder.textPlace.setText("find place：" + deviceBean.getFindPlace() + "");

        if (deviceBean.getMe() == 0){
            holder.textUser.setVisibility(View.VISIBLE);
            holder.textMessenger.setVisibility(View.VISIBLE);

            holder.textUser.setText("user：" + deviceBean.getUserName() + "");
            holder.textMessenger.setText("messenger：" + deviceBean.getMessengerName() + "");

        }else {
            holder.textUser.setVisibility(View.GONE);
            holder.textMessenger.setVisibility(View.GONE);
        }

        if (deviceBean.getFind() == 1){
//            float d = (float) Math.pow(10, ((Math.abs(deviceBean.getRssi()) - 60) / (10 * 2.0f)));
//            int i = (int) (d * 100);
//            d = (float) i / 100;
//            holder.textDistance.setText("distance(m)：" + d);
            holder.textDistance.setText("distance(m)：" + deviceBean.getDistance());
        }



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
        TextView textUser;
        TextView textMessenger;
        TextView textTime;
        TextView textPlace;
        TextView textDistance;


        public MyHolder(View itemView) {
            super(itemView);
            textSort = itemView.findViewById(R.id.tv_sort);
            textName = itemView.findViewById(R.id.tv_name);
            textMac = itemView.findViewById(R.id.tv_mac);

            textUser = itemView.findViewById(R.id.tv_user);
            textMessenger = itemView.findViewById(R.id.tv_messenger);
            textTime = itemView.findViewById(R.id.tv_time);
            textPlace = itemView.findViewById(R.id.tv_place);
            textDistance = itemView.findViewById(R.id.tv_distance);


        }
    }


    public interface DeviceRecycleViewAdapterInterface{
        void onLongClick(int position);
        void onClick(int position);
    }

}

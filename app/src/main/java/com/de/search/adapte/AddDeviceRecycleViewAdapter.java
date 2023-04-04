package com.de.search.adapte;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.de.search.R;
import com.de.search.bean.DeviceBean;

import java.util.List;

public class AddDeviceRecycleViewAdapter extends RecyclerView.Adapter<AddDeviceRecycleViewAdapter.MyHolder> {

    private List<DeviceBean> mList;//数据源
    private DeviceRecycleViewAdapterInterface myRecycleViewAdapterInterface;

    public AddDeviceRecycleViewAdapter(List<DeviceBean> list, DeviceRecycleViewAdapterInterface myRecycleViewAdapterInterface) {
        mList = list;
        this.myRecycleViewAdapterInterface = myRecycleViewAdapterInterface;
    }

    //创建ViewHolder并返回，后续item布局里控件都是从ViewHolder中取出
    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //将我们自定义的item布局R.layout.item_one转换为View
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_device, parent, false);
        //将view传递给我们自定义的ViewHolder
        MyHolder holder = new MyHolder(view);
        //返回这个MyHolder实体
        return holder;
    }

    //通过方法提供的ViewHolder，将数据绑定到ViewHolder中
    @Override
    public void onBindViewHolder(MyHolder holder, int position) {
        DeviceBean deviceBean = mList.get(position);



        holder.textSort.setText(position + 1 + ".");
        holder.textName.setText("name：" + deviceBean.getName() + "");
        holder.textMac.setText("mac：" + deviceBean.getMac() + "");
        holder.textRssi.setText("rssi：" + deviceBean.getRssi() + "");




        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                myRecycleViewAdapterInterface.onLongClick(position);
                return true;
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRecycleViewAdapterInterface.onClick(position);
            }
        });

    }

    //获取数据源总的条数
    @Override
    public int getItemCount() {
        return mList.size();
    }

    /**
     * 自定义的ViewHolder
     */
    class MyHolder extends RecyclerView.ViewHolder {


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

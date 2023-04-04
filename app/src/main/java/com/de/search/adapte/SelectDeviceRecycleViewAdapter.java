package com.de.search.adapte;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.de.search.R;
import com.de.search.bean.DeviceBean;

import java.util.ArrayList;
import java.util.List;

public class SelectDeviceRecycleViewAdapter extends RecyclerView.Adapter<SelectDeviceRecycleViewAdapter.MyHolder> {

    private List<DeviceBean> mList;//数据源


    public SelectDeviceRecycleViewAdapter(List<DeviceBean> list) {
        mList = list;
    }

    //创建ViewHolder并返回，后续item布局里控件都是从ViewHolder中取出
    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //将我们自定义的item布局R.layout.item_one转换为View
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_device, parent, false);
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

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mList.get(position).setC(b);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.checkBox.isChecked()){
                    holder.checkBox.setChecked(false);
                    mList.get(position).setC(false);
                }else {
                    holder.checkBox.setChecked(true);
                    mList.get(position).setC(true);
                }
            }
        });

    }

    //获取数据源总的条数
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
     * 自定义的ViewHolder
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

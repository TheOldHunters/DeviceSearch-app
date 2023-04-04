package com.de.search.adapte;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;


import com.de.search.R;
import com.de.search.bean.DeviceBean;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

public class DeviceRecycleViewAdapter extends RecyclerView.Adapter<DeviceRecycleViewAdapter.MyHolder> {

    private List<DeviceBean> mList;//数据源
    private DeviceRecycleViewAdapterInterface myRecycleViewAdapterInterface;

    public DeviceRecycleViewAdapter(List<DeviceBean> list, DeviceRecycleViewAdapterInterface myRecycleViewAdapterInterface) {
        mList = list;
        this.myRecycleViewAdapterInterface = myRecycleViewAdapterInterface;
    }

    //创建ViewHolder并返回，后续item布局里控件都是从ViewHolder中取出
    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //将我们自定义的item布局R.layout.item_one转换为View
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
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
            float d = (float) Math.pow(10, ((Math.abs(deviceBean.getRssi()) - 60) / (10 * 2.0f)));
            int i = (int) (d * 100);
            d = (float) i / 100;
            holder.textDistance.setText("distance(m)：" + d);
        }





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

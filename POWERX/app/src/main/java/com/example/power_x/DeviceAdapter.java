package com.example.power_x;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class DeviceAdapter extends ArrayAdapter<Device> {

    public DeviceAdapter(Context context, List<Device> devices) {
        super(context, 0, devices);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_item, parent, false);
        }

        Device device = getItem(position);

        TextView nameTextView = convertView.findViewById(R.id.device_name);
        ImageView iconImageView = convertView.findViewById(R.id.device_icon);

        if (device != null) {
            nameTextView.setText(device.getName());
            iconImageView.setImageResource(device.getIcon());
        }

        return convertView;
    }
}

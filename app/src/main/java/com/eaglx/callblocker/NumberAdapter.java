package com.eaglx.callblocker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eaglx.callblocker.global.AppConstants;
import com.eaglx.callblocker.model.Number;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NumberAdapter extends ArrayAdapter<Number> {

    NumberAdapter(Context context) {
        super(context, R.layout.blacklist_item);
    }

    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        if (view == null)
            view = View.inflate(getContext(), R.layout.blacklist_item, null);

        Number number = getItem(position);

        TextView tv = (TextView)view.findViewById(R.id.number);
        tv.setText(Number.wildcardsDbToView(number.number));

        tv = (TextView)view.findViewById(R.id.name);
        tv.setText(number.name);

        tv = (TextView)view.findViewById(R.id.rule);
        if(number.allow == 1) {
            tv.setText(R.string.allow);
        }
        else {
            tv.setText(R.string.block);
        }

        tv = (TextView)view.findViewById(R.id.stats);
        if (number.lastCall != null) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(getContext().getResources().getQuantityString(R.plurals.blacklist_call_details, number.timesCalled,
                    number.timesCalled, SimpleDateFormat.getDateTimeInstance().format(new Date(number.lastCall))));
        } else
            tv.setVisibility(View.GONE);

        ImageButton upButton = (ImageButton) view.findViewById(R.id.upEditButton);
        ImageButton downButton = (ImageButton) view.findViewById(R.id.downEditButton);

        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            }
        });

        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            }
        });

        if(AppConstants.getEdit_mode()) {
            upButton.setVisibility(View.VISIBLE);
            downButton.setVisibility(View.VISIBLE);
        }
        else {
            upButton.setVisibility(View.INVISIBLE);
            downButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }

}
package com.github.mikephil.charting.listener;

import com.github.mikephil.charting.components.LimitLine;

public interface OnChartLimitLineSelectedListener{

    void onValueSelected(LimitLine limitLine);

    void onNothingSelected();
}

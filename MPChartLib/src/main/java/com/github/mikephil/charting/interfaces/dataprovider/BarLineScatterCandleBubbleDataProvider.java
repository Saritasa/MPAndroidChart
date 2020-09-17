package com.github.mikephil.charting.interfaces.dataprovider;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.BarLineScatterCandleBubbleData;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.List;

public interface BarLineScatterCandleBubbleDataProvider extends ChartInterface{

    Transformer getTransformer(AxisDependency axis);

    boolean isInverted(AxisDependency axis);

    float getLowestVisibleX();

    float getHighestVisibleX();

    BarLineScatterCandleBubbleData getData();

    List<LimitLine> getLimitLines();

    ViewPortHandler getViewPortHanl();
}